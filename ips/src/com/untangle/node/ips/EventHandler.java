/**
 * $Id$
 */
package com.untangle.node.ips;

import com.untangle.uvm.vnet.AbstractEventHandler;
import com.untangle.uvm.vnet.IPNewSessionRequest;
import com.untangle.uvm.vnet.NodeSession;
import com.untangle.uvm.vnet.NodeTCPSession;
import com.untangle.uvm.vnet.NodeUDPSession;
import com.untangle.uvm.vnet.Protocol;
import com.untangle.uvm.vnet.event.TCPChunkEvent;
import com.untangle.uvm.vnet.event.TCPNewSessionRequestEvent;
import com.untangle.uvm.vnet.event.UDPNewSessionRequestEvent;
import com.untangle.uvm.vnet.event.UDPPacketEvent;

public class EventHandler extends AbstractEventHandler
{
    private IpsDetectionEngine ipsEngine;

    public EventHandler( IpsNodeImpl node )
    {
        super(node);
        ipsEngine = node.getEngine();
    }

    public void handleTCPNewSessionRequest( TCPNewSessionRequestEvent event )
    {
        handleNewSessionRequest(event.sessionRequest(), Protocol.TCP);
    }

    public void handleUDPNewSessionRequest( UDPNewSessionRequestEvent event )
    {
        handleNewSessionRequest(event.sessionRequest(), Protocol.UDP);
    }

    private void handleNewSessionRequest( IPNewSessionRequest request, Protocol protocol )
    {
        ipsEngine.processNewSessionRequest(request, protocol);
    }

    public void handleTCPNewSession( NodeTCPSession session )
    {
        handleNewSession( session, Protocol.TCP );
    }

    public void handleUDPNewSession( NodeUDPSession session )
    {
        handleNewSession( session, Protocol.UDP);
    }

    private void handleNewSession(NodeSession session, Protocol protocol)
    {
        ipsEngine.processNewSession(session, protocol);
    }

    public void handleTCPFinalized( NodeTCPSession session )
    {
        handleFinalized( session, Protocol.TCP );
    }

    public void handleUDPFinalized( NodeUDPSession session )
    {
        handleFinalized( session, Protocol.UDP );
    }

    private void handleFinalized( NodeSession session, Protocol protocol )
    {
        ipsEngine.processFinalized( session, protocol );
    }

    public void handleTCPClientChunk( TCPChunkEvent event )
    {
        ipsEngine.handleChunk( event.data(), event.session(), false );
        super.handleTCPClientChunk( event );
    }

    public void handleTCPServerChunk( TCPChunkEvent event )
    {
        ipsEngine.handleChunk( event.data(), event.session(), true );
        super.handleTCPServerChunk( event );
    }

    public void handleUDPClientPacket( UDPPacketEvent event )
    {
        ipsEngine.handleChunk( event.data(), event.session(), false );
        super.handleUDPClientPacket(event);
    }

    public void handleUDPServerPacket( UDPPacketEvent event )
    {
        ipsEngine.handleChunk( event.data(), event.session(), true );
        super.handleUDPServerPacket(event);
    }

}
