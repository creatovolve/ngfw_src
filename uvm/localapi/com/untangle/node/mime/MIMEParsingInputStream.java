/*
 * $HeadURL:$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent modules,
 * and to copy and distribute the resulting executable under terms of your
 * choice, provided that you also meet, for each linked independent module,
 * the terms and conditions of the license of that module.  An independent
 * module is a module which is not derived from or based on this library.
 * If you modify this library, you may extend this exception to your version
 * of the library, but you are not obligated to do so.  If you do not wish
 * to do so, delete this exception statement from your version.
 */

package com.untangle.node.mime;

import static com.untangle.node.util.Ascii.*;
import static com.untangle.node.util.ASCIIUtil.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.nio.*;
import java.util.*;

import com.untangle.node.util.*;
import org.apache.log4j.Logger;

/**
 * Specialized Stream with methods useful for
 * parsing MIME messages.  It defines a "line" as
 * being terminated by CRLF, CR, or LF.  The
 * sequences "CRCR" and "LFLF" would be considered
 * two lines.
 * <p>
 * For the MIME classes, this class supports querying
 * for the relative position within the larger stream via
 * the {@link #position position() method}.  Note that
 * calling the {@link #skip skip method} affects the count.
 * <p>
 * This stream has support for <i>unreading</i>.  This is implemented
 * by pushing bytes back, such that subsequent reads will see
 * the pushed-back bytes (like the JavaSoft "PushbackInputStream").
 */
public class MIMEParsingInputStream extends InputStream {

    private final Logger m_logger = Logger.getLogger(MIMEParsingInputStream.class);

    private static final int LINE_SZ = 1024;

    //Constants used when scanning for an EOL
    private static final int EOF_EOL = -1;
    private static final int NOT_EOL = 0;
    private static final int CRLF_EOL = 1;
    private static final int CR_EOL = 2;
    private static final int LF_EOL = 3;


    /**
     * Class used as the return of the
     * {@link #skipToBoundary skipToBoundary method}.
     */
    public static class BoundaryResult {

        /**
         * Was the boundary found.
         */
        public final boolean boundaryFound;
        /**
         * If {@link #boundaryFound the boundary was found}, was it
         * a final boundary (ending in "--").
         */
        public final boolean boundaryWasLast;
        /**
         * Includes leading CRLF (i.e. is before them).  If they
         * were not found (i.e. the method assumes it is started
         * at a new line and may not have scanned them) then
         * this is the position at-which the scanning
         * method was first invoked.
         */
        public final long boundaryStartPos;
        /**
         * Number of bytes from {@link #boundaryStartPos start}
         * which made-up the boundary.  Includes leading "--" or
         * "EOL--" and trailing "EOL", "--", or "--EOL"
         */
        public final long boundaryLen;

        private BoundaryResult() {
            this.boundaryFound = false;
            this.boundaryWasLast = false;
            this.boundaryStartPos = -1;
            this.boundaryLen = -1;
        }
        private BoundaryResult(long start,
                               long end,
                               boolean wasLast) {
            this.boundaryFound = true;
            this.boundaryWasLast = wasLast;
            this.boundaryStartPos = start;
            this.boundaryLen = end-start;
        }
    }

    /**
     * Member indicating that the final boundary was not found.  Used when
     * parsing pre-MIME or non-conformant MIME messages.
     */
    public static final BoundaryResult BOUNDARY_NOT_FOUND = new BoundaryResult();


    private final DynPushbackInputStream m_wrapped;

    //A bit optimistic on the system to read more than
    //2 gigs, but since the Java APIs let you skip
    //with a long we should count that as well
    private long m_count = 0;


    /**
     * Construct a new MIMEParsingInputStream, wrapping the
     * given stream.  Note that this class does not do any
     * buffering, so if the underlying stream is to a file
     * it should be buffered.
     */
    public MIMEParsingInputStream(InputStream wrap) {
        m_wrapped = new DynPushbackInputStream(wrap, LINE_SZ, LINE_SZ);
    }

    /**
     * The current position (or "count").  This is the number
     * of bytes consumed from the underlying
     * wrapped stream.  Note that any bytes pushed-back
     * are not considered (i.e. unreading moves the
     * pointer "back").
     *
     * @return the position (num bytes read)
     */
    public long position() {
        return m_count;
    }

    /**
     * Unread the byte by placing it back into the
     * stream for the next call to {@link #read read}.
     *
     * @param b the byte to be placed back-into the stream
     *
     * @exception IOException from the backing stream
     */
    public void unread(int b)
        throws IOException {
        m_wrapped.unread(b);
        m_count--;
    }


    /**
     * Unread the byte sequence by placing it back into the
     * stream for the next call to {@link #read read}.
     *
     * @exception IOException from the backing stream
     */
    public void unread(byte[] b)
        throws IOException {
        m_wrapped.unread(b);
        m_count-=b.length;
    }


    /**
     * Unread the byte sequence by placing it back into the
     * stream for the next call to {@link #read read}.
     *
     * @exception IOException from the backing stream
     */
    public void unread(byte[] b, int off, int len)
        throws IOException {
        m_wrapped.unread(b, off, len);
        m_count-=len;
    }


    @Override
    public int read()
        throws IOException {
        int ret = m_wrapped.read();
        if(ret >= 0) {
            m_count++;
        }
        return ret;
    }


    @Override
    public int read(byte[] b)
        throws IOException {
        int ret = m_wrapped.read(b);
        if(ret > 0) {
            m_count+=ret;
        }
        return ret;
    }


    @Override
    public int read(byte[] b, int off, int len)
        throws IOException {
        int ret = m_wrapped.read(b, off, len);
        if(ret > 0) {
            m_count+=ret;
        }
        return ret;
    }


    /**
     * Reads a line from the underlying data source.
     * <p>
     * If an EOF is encountered before a line terminator,
     * the remaining bytes are considered a line and a Line is returned
     * with a zero-length {@link Line#getTermLen terminator}.  If an EOF
     * is encountered as the first byte, null is returned.  If
     * only a terminator is encountered, a Line with a zero-length
     * {@link Line#getBuffer ByteBuffer} is returned.
     *
     * @param maxLen the maximum length to read before throwing
     *        LineTooLongException
     *
     * @return a Line or null (see desc above).
     *
     * @exception LineTooLongException if the line exceeds
     *            <code>maxLen</code>
     * @exception IOException from the backing stream
     */
    public Line readLine(int maxLen)
        throws IOException, LineTooLongException {

        byte b;
        int count = 0;
        ByteBufferBuilder bb = new ByteBufferBuilder(
                                                     LINE_SZ,
                                                     ByteBufferBuilder.GrowthStrategy.INCREMENTAL);


        int read = read();
        count++;

        while(read >= 0 /*0 isn't legal, but...*/ && count < maxLen) {
            b = (byte) read;
            bb.add(b);
            count++;
            if(b == CR) {
                //We read a CR.  Check if the next is an LF
                read = read();
                if(read >= 0) {
                    //Not EOF.  Check if the next byte was an LF
                    if((byte) read == LF) {
                        //Add the LF, return with a two-byte EOL
                        bb.add((byte) read);
                        return new Line(bb.toByteBuffer(), 2);
                    }
                    //Not a LF.  Put it back
                    unread(read);
                    return new Line(bb.toByteBuffer(), 1);
                }
                //Return from EOF
                return new Line(bb.toByteBuffer(), 1);
            }
            //We permit bare LFs
            if(b == LF) {
                return new Line(bb.toByteBuffer(), 1);
            }
            read = read();
        }
        if(count >= maxLen) {
            throw new LineTooLongException(maxLen);
        }
        return bb.size() == 0 ? null : new Line(bb.toByteBuffer(), 0);
    }

    /**
     * Read a Line of unlimited length
     *
     * @return a Line
     *
     * @exception LineTooLongException if the line exceeds the
     *            ability to buffer (~2 Gigs).
     * @exception IOException from the backing stream
     */
    public Line readLine()
        throws IOException, LineTooLongException {

        return readLine(Integer.MAX_VALUE);
    }

    /**
     * Unreads the line w/ terminator.
     *
     * @param line the line to unread
     */
    public void unreadLine(Line line)
        throws IOException {
        ByteBuffer buf = line.getBuffer(true);
        unread(buf.array(), buf.arrayOffset() + buf.position(), buf.remaining());
    }


    /**
     * Advance the Stream to a boundary.  The flag <code>leaveBoundary</code>
     * controls if the boundary will be left in the stream.
     * <br>
     * Normally, a boundary is in the following format:
     * <br>
     * EOL--<i>boundaryValue</i>[--]EOL
     * <br>
     * Where the leading "--" is required, and the trailing "--" indicates
     * that this was the last boundary in a multipart-set.  The leading
     * EOL is a subtle point.  To allow for parts which do <b>not</b>
     * end in an EOL, the EOL preceeding the boundary is treated
     * as part of the boundary.  So, in normal use this method
     * will consume "EOL--<i>boundaryValue</i>[--]EOL".  There
     * is one exception.  This method does not know if the stream
     * is already positioned past an EOL.  As such, scanning starts
     * assuming the preceeding sequence was an EOL.  This can effect
     * the {@link com.untangle.node.mime.MIMEParsingInputStream.BoundaryResult#boundaryStartPos start position}
     * of the boundary.
     * <br>
     * EOF is considered a valid terminator for a boundary.
     *
     * @param boundaryStr the boundary string <b>as it was defined in the
     *        headers (i.e. don't get smart and prepend "--")</b>
     * @param leaveBoundary if true, the boundary (including any leading/trailing
     *        EOL) will be left in the stream (unread).
     *
     * @return the BoundaryResult.
     */
    public BoundaryResult skipToBoundary(String boundaryStr,
                                         final boolean leaveBoundary)
        throws IOException {

        final byte[] matchPattern = new StringBuilder().
            append('-').
            append('-').
            append(boundaryStr).
            toString().getBytes();

        final int matchPatternLen = matchPattern.length;
        long boundaryStart = position();
        int read = read();

        //Members which are used while scanning for a
        //candidate boundary
        int candidatePos = 0;//In case someone positioned us after a EOL,
        //start at "0" instead of "-1"

        long boundaryEnd = 0;
        int boundaryStartEOL = 0;//TODO Make this a symbol
        int isEOL;

        while(read >= 0) {
            if(candidatePos == -1) {
                //candidatePos == -1 means "not starting search yet"

                //The "eatEOL" method is defined below
                isEOL = eatEOL(read);
                switch(isEOL) {
                case EOF_EOL:
                    return new BoundaryResult();
                case NOT_EOL:
                    break;
                case CRLF_EOL:
                    candidatePos = 0;
                    boundaryStartEOL = isEOL;
                    boundaryStart = position();
                    boundaryStart-=2;
                    break;
                case CR_EOL:
                case LF_EOL:
                    candidatePos = 0;
                    boundaryStartEOL = isEOL;
                    boundaryStart = position();
                    boundaryStart-=1;
                }

                read = read();
                continue;
            }
            //If we're here, then we're scanning a candidate
            if(matchPattern[candidatePos++] == (byte) read) {
                if(candidatePos == matchPatternLen) {
                    //We've found the whole boundary.  Figure out
                    //if it is the last
                    read = read();
                    if(read == -1) {
                        //Boundary ended the stream
                        boundaryEnd = position();
                        if(leaveBoundary) {
                            uneatEOL(boundaryStartEOL);
                            unread(matchPattern);
                        }
                        m_logger.debug("-1 ended (implicitly last) boundary");
                        return new BoundaryResult(boundaryStart,
                                                  boundaryEnd,
                                                  false);
                    }
                    else if((char) read == DASH) {
                        //So far, have read "boundary-".  Check for "boundary--".
                        read = read();
                        if(read == -1) {
                            //Boundary"-" ended the stream.  Do not count
                            //the trailing dash as part of a terminating boundary
                            unread(DASH);
                            boundaryEnd = position();
                            if(leaveBoundary) {
                                uneatEOL(boundaryStartEOL);
                                unread(matchPattern);
                            }
                            m_logger.debug("\"-\"(-1) ended (implicitly last) boundary");
                            return new BoundaryResult(boundaryStart,
                                                      boundaryEnd,
                                                      false);
                        }
                        else if(read == DASH) {
                            //Boundary ended in "--".  Check for the terminator
                            //as well
                            m_logger.debug("\"--\" ended (last) boundary");
                            int boundaryEndEOL = eatEOL();
                            boundaryEnd = position();
                            if(leaveBoundary) {
                                uneatEOL(boundaryStartEOL);
                                unread(matchPattern);
                                uneatEOL(boundaryEndEOL);
                            }
                            return new BoundaryResult(boundaryStart,
                                                      boundaryEnd,
                                                      true);
                        }
                        else {
                            //Boundary ended in "boundary-X" where "X" was not
                            //a dash.
                            m_logger.debug("\"-\"" + read + " ended non-last boundary");
                            unread(DASH);
                            unread(read);
                            boundaryEnd = position();
                            if(leaveBoundary) {
                                uneatEOL(boundaryStartEOL);
                                unread(matchPattern);
                            }
                            return new BoundaryResult(boundaryStart,
                                                      position(),
                                                      false);
                        }
                    }
                    else {
                        //Character after boundary was not "-".  It may have been
                        //an EOL.  If so, eat it.
                        int boundaryEndEOL = eatEOL(read);
                        if(boundaryEndEOL == EOF_EOL || boundaryEndEOL == NOT_EOL) {
                            unread(read);
                            m_logger.debug("\"" + read + "\" ended non-last boundary");
                        }
                        else {
                            m_logger.debug("New line ended non-last boundary");
                        }

                        boundaryEnd = position();
                        if(leaveBoundary) {
                            uneatEOL(boundaryStartEOL);
                            unread(matchPattern);
                            uneatEOL(boundaryEndEOL);
                        }
                        return new BoundaryResult(boundaryStart,
                                                  boundaryEnd,
                                                  false);
                    }
                }
                read = read();
            }
            else {
                //Fell out of the candidate.  Let the byte be re-evaluated (it may be a CR/LF)
                candidatePos = -1;
                boundaryStartEOL = 0;
                boundaryStart = -1;
            }
        }
        return new BoundaryResult();
    }

    /**
     * Advances past the next EOL sequence (CRLF
     * CR, or LF), or EOF
     *
     * @exception IOException from the backing stream
     */
    public void advanceToNextLine()
        throws IOException {
        int b = read();
        while(b >= 0) {
            if(isEOL((byte) b)) {
                eatEOL(b);
                return;
            }
            b = read();
        }
    }

    /**
     * Skips to the end of the file.
     *
     * @exception IOException from the backing stream
     */
    public void advanceToEOF()
        throws IOException {
        while(read() >= 0);
    }


    @Override
    public long skip(long n)
        throws IOException {
        long ret = m_wrapped.skip(n);
        m_count+=ret;
        return ret;
    }


    @Override
    public int available() throws IOException {
        return m_wrapped.available();
    }


    @Override
    public void close()
        throws IOException {
        m_wrapped.close();
    }

    /**
     * Mark is not supported, so this method does nothing
     *
     * @exception IOException from the backing stream
     */
    @Override
    public void mark(int readlimit) {
        //Do nothing
    }


    /**
     * Since marks are not supported, this always throws
     * an exception
     *
     * @exception IOException (always)
     */
    @Override
    public void reset()
        throws IOException {
        throw new IOException("mark not supported");
    }


    /**
     * Always returns false
     *
     * @exception IOException from the backing stream
     */
    @Override
    public boolean markSupported() {
        return false;
    }


    private void uneatEOL(int val)
        throws IOException {
        switch(val) {
        case CRLF_EOL:
            unread(CRLF_BA);
            break;
        case CR_EOL:
            unread(CR);
            break;
        case LF_EOL:
            unread(LF);
            break;
        }
    }

    /**
     * Eat the next EOL, if one is found
     *
     * @return constants defined as "XXX_EOL"
     *         on this class.
     */
    private int eatEOL()
        throws IOException {
        int read = read();
        int ret = eatEOL(read);
        if(ret == 0) {
            unread(read);
        }
        return ret;
    }
    /**
     * Eat the EOL, if the character starts
     * an EOL sequence.  If it does not,
     * it is <b>not</b> unread implicitly.
     *
     * @return constants defined as "XXX_EOL"
     *         on this class.
     */
    private int eatEOL(int read)
        throws IOException {
        if(read == -1) {
            return EOF_EOL;
        }
        if((char) read == CR) {
            int read2 = read();
            if(read2 == -1) {
                return CR_EOL;
            }
            if((char) read2 == LF) {
                return CRLF_EOL;
            }
            else {
                unread(read2);
                return CR_EOL;
            }
        }
        if((char) read == LF) {
            return LF_EOL;
        }
        return NOT_EOL;
    }


    public static void main(String[] args) throws Exception {
        String crlf = new String(CRLF_BA);

        doTest(crlf + "--foo" + crlf + "next line");
        doTest("ABC" + crlf + "--foo" + crlf + "next line");
        doTest("--foo" + crlf + "next line");
    }

    private static void doTest(String str)
        throws Exception {
        System.out.println("\n\n----------------\n");
        byte[] bytes = str.getBytes();
        for(int i = 0; i<bytes.length; i++) {
            String toPrint =
                bytes[i] == CR ? "<CR>" :
                bytes[i] == LF ? "<LF>" : new StringBuilder().append((char) bytes[i]).toString();
            System.out.println(i + " " + toPrint);
        }
        java.io.ByteArrayInputStream bais =
            new java.io.ByteArrayInputStream(bytes);
        MIMEParsingInputStream stream = new MIMEParsingInputStream(bais);
        System.out.println("Position: " + stream.position());
        BoundaryResult br = stream.skipToBoundary("foo", false);
        System.out.println("boundaryStartPos: " + br.boundaryStartPos);
        System.out.println("boundaryLen: " + br.boundaryLen);
        System.out.println("Position: " + stream.position());
    }
}
