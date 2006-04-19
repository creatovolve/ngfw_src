/*
 * Copyright (c) 2006 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.portal.proxy;

import java.io.PrintWriter;

import org.apache.log4j.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.DTDHandler;
import org.xml.sax.ErrorHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public class HtmlRewriter implements DTDHandler, ContentHandler, ErrorHandler
{
    private static final String DOCTYPE = "!DOCTYPE";
    private static final String TEXT = "#text";
    private static final String HREF = "href";
    private static final String SRC = "src";

    private final PrintWriter writer;
    private final String contextRoot;
    private final String requestHost;
    private final Logger logger = Logger.getLogger(getClass());

    HtmlRewriter(PrintWriter writer, String contextRoot, String requestHost)
    {
        this.writer = writer;
        this.contextRoot = contextRoot;
        this.requestHost = requestHost;
    }

    // DTDHandler methods -----------------------------------------------------

    public void notationDecl(String name,
                             String publicId,
                             String systemId)
        throws SAXException
    {
        System.out.println("notationDecl");
    }


    public void unparsedEntityDecl(String name,
                                   String publicId,
                                   String systemId,
                                   String notationName)
        throws SAXException
    {
        System.out.println("unparsedEntityDecl");
    }

    // ContentHandler methods -------------------------------------------------

    public void setDocumentLocator(Locator locator) { }

    public void startDocument() throws SAXException { }

    public void endDocument() throws SAXException { }

    public void startPrefixMapping(String prefix, String uri)
        throws SAXException
    {
        System.out.println("startPrefixMapping");
    }

    public void endPrefixMapping(String prefix) throws SAXException
    {
        System.out.println("endPrefixMapping");
    }

    public void startElement(String uri, String localName, String qName,
                             Attributes atts)
        throws SAXException
    {
        boolean useSpaces = localName.equals(DOCTYPE);

        writer.print("<");
        writer.print(localName);

        int l = atts.getLength();
        for (int i = 0; i < l; i++) {
            String k = atts.getQName(i);
            String v = atts.getValue(i);

            if (k.equals(TEXT)) {
                writer.print(v);
            } else if (HREF.equals(k)) {
                rewrite(k, v, useSpaces);
            } else if (SRC.equals(k)) {
                rewrite(k, v, useSpaces);
            } else {
                writer.print(k);
                if (!useSpaces) {
                    writer.print("=\"");
                }
                writer.print(v);
                if (!useSpaces) {
                    writer.print("\"");
                }

            }
        }

        writer.print(">");
    }

    public void endElement(String uri, String localName, String qName)
        throws SAXException
    {
        writer.print("</");
        writer.print(localName);
        writer.print(">");
    }

    public void characters(char ch[], int start, int length)
        throws SAXException
    {
        writer.print(new String(ch, start, length));
    }

    public void ignorableWhitespace(char ch[], int start, int length)
        throws SAXException
    {
        writer.print(new String(ch, start, length));
    }

    public void processingInstruction(String target, String data)
        throws SAXException
    {
        System.out.println("processingInstruction");
    }

    public void skippedEntity(String name)
        throws SAXException
    {
        System.out.println("skippedEntity");
    }

    // ErrorHandler methods ---------------------------------------------------

    public void warning(SAXParseException exn)
        throws SAXException
    {
        System.out.println("WARNING: " + exn);
    }


    public void error(SAXParseException exn)
        throws SAXException
    {
        System.out.println("ERROR: " + exn);
    }


    public void fatalError(SAXParseException exn)
        throws SAXException
    {
        System.out.println("FATAL: " + exn);
    }


    // private methods --------------------------------------------------------

    public void rewrite(String k, String v, boolean useSpaces)
    {
        String url;

        if (v.startsWith("http://")) {
            url = contextRoot + "/" + v.substring(7);
        } else if (v.startsWith("//")) {
            url = contextRoot + "/" + v.substring(2);
        } else if (v.startsWith("/")) {
            url = contextRoot + "/" + requestHost + v;
        } else {
            url = v;
        }

        writer.print(k);
        if (!useSpaces) {
            writer.print("=\"");
        }
        writer.print(url);
        if (!useSpaces) {
            writer.print("\"");
        }
    }
}
