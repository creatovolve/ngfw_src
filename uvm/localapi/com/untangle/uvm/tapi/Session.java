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

package com.untangle.uvm.tapi;

// import org.apache.commons.jxpath.JXPathContext;

/**
 * The interface <code>Session</code> here.
 *
 * @author <a href="mailto:jdi@untangle.com"></a>
 * @version 1.0
 */
public interface Session extends SessionDesc {

    /**
     * <code>mPipe</code> returns the Meta Pipe <code>MPipe</code> that this session lives on.
     *
     * @return the <code>MPipe</code> that this session is for
     */
    MPipe mPipe();

    /**
     * Attaches the given object to this session.
     *
     * <p> An attached object may later be retrieved via the {@link #attachment
     * attachment} method.  Only one object may be attached at a time; invoking
     * this method causes any previous attachment to be discarded.  The current
     * attachment may be discarded by attaching <tt>null</tt>.  </p>
     *
     * @param  ob
     *         The object to be attached; may be <tt>null</tt>
     *
     * @return  The previously-attached object, if any,
     *          otherwise <tt>null</tt>
     */
    Object attach(Object ob);

    /**
     * Retrieves the current attachment.  </p>
     *
     * @return  The object currently attached to this session,
     *          or <tt>null</tt> if there is no attachment
     */
    Object attachment();

    // JXPathContext sessionContext();

    // ExtendedPreferences sessionNode();

}
