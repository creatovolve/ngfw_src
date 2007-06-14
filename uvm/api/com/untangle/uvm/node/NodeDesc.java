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

package com.untangle.uvm.node;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import com.untangle.uvm.security.Tid;

/**
 * Node settings and properties.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
public class NodeDesc implements Serializable
{
    private static final long serialVersionUID = -578021414141899172L;

    private final Tid tid;

    private final String name;

    private final String className;
    private final String guiClassName;
    private final String nodeBase;

    private final List<String> exports;
    private final List<String> parents;
    private final boolean singleInstance;

    private final String displayName;
    private final String syslogName;

    private final int tcpClientReadBufferSize = 8192;
    private final int tcpServerReadBufferSize = 8192;
    private final int udpMaxPacketSize = 16384;

    public NodeDesc(Tid tid, String name, String className,
                         String guiClassName, String nodeBase,
                         List<String> exports, List<String> parents,
                         boolean singleInstance, String displayName)
    {
        this.tid = tid;
        this.name = name;
        this.className = className;
        this.guiClassName = guiClassName;
        this.nodeBase = nodeBase;
        List<String> l = null == exports ? new LinkedList<String>() : exports;
        this.exports = Collections.unmodifiableList(l);
        l = null == parents ? new LinkedList<String>() : parents;
        this.parents = Collections.unmodifiableList(l);
        this.singleInstance = singleInstance;
        this.displayName = displayName;
        syslogName = displayName.replaceAll("\\p{Space}", "_");
    }

    // accessors --------------------------------------------------------------

    /**
     * Node id.
     *
     * @return tid for this instance.
     */
    public Tid getTid()
    {
        return tid;
    }

    /**
     * Internal name of the node.
     *
     * @return the node's name.
     */
    public String getName()
    {
        return name;
    }

    /**
     * Name of the main node Class.
     *
     * @return node class name.
     */
    public String getClassName()
    {
        return className;
    }

    /**
     * Names of shared jars.
     *
     * @return names of shared jars.
     */
    public List<String> getExports()
    {
        return exports;
    }

    /**
     * The parent node, usually a casing.
     *
     * @return the parent node, null if node has no parent.
     */
    public List<String> getParents()
    {
        return parents;
    }

    /**
     * Only a single instance may be initialized in the system.
     *
     * @return true if only a single instance may be loaded.
     */
    public boolean isSingleInstance()
    {
        return singleInstance;
    }

    /**
     * TCP client read BufferSize is between 1 and 65536 bytes.
     *
     * @return the TCP client read bufferSize.
     */
    public int getTcpClientReadBufferSize()
    {
        return tcpClientReadBufferSize;
    }

    /**
     * TCP server read bufferSize is between 1 and 65536 bytes.
     *
     * @return the TCP server read bufferSize.
     */
    public int getTcpServerReadBufferSize()
    {
        return tcpServerReadBufferSize;
    }

    /**
     * UDP max packet size, between 1 and 65536 bytes, defaults to 16384.
     *
     * @return UDP max packet size.
     */
    public int getUdpMaxPacketSize()
    {
        return udpMaxPacketSize;
    }

    /**
     * The name of the node, for display purposes.
     *
     * @return display name.
     */
    public String getDisplayName()
    {
        return displayName;
    }

    /**
     * The name of the node, for syslog purposes.
     *
     * @return syslog name.
     */
    public String getSyslogName()
    {
        return syslogName;
    }

    /**
     * The class name of the GUI module.
     *
     * @return class name of GUI component.
     */
    public String getGuiClassName()
    {
        return guiClassName;
    }

    /**
     * The nodeBase is the name of the base node. For example
     * clam-node's nodeBase is virus-base.
     *
     * @return the nodeBase, null if node does not have a base.
     */
    public String getNodeBase()
    {
        return nodeBase;
    }

    // Object methods ---------------------------------------------------------

    /**
     * Equality based on the business key (tid).
     *
     * @param o the object to compare to.
     * @return true if equal.
     */
    public boolean equals(Object o)
    {
        if (!(o instanceof NodeDesc)) {
            return false;
        }

        NodeDesc td = (NodeDesc)o;

        return tid.equals(td.getTid());
    }
}
