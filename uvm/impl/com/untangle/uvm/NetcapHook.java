/**
 * $Id$
 */
package com.untangle.uvm;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.net.InetAddress;

import org.apache.log4j.Logger;

import com.untangle.jnetcap.NetcapSession;
import com.untangle.jvector.IncomingSocketQueue;
import com.untangle.jvector.OutgoingSocketQueue;
import com.untangle.jvector.Relay;
import com.untangle.jvector.ResetCrumb;
import com.untangle.jvector.Sink;
import com.untangle.jvector.Source;
import com.untangle.jvector.Vector;
import com.untangle.uvm.IntfConstants;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.NetworkManager;
import com.untangle.uvm.HostTable;
import com.untangle.uvm.HostTableEntry;
import com.untangle.uvm.node.SessionTuple;
import com.untangle.uvm.node.SessionTupleImpl;
import com.untangle.uvm.node.SessionEvent;
import com.untangle.uvm.node.SessionNatEvent;
import com.untangle.uvm.node.SessionStatsEvent;
import com.untangle.uvm.node.PolicyManager;
import com.untangle.uvm.vnet.NodeSession;
import com.untangle.uvm.node.HostnameLookup;
import com.untangle.uvm.network.InterfaceSettings;

/**
 * Helper class for the IP session hooks.
 */
public abstract class NetcapHook implements Runnable
{
    private static final Logger logger = Logger.getLogger( NetcapHook.class );

    /* Reject the client with whatever response the server returned */
    protected static final int REJECT_CODE_SRV = -1;

    private static final int TCP_HEADER_SIZE_ESTIMATE = 32;
    private static final int IP_HEADER_SIZE = 20;
    private static final int UDP_HEADER_SIZE = 8;

    private static final SessionTable activeSessions = SessionTable.getInstance();

    /**
     * List of all of the nodes( PipelineConnectorImpls )
     */
    protected List<PipelineConnectorImpl> pipelineConnectors;
    protected Long policyId = null;

    protected List<NodeSessionImpl> sessionList = new ArrayList<NodeSessionImpl>();
    protected List<NodeSessionImpl> releasedSessionList = new ArrayList<NodeSessionImpl>();

    protected Source clientSource;
    protected Sink   clientSink;
    protected Source serverSource;
    protected Sink   serverSink;

    protected Vector vector = null;

    protected SessionGlobalState sessionGlobalState;

    protected SessionTuple clientSide = null;
    protected SessionTuple serverSide = null;

    protected static final PipelineFoundryImpl pipelineFoundry = (PipelineFoundryImpl)UvmContextFactory.context().pipelineFoundry();
    
    /**
     * State of the session
     */
    protected int state      = IPNewSessionRequestImpl.REQUESTED;
    protected int rejectCode = REJECT_CODE_SRV;

    public Vector getVector()
    {
        return this.vector;
    }
    
    /**
     * Thread hook
     */
    public final void run()
    {
        long start = 0;
        SessionEvent sessionEvent = null;
        
        try {
            Thread.currentThread().setContextClassLoader(getClass().getClassLoader());

            sessionGlobalState = new SessionGlobalState( netcapSession(), clientSideListener(), serverSideListener(), this );
            boolean serverActionCompleted = false;
            boolean clientActionCompleted = false;
            NetcapSession netcapSession = sessionGlobalState.netcapSession();
            int clientIntf = netcapSession.clientSide().interfaceId();
            int serverIntf = netcapSession.serverSide().interfaceId();
            InetAddress clientAddr = netcapSession.clientSide().client().host();
            long sessionId = sessionGlobalState.id();
            boolean entitled = true;
            
            if ( logger.isDebugEnabled()) {
                logger.debug( "New thread for session id: " + sessionId + " " + sessionGlobalState );
            }

            if ( serverIntf == IntfConstants.UNKNOWN_INTF ) {
                logger.warn( "Unknown destination interface: " + netcapSession + ". Killing session." );
                raze();
                return;
            } else {
                netcapSession.setServerIntf(serverIntf);
            }

            /**
             * Create the initial tuples based on current information
             */
            clientSide = new SessionTupleImpl( sessionGlobalState.id(),
                                               sessionGlobalState.getProtocol(),
                                               netcapSession.clientSide().interfaceId(), /* always get clientIntf from client side */
                                               netcapSession.serverSide().interfaceId(), /* always get serverIntf from server side */
                                               netcapSession.clientSide().client().host(),
                                               netcapSession.clientSide().server().host(),
                                               netcapSession.clientSide().client().port(),
                                               netcapSession.clientSide().server().port());
            serverSide = new SessionTupleImpl( sessionGlobalState.id(),
                                               sessionGlobalState.getProtocol(),
                                               netcapSession.clientSide().interfaceId(), /* always get clientIntf from client side */
                                               netcapSession.serverSide().interfaceId(), /* always get serverIntf from server side */
                                               netcapSession.serverSide().client().host(),
                                               netcapSession.serverSide().server().host(),
                                               netcapSession.serverSide().client().port(),
                                               netcapSession.serverSide().server().port());

            /* lookup the host table information */
            HostTableEntry hostEntry = UvmContextFactory.context().hostTable().getHostTableEntry( clientAddr );
            DeviceTableEntry deviceEntry = null;
            String username = null;
            String hostname = null;

            if ( hostEntry == null ) {
                /* if its not in the host table and is a non-WAN client, create a host table entry */
                InterfaceSettings intfSettings = UvmContextFactory.context().networkManager().findInterfaceId( netcapSession.clientSide().interfaceId() );
                if ( intfSettings != null && ! intfSettings.getIsWan() ) {
                    hostEntry = UvmContextFactory.context().hostTable().getHostTableEntry( clientAddr, true ); /* create/get host */
                }
                /* include OpenVPN, L2TP, Xauth, and GRE clients in host table */
                if ( netcapSession.clientSide().interfaceId() == 0xfa ||
                     netcapSession.clientSide().interfaceId() == 0xfb ||
                     netcapSession.clientSide().interfaceId() == 0xfc ||
                     netcapSession.clientSide().interfaceId() == 0xfd ) {
                    hostEntry = UvmContextFactory.context().hostTable().getHostTableEntry( clientAddr, true ); /* create/get host */
                }
            } 
            
            /* if hostEntry is still not null */
            if ( hostEntry != null ) {
                String macAddress = hostEntry.getMacAddress();
                if ( macAddress != null )
                    deviceEntry = UvmContextFactory.context().deviceTable().getDevice( macAddress );
                
                /* update last session & last seen time */
                hostEntry.setLastSessionTime( System.currentTimeMillis() );
                if ( deviceEntry != null )
                    deviceEntry.updateLastSeenTime();

                /* update client interface */
                if ( clientIntf != hostEntry.getInterfaceId() )
                    hostEntry.setInterfaceId( clientIntf );
                if ( deviceEntry != null && clientIntf != deviceEntry.getLastSeenInterfaceId() )
                    deviceEntry.setLastSeenInterfaceId( clientIntf );
                
                /* if host is not entitled, session is not entitled */
                if ( ! hostEntry.getEntitled() )
                    entitled = false;
                
                username = hostEntry.getUsername();
                /* if we don't know the username from the host table, check the device table */
                if (username == null && deviceEntry != null) {
                    String deviceUsername = deviceEntry.getDeviceUsername();
                    if ( deviceUsername != null ) {
                        hostEntry.setUsernameDevice( deviceUsername );
                        username = deviceUsername;
                    }
                }
                /* if we know the username, set the username on the session */
                if (username != null && username.length() > 0 ) { 
                    logger.debug( "user information: " + username );
                    sessionGlobalState.setUser( username );
                    sessionGlobalState.attach( NodeSession.KEY_PLATFORM_USERNAME, username );
                }
                /* lookup the hostname information */
                HostnameLookup router = (HostnameLookup) UvmContextFactory.context().nodeManager().node("untangle-node-router");
                HostnameLookup reports = (HostnameLookup) UvmContextFactory.context().nodeManager().node("untangle-node-reports");
                hostname = hostEntry.getHostname();
                if ((hostname == null || hostname.length() == 0) && reports != null)
                    hostname = reports.lookupHostname( clientAddr );
                if ((hostname == null || hostname.length() == 0) && router != null)
                    hostname = router.lookupHostname( clientAddr );
                if ((hostname == null || hostname.length() == 0))
                    hostname = clientAddr.getHostAddress();
                if (hostname != null && hostname.length() > 0 ) {
                    sessionGlobalState.attach( NodeSession.KEY_PLATFORM_HOSTNAME, hostname );
                    /* If the hostname isn't known in the host table then set hostname */
                    if ( !hostEntry.isHostnameKnown()) {
                        hostEntry.setHostname( hostname );
                        if ( deviceEntry != null )
                            deviceEntry.setHostname( hostname );
                    }
                }
            } else {
                /**
                 * no host (probably an external host)
                 * still need to set hostname
                 */
                if ((hostname == null || hostname.length() == 0))
                    hostname = clientAddr.getHostAddress();
            }
            
            PolicyManager policyManager = (PolicyManager) UvmContextFactory.context().nodeManager().node("untangle-node-policy-manager");
            if ( policyManager != null && entitled ) {
                this.policyId  = policyManager.findPolicyId( sessionGlobalState.getProtocol(),
                                                             netcapSession.clientSide().interfaceId(), netcapSession.serverSide().interfaceId(),
                                                             netcapSession.clientSide().client().host(), netcapSession.serverSide().server().host(),
                                                             netcapSession.clientSide().client().port(), netcapSession.serverSide().server().port());
            } else {
                this.policyId = 1L; /* Default Policy */
            }

            pipelineConnectors = pipelineFoundry.weld( sessionGlobalState.id(), clientSide, policyId, entitled );
            sessionGlobalState.setPipelineConnectorImpls(pipelineConnectors);
            
            /* Create the sessionEvent early so they can be available at request time. */
            sessionEvent =  new SessionEvent( );
            sessionEvent.setSessionId( sessionGlobalState.id() );
            sessionEvent.setBypassed( false );
            sessionEvent.setEntitled( entitled );
            sessionEvent.setProtocol( sessionGlobalState.getProtocol() );
            sessionEvent.setClientIntf( clientSide.getClientIntf() );
            sessionEvent.setServerIntf( clientSide.getServerIntf() );
            sessionEvent.setUsername( username );
            sessionEvent.setHostname( hostname );
            sessionEvent.setPolicyId( policyId );
            sessionEvent.setCClientAddr( clientSide.getClientAddr() );
            sessionEvent.setCClientPort( clientSide.getClientPort() );
            sessionEvent.setCServerAddr( clientSide.getServerAddr() );
            sessionEvent.setCServerPort( clientSide.getServerPort() );
            sessionEvent.setSClientAddr( serverSide.getClientAddr() );
            sessionEvent.setSClientPort( serverSide.getClientPort() );
            sessionEvent.setSServerAddr( serverSide.getServerAddr() );
            sessionEvent.setSServerPort( serverSide.getServerPort() );

            sessionGlobalState.setSessionEvent( sessionEvent );

            int tupleHashCodeOriginal =
                sessionEvent.getSClientAddr().hashCode() + 
                sessionEvent.getSClientPort() + 
                sessionEvent.getSServerAddr().hashCode() + 
                sessionEvent.getSServerPort();

            /* log the session event */
            UvmContextFactory.context().logEvent( sessionEvent );

            /* Initialize all of the nodes, sending the request events to each in turn */
            initializeNodeSessions( sessionEvent );

            int tupleHashCodeNew =
                sessionEvent.getSClientAddr().hashCode() + 
                sessionEvent.getSClientPort() + 
                sessionEvent.getSServerAddr().hashCode() + 
                sessionEvent.getSServerPort();

            /* If any NAT/transformation of the session has taken place, log a NAT event to update the server side attributes */
            if (  tupleHashCodeOriginal != tupleHashCodeNew ) {
                SessionNatEvent natEvent = new SessionNatEvent( sessionEvent,
                                                                serverSide.getServerIntf(),
                                                                sessionEvent.getSClientAddr(),
                                                                sessionEvent.getSClientPort(),                                                               
                                                                sessionEvent.getSServerAddr(),
                                                                sessionEvent.getSServerPort());
                UvmContextFactory.context().logEvent(natEvent);
            }
            
            /* Connect to the server */
            serverActionCompleted = connectServerIfNecessary( sessionEvent );

            /* Now generate the server side since the nodes may have
             * modified the sessionEvent (we can't do it until we connect
             * to the server since that is what actually modifies the
             * session global state. */
            serverSide = new SessionTupleImpl( sessionGlobalState.id(),
                                               sessionGlobalState.getProtocol(),
                                               netcapSession.clientSide().interfaceId(), /* always get clientIntf from client side */
                                               netcapSession.serverSide().interfaceId(), /* always get serverIntf from server side */
                                               sessionEvent.getSClientAddr(),
                                               sessionEvent.getSServerAddr(),
                                               sessionEvent.getSClientPort(),
                                               sessionEvent.getSServerPort());

            /* Connect to the client */
            clientActionCompleted = connectClientIfNecessary();

            /* Remove all non-vectored sessions, it is non-efficient
             * to iterate the session list twice, but the list is
             * typically small and this logic may get very complex
             * otherwise */
            for ( Iterator<NodeSessionImpl> iter = sessionList.iterator(); iter.hasNext() ; ) {
                NodeSessionImpl nodeSession = iter.next();
                if ( !nodeSession.isVectored() ) {
                    logger.debug( "Removing non-vectored nodeSession from the nodeSession list" + nodeSession );
                    iter.remove();
                    /* Append to the released nodeSession list */
                    releasedSessionList.add( nodeSession );
                }

                // Complete (if we completed both server and client)
                if (serverActionCompleted && clientActionCompleted)
                    nodeSession.complete();
            }

            /* Only start vectoring if the session is alive */
            if ( alive() ) {

                /* if host is not null and this is a TCP session updated host host */
                if ( hostEntry != null && sessionGlobalState.getProtocol() == 6 ) {
                    hostEntry.setLastCompletedTcpSessionTime( System.currentTimeMillis() );
                }

                try {
                    /* Build the pipeline */
                    buildPipeline();

                    /* Insert the vector */
                    activeSessions.put( vector, sessionGlobalState );

                    /* Set the timeout for the vectoring machine */
                    vector.timeout( timeout() );

                    if ( logger.isDebugEnabled())
                        logger.debug( "Starting vectoring for session " + sessionGlobalState );

                    //vector.print();

                    /* Start vectoring */
                    vector.vector();

                    /* Call the raze method for each session */
                } catch ( Exception e ) {
                    logger.error( "Exception inside netcap hook: " + sessionGlobalState, e );
                }

                if ( logger.isDebugEnabled())
                    logger.debug( "Finished vectoring for session: " + sessionGlobalState );
            } else {
                logger.debug( "Session rejected, skipping vectoring: " + sessionGlobalState );
            }
        } catch ( Exception e ) {
            /* Some exceptions have null messages, who knew */
            String message = e.getMessage();
            if ( message == null ) message = "";

            if ( message.startsWith( "Invalid netcap interface" )) {
                try {
                    logger.warn( "invalid interface: " + sessionGlobalState.netcapSession());
                } catch( Exception exn ) {
                    /* Just in case */
                    logger.warn( "exception debugging invalid netcap interface: ", exn );
                }
            } else if ( message.startsWith( "netcap_interface_dst_intf" )) {
                logger.warn( "Unable to determine the outgoing interface: " + sessionGlobalState.netcapSession() );
            } else {
                logger.warn( "Exception executing netcap hook:", e );
            }
        }

        try {
            /* Must raze sessions all sessions in the session list */
            razeSessions();
        } catch ( Exception e ) {
            logger.error( "Exception razing sessions", e );
        }

        try {
            if (clientSide != null) {
                SessionStatsEvent statEvent = new SessionStatsEvent(sessionEvent);
                long c2pBytes = sessionGlobalState.clientSideListener().rxBytes;
                long p2cBytes = sessionGlobalState.clientSideListener().txBytes;
                long c2pChunks = sessionGlobalState.clientSideListener().rxChunks;
                long p2cChunks = sessionGlobalState.clientSideListener().txChunks;

                long s2pBytes = sessionGlobalState.serverSideListener().rxBytes;
                long p2sBytes = sessionGlobalState.serverSideListener().txBytes;
                long s2pChunks = sessionGlobalState.serverSideListener().rxChunks;
                long p2sChunks = sessionGlobalState.serverSideListener().txChunks;

                /**
                 * Adjust for packet headers
                 */
                if ( sessionGlobalState.getProtocol() == 6 ) {
                    c2pBytes = c2pBytes + (c2pChunks * IP_HEADER_SIZE) + (c2pChunks * TCP_HEADER_SIZE_ESTIMATE);
                    p2cBytes = p2cBytes + (p2cChunks * IP_HEADER_SIZE) + (p2cChunks * TCP_HEADER_SIZE_ESTIMATE);
                    s2pBytes = s2pBytes + (s2pChunks * IP_HEADER_SIZE) + (s2pChunks * TCP_HEADER_SIZE_ESTIMATE);
                    p2sBytes = p2sBytes + (p2sChunks * IP_HEADER_SIZE) + (p2sChunks * TCP_HEADER_SIZE_ESTIMATE);
                }
                if ( sessionGlobalState.getProtocol() == 17 ) {
                    c2pBytes = c2pBytes + (c2pChunks * IP_HEADER_SIZE) + (c2pChunks * UDP_HEADER_SIZE);
                    p2cBytes = p2cBytes + (p2cChunks * IP_HEADER_SIZE) + (p2cChunks * UDP_HEADER_SIZE);
                    s2pBytes = s2pBytes + (s2pChunks * IP_HEADER_SIZE) + (s2pChunks * UDP_HEADER_SIZE);
                    p2sBytes = p2sBytes + (p2sChunks * IP_HEADER_SIZE) + (p2sChunks * UDP_HEADER_SIZE);
                }
                    
                statEvent.setC2pBytes(sessionGlobalState.clientSideListener().rxBytes);
                statEvent.setP2cBytes(sessionGlobalState.clientSideListener().txBytes);
                statEvent.setC2pChunks(sessionGlobalState.clientSideListener().rxChunks);
                statEvent.setP2cChunks(sessionGlobalState.clientSideListener().txChunks);

                statEvent.setS2pBytes(sessionGlobalState.serverSideListener().rxBytes);
                statEvent.setP2sBytes(sessionGlobalState.serverSideListener().txBytes);
                statEvent.setS2pChunks(sessionGlobalState.serverSideListener().rxChunks);
                statEvent.setP2sChunks(sessionGlobalState.serverSideListener().txChunks);

                UvmContextFactory.context().logEvent( statEvent );
            }

            /* Remove the vector from the active sessions table */
            /* You must remove the vector before razing, or else the
             * vector may receive a message(eg shutdown) from another
             * thread */
            activeSessions.remove( vector );
        } catch ( Exception e ) {
            logger.error( "Exception destroying pipeline", e );
        }

        try {
            /* Delete the vector */
            if ( vector != null ) {
                vector.raze();
                vector = null;
            }

            /* Delete everything else */
            raze();

            if ( logger.isDebugEnabled()) logger.debug( "Exiting thread: " + sessionGlobalState );
        } catch ( Exception e ) {
            logger.error( "Exception razing vector and session", e );
        }
    }

    public SessionTuple getClientSide()
    {
        return this.clientSide;
    }

    public SessionTuple getServerSide()
    {
        return this.serverSide;
    }

    public Long getPolicyId()
    {
        return this.policyId;
    }
    
    /**
     * Describe <code>connectServer</code> method here.
     *
     * @return a <code>boolean</code> true if the server was completed
     * OR we explicitly rejected
     */
    private boolean connectServerIfNecessary( SessionEvent sessionEvent )
    {
        boolean serverActionCompleted = true;
        switch ( state ) {
        case IPNewSessionRequestImpl.REQUESTED:
            /* If the server doesn't complete, we have to "vector" the reset */
            boolean connected = serverComplete( sessionEvent );
            if ( ! connected ) {
                if ( vectorReset() ) {
                    /* Forward the rejection type that was passed from the server */
                    state        = IPNewSessionRequestImpl.REJECTED;
                    rejectCode = REJECT_CODE_SRV;
                    serverActionCompleted = false;
                } else {
                    state = IPNewSessionRequestImpl.ENDPOINTED;
                }
            }
            break;

            /* Nothing to do on the server side */
        case IPNewSessionRequestImpl.ENDPOINTED: /* fallthrough */
        case IPNewSessionRequestImpl.REJECTED: /* fallthrough */
        case IPNewSessionRequestImpl.REJECTED_SILENT: /* fallthrough */
            break;

        default:
            throw new IllegalStateException( "Invalid state" );

        }
        return serverActionCompleted;
    }

    /**
     * Describe <code>connectClient</code> method here.
     *
     * @return a <code>boolean</code> true if the client was completed
     * OR we explicitly rejected.
     */
    private boolean connectClientIfNecessary()
    {
        boolean clientActionCompleted = true;

        switch ( state ) {
        case IPNewSessionRequestImpl.REQUESTED:
        case IPNewSessionRequestImpl.ENDPOINTED:
            if ( !clientComplete()) {
                logger.debug( "Unable to complete connection to client" );
                state = IPNewSessionRequestImpl.REJECTED;
                clientActionCompleted = false;
            }
            break;

        case IPNewSessionRequestImpl.REJECTED:
            logger.debug( "Rejecting session" );
            clientReject();
            break;

        case IPNewSessionRequestImpl.REJECTED_SILENT:
            logger.debug( "Rejecting session silently" );
            clientRejectSilent();
            break;

        default:
            throw new IllegalStateException( "Invalid state" );
        }

        return clientActionCompleted;
    }

    protected void buildPipeline() 
    {
        LinkedList<Relay> relayList = new LinkedList<Relay>();

        if ( sessionList.isEmpty() ) {
            if ( state == IPNewSessionRequestImpl.ENDPOINTED ) {
                throw new IllegalStateException( "Endpointed session without any nodes" );
            }

            clientSource = makeClientSource();
            clientSink   = makeClientSink();
            serverSource = makeServerSource();
            serverSink   = makeServerSink();

            relayList.add( new Relay( clientSource, serverSink ));
            relayList.add( new Relay( serverSource, clientSink ));
        } else {
            Sink   prevSink = null;
            Source prevSource = null;

            boolean first = true;
            NodeSessionImpl prevSession = null;
            Iterator<NodeSessionImpl> iter = sessionList.iterator();
            do {
                NodeSessionImpl session = null;
                try { session = iter.next(); } catch ( Exception e ) {};

                Source source;
                Sink sink;
                
                if ( session != null ) {
                    source = session.clientOutgoingSocketQueue();
                    sink   = session.clientIncomingSocketQueue();
                } else { 
                    // If session is null, we are past the end of the list
                    // as such, wrap things up by using the actual server source/sink
                    source = makeServerSource();
                    sink   = makeServerSink();
                }
                if ( first ) {
                    // If this is the first node, start things with the actual client source/sink
                    prevSource = makeClientSource();
                    prevSink = makeClientSink();
                    first = false;
                }

                Relay c2sInputRelay = new Relay( prevSource, sink );
                Relay s2cOutputRelay = new Relay( source, prevSink );

                relayList.add( c2sInputRelay );
                relayList.add( s2cOutputRelay );

                if ( session == null )
                    break;
                
                prevSource = session.serverOutgoingSocketQueue();
                prevSink = session.serverIncomingSocketQueue();
                prevSession = session;

                if ( logger.isDebugEnabled()) {
                    logger.debug( "NetcapHook: buildPipeline - added session: " + session );
                }

                session.pipelineConnector().addSession( session );
            } while ( true ) ;
        }

        printRelays( relayList );

        vector = new Vector( relayList );
    }

    @SuppressWarnings("fallthrough")
    protected void processSession( IPNewSessionRequestImpl request, NodeSessionImpl session )
    {
        if ( logger.isDebugEnabled())
            logger.debug( "Processing session: with state: " + request.state() + " session: " + session );

        switch ( request.state()) {
        case IPNewSessionRequestImpl.RELEASED:
            if ( session == null ) {
                /* Released sessions don't need a session, but for
                 * those that redirects may modify session
                 * parameters */
                break;
            }

            if ( session.isVectored()) {
                throw new IllegalStateException( "Released session trying to vector: " + request.state());
            }

            if ( logger.isDebugEnabled())
                logger.debug( "Adding released session: " + session );


            /* Add to the session list, and then move it in
             * buildPipeline, this way, any modifications to the
             * session will occur in order */
            sessionList.add( session );
            break;

        case IPNewSessionRequestImpl.ENDPOINTED:
            /* Set the state to endpointed */
            state = IPNewSessionRequestImpl.ENDPOINTED;

            /* fallthrough */
        case IPNewSessionRequestImpl.REQUESTED:
            if ( session == null ) {
                throw new IllegalStateException( "Session required for this state: " + request.state());
            }

            if ( logger.isDebugEnabled())
                logger.debug( "Adding session: " + session );

            sessionList.add( session );
            break;

        case IPNewSessionRequestImpl.REJECTED:
            rejectCode  = request.rejectCode();

            /* fallthrough */
        case IPNewSessionRequestImpl.REJECTED_SILENT:
            state = request.state();

            /* Done if the session wants to be notified of complete */
            if ( session != null ) sessionList.add( session );
            break;

        default:
            throw new IllegalStateException( "Invalid session state: " + request.state());
        }
    }

    /**
     * Call finalize on each node session that participates in this
     * session, also raze all of the sinks associated with the
     * sessionEvent.  This is just an extra precaution just in case they
     * were not razed by the pipeline.
     */
    private void razeSessions()
    {
        for ( Iterator<NodeSessionImpl> iter = sessionList.iterator() ; iter.hasNext() ; ) {
            NodeSessionImpl session = iter.next();
            session.raze();
        }

        for ( Iterator<NodeSessionImpl> iter = releasedSessionList.iterator() ; iter.hasNext() ; ) {
            NodeSessionImpl session = iter.next();
            /* Raze all of the released sessions */
            session.raze();
        }

        if ( clientSource != null ) clientSource.raze();
        if ( clientSink   != null ) clientSink.raze();
        if ( serverSource != null ) serverSource.raze();
        if ( serverSink   != null ) serverSink.raze();
    }

    /**
     * Call this to fake vector a reset before starting vectoring</p>
     * @return True if the reset made it all the way through, false if
     *   a node endpointed.
     */
    private boolean vectorReset()
    {
        logger.debug( "vectorReset: " + state );

        /* No need to vector, the session wasn't even requested */
        if ( state != IPNewSessionRequestImpl.REQUESTED ) return true;

        int size = sessionList.size();
        boolean isEndpointed = false;
        // Iterate through each session passing the reset.
        ResetCrumb reset = ResetCrumb.getInstanceNotAcked();

        for ( ListIterator<NodeSessionImpl> iter = sessionList.listIterator( size ) ; iter.hasPrevious(); ) {
            NodeSessionImpl session = iter.previous();

            if ( !session.isVectored()) {
                logger.debug( "vectorReset: skipping non-vectored session" );
                continue;
            }

            session.serverIncomingSocketQueue().send_event( reset );

            /* Empty the server queue */
            while ( session.serverIncomingSocketQueue() != null && !session.serverIncomingSocketQueue().isEmpty() ) {
                logger.debug( "vectorReset: Removing crumb left in IncomingSocketQueue:" );
                session.serverIncomingSocketQueue().read();
            }

            /* Indicate that the server is shutdown */
            session.setServerShutdown(true);

            /* Check if they passed the reset */
            if ( session.clientOutgoingSocketQueue() != null && session.clientOutgoingSocketQueue().isEmpty() ) {

                logger.debug( "vectorReset: ENDPOINTED by " + session );
                isEndpointed = true;
                
            } else {

                if ( session.clientOutgoingSocketQueue() != null && !session.clientOutgoingSocketQueue().containsReset() ) {
                    /* Sent data or non-reset, catch this error. */
                    logger.error( "Sent non-reset crumb before vectoring." );
                }

                if ( logger.isDebugEnabled()) {
                    logger.debug( "vectorReset: " + session + " passed reset" );
                }

                session.setClientShutdown( true );
            }
        }

        logger.debug( "vectorReset: isEndpointed - " + isEndpointed );

        return !isEndpointed;
    }

    protected boolean alive()
    {
        if ( state == IPNewSessionRequestImpl.REQUESTED || state == IPNewSessionRequestImpl.ENDPOINTED ) {
            return true;
        }

        return false;
    }

    protected void printRelays( List<Relay> relayList )
    {
        if ( logger.isDebugEnabled()) {
            logger.debug( "Relays: " );
            for ( Iterator<Relay>iter = relayList.iterator() ; iter.hasNext() ;) {
                Relay relay = iter.next();
                logger.debug( "" + relay.source() + " --> " + relay.sink());
            }
        }
    }

    /* Get the desired timeout for the vectoring machine */
    protected abstract int  timeout();

    protected abstract NetcapSession netcapSession();

    protected abstract SideListener clientSideListener();
    protected abstract SideListener serverSideListener();

    /**
     * Complete the connection to the server, returning whether or not
     * the connection was succesful.
     * @return - True connection was succesful, false otherwise.
     */
    protected abstract boolean serverComplete( SessionEvent sessionEvent );

    /**
     * Complete the connection to the client, returning whether or not the
     * connection was succesful
     * @return - True connection was succesful, false otherwise.
     */
    protected abstract boolean clientComplete();
    protected abstract void clientReject();
    protected abstract void clientRejectSilent();

    protected abstract Sink makeClientSink();
    protected abstract Sink makeServerSink();
    protected abstract Source makeClientSource();
    protected abstract Source makeServerSource();

    protected abstract void initializeNodeSessions( SessionEvent sessionEvent );

    protected abstract void raze();
}
