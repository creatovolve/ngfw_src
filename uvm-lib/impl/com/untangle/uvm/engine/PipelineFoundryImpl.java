/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.untangle.uvm.engine;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import com.untangle.uvm.LocalUvmContextFactory;
import com.untangle.uvm.argon.ArgonAgent;
import com.untangle.uvm.argon.PipelineDesc;
import com.untangle.uvm.argon.SessionEndpoints;
import com.untangle.uvm.localapi.LocalIntfManager;
import com.untangle.uvm.logging.EventLogger;
import com.untangle.uvm.node.IPSessionDesc;
import com.untangle.uvm.node.InterfaceComparator;
import com.untangle.uvm.node.LocalNodeManager;
import com.untangle.uvm.node.PipelineEndpoints;
import com.untangle.uvm.node.PipelineStats;
import com.untangle.uvm.policy.LocalPolicyManager;
import com.untangle.uvm.policy.Policy;
import com.untangle.uvm.policy.PolicyRule;
import com.untangle.uvm.policy.UserPolicyRule;
import com.untangle.uvm.vnet.CasingPipeSpec;
import com.untangle.uvm.vnet.Fitting;
import com.untangle.uvm.vnet.MPipe;
import com.untangle.uvm.vnet.PipeSpec;
import com.untangle.uvm.vnet.Pipeline;
import com.untangle.uvm.vnet.PipelineFoundry;
import com.untangle.uvm.vnet.SoloPipeSpec;

/**
 * Implements PipelineFoundry.
 * 
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
public class PipelineFoundryImpl implements PipelineFoundry {
    private static final PipelineFoundryImpl PIPELINE_FOUNDRY_IMPL = new PipelineFoundryImpl();

    private static final EventLogger eventLogger = UvmContextImpl.context()
            .eventLogger();
    private final Logger logger = Logger.getLogger(getClass());

    private final Map<Fitting, List<MPipe>> mPipes = new HashMap<Fitting, List<MPipe>>();
    private final Map<MPipe, MPipe> casings = new HashMap<MPipe, MPipe>();

    private final Map<InetSocketAddress, Fitting> connectionFittings = new ConcurrentHashMap<InetSocketAddress, Fitting>();

    private final Map<Integer, PipelineImpl> pipelines = new ConcurrentHashMap<Integer, PipelineImpl>();

    // These don't need to be concurrent and being able to use a null key
    // is currently useful for the null policy.
    private static final Map<Policy, Map<Fitting, List<MPipeFitting>>> chains = new HashMap<Policy, Map<Fitting, List<MPipeFitting>>>();
    
    private PipelineFoundryImpl() {
    }

    public static PipelineFoundryImpl foundry() {
        return PIPELINE_FOUNDRY_IMPL;
    }

    public PipelineDesc weld(IPSessionDesc sd) {
        Long t0 = System.nanoTime();

        PolicyRule pr = selectPolicy(sd);
        if (pr == null) {
            logger.error("No policy rule found for session " + sd);
        }
        Policy policy = null == pr ? null : pr.getPolicy();


        InetAddress sAddr = sd.serverAddr();
        int sPort = sd.serverPort();

        InetSocketAddress socketAddress = new InetSocketAddress(sAddr, sPort);
        Fitting start = connectionFittings.remove(socketAddress);

        if (SessionEndpoints.PROTO_TCP == sd.protocol()) {
            if (null == start) {
                switch (sPort) {
                case 21:
                    start = Fitting.FTP_CTL_STREAM;
                    break;

                case 25:
                    start = Fitting.SMTP_STREAM;
                    break;

                case 80:
                    start = Fitting.HTTP_STREAM;
                    break;

                case 110:
                    start = Fitting.POP_STREAM;
                    break;

                case 143:
                    start = Fitting.IMAP_STREAM;
                    break;

                default:
                    start = Fitting.OCTET_STREAM;
                    break;
                }
            }
        } else {
            start = Fitting.OCTET_STREAM; // XXX we should have UDP hier.
        }

        long ct0 = System.nanoTime();
        List<MPipeFitting> chain = makeChain(sd, policy, start);
        long ct1 = System.nanoTime();

        // filter list
        long ft0 = System.nanoTime();

        List<ArgonAgent> al = new ArrayList<ArgonAgent>(chain.size());
        List<MPipeFitting> ml = new ArrayList<MPipeFitting>(chain.size());

        MPipe end = null;

        UvmContextImpl upi = UvmContextImpl.getInstance();
                       
        for (Iterator<MPipeFitting> i = chain.iterator(); i.hasNext();) {
            MPipeFitting mpf = i.next();

            if (null != end) {
                if (mpf.mPipe == end) {
                    end = null;
                }
            } else {
                MPipe mPipe = mpf.mPipe;
                PipeSpec pipeSpec = mPipe.getPipeSpec();

                // We want the node if its policy matches (this policy or one of
                // is parents), or the node has no
                // policy (is a service).
                if (pipeSpec.matches(sd)) {
                    ml.add(mpf);
                    /* XXXX Nasty cast */
                    al.add(((MPipeImpl) mPipe).getArgonAgent());
                } else {
                    end = mpf.end;
                }
            }
        }

        long ft1 = System.nanoTime();

        PipelineImpl pipeline = new PipelineImpl(sd.id(), ml);
        pipelines.put(sd.id(), pipeline);

        Long t1 = System.nanoTime();
        if (logger.isDebugEnabled()) {
            logger.debug("sid: " + sd.id() + " pipe in " + (t1 - t0)
                    + " made: " + (ct1 - ct0) + " filtered: " + (ft1 - ft0)
                    + " chain: " + ml);
        }

        return new PipelineDesc(pr, al);
    }

    public PipelineEndpoints createInitialEndpoints(IPSessionDesc start) {
        return new PipelineEndpoints(start);
    }

    public void registerEndpoints(PipelineEndpoints pe) {
        eventLogger.log(pe);
    }

    public void destroy(IPSessionDesc start, IPSessionDesc end,
            PipelineEndpoints pe, String uid) {
        PipelineImpl pipeline = pipelines.remove(start.id());

        if (logger.isDebugEnabled()) {
            logger.debug("removed: " + pipeline + " for: " + start.id());
        }

        // Endpoints can be null, if the session was never properly
        // set up at all (unknown server interface for example)
        if (pe != null)
            eventLogger.log(new PipelineStats(start, end, pe, uid));

        pipeline.destroy();
    }

    public void registerMPipe(MPipe mPipe) {
        SoloPipeSpec sps = (SoloPipeSpec) mPipe.getPipeSpec();
        Fitting f = sps.getFitting();

        List<MPipe> l = mPipes.get(f);

        if (null == l) {
            l = new ArrayList<MPipe>();
            l.add(null);
            mPipes.put(f, l);
        }

        int i = Collections.binarySearch(l, mPipe, MPipeComparator.COMPARATOR);
        l.add(0 > i ? -i - 1 : i, mPipe);

        clearCache();
    }

    public void deregisterMPipe(MPipe mPipe) {
        SoloPipeSpec sps = (SoloPipeSpec) mPipe.getPipeSpec();
        Fitting f = sps.getFitting();

        List<MPipe> l = mPipes.get(f);

        int i = Collections.binarySearch(l, mPipe, MPipeComparator.COMPARATOR);
        if (0 > i) {
            logger.warn("deregistering nonregistered pipe");
        } else {
            l.remove(i);
        }

        clearCache();
    }

    public void registerCasing(MPipe insideMPipe, MPipe outsideMPipe) {
        if (insideMPipe.getPipeSpec() != outsideMPipe.getPipeSpec()) {
            throw new IllegalArgumentException("casing constraint violated");
        }

        synchronized (this) {
            casings.put(insideMPipe, outsideMPipe);
            clearCache();
        }
    }

    public void deregisterCasing(MPipe insideMPipe) {
        synchronized (this) {
            casings.remove(insideMPipe);
            clearCache();
        }
    }

    public void registerConnection(InetSocketAddress socketAddress,
            Fitting fitting) {
        connectionFittings.put(socketAddress, fitting);
    }

    public Pipeline getPipeline(int sessionId) {
        return (Pipeline) pipelines.get(sessionId);
    }
    
    /* Remove all of the cached chains */
    public void clearChains()
    {
        synchronized( this ) {
            clearCache();
        }
    }


    // package protected methods ----------------------------------------------

    PolicyRule selectPolicy(IPSessionDesc sd) {
        UvmContextImpl upi = UvmContextImpl.getInstance();
        LocalPolicyManager pmi = upi.policyManager();
        LocalIntfManager im = upi.localIntfManager();

        UserPolicyRule[] userRules = pmi.getUserRules();

        InterfaceComparator c = im.getInterfaceComparator();

        for (UserPolicyRule upr : userRules) {
            if (upr.matches(sd, c)) {
                return upr;
            }
        }

        return pmi.getDefaultPolicyRule();
    }

    // private methods --------------------------------------------------------

    private List<MPipeFitting> makeChain(IPSessionDesc sd, Policy p,
            Fitting start) {
        List<MPipeFitting> mPipeFittings = null;

        /*
         * Check if there is a cache for this policy. First time is without the
         * lock
         */
        Map<Fitting, List<MPipeFitting>> fcs = chains.get(p);

        /* If there is a cache, check if the chain exists for this fitting */
        if (null != fcs) {
            mPipeFittings = fcs.get(start);
        }

        if (null == mPipeFittings) {
            synchronized (this) {
                /* Check if there is a cache again, after grabbing the lock */
                fcs = chains.get(p);

                if (null == fcs) {
                    /* Cache doesn't exist, create a new one */
                    fcs = new HashMap<Fitting, List<MPipeFitting>>();
                    chains.put(p, fcs);
                } else {
                    /* Cache exists, get the chain for this fitting */
                    mPipeFittings = fcs.get(start);
                }

                if (null == mPipeFittings) {
                    /*
                     * Chain hasn't been created, create a list of available
                     * casings
                     */
                    Map<MPipe, MPipe> availCasings = new HashMap<MPipe, MPipe>(
                            casings);

                    /*
                     * Chain hasn't been created, create a list of available
                     * nodes mPipes is ordered so iterating the list of mPipes
                     * will insert them in the correct order
                     */
                    Map<Fitting, List<MPipe>> availMPipes = new HashMap<Fitting, List<MPipe>>(
                            mPipes);

                    int s = availCasings.size() + availMPipes.size();
                    mPipeFittings = new ArrayList<MPipeFitting>(s);

                    /* Weld together the nodes and the casings */
                    weld(mPipeFittings, start, p, availMPipes, availCasings);
                    
                    removeDuplicates(p, mPipeFittings);

                    fcs.put(start, mPipeFittings);
                }
            }
        }

        return mPipeFittings;
    }

    private void weld(List<MPipeFitting> mPipeFittings, Fitting start,
            Policy p, Map<Fitting, List<MPipe>> availMPipes,
            Map<MPipe, MPipe> availCasings) {
        weldMPipes(mPipeFittings, start, p, availMPipes, availCasings);
        weldCasings(mPipeFittings, start, p, availMPipes, availCasings);
    }

    private boolean weldMPipes(List<MPipeFitting> mPipeFittings, Fitting start,
            Policy p, Map<Fitting, List<MPipe>> availMPipes,
            Map<MPipe, MPipe> availCasings) {
        UvmContextImpl upi = UvmContextImpl.getInstance();
        LocalPolicyManager pmi = upi.policyManager();

        boolean welded = false;

        boolean tryAgain;
        do {
            tryAgain = false;
            /* Iterate the Map Fittings to available Nodes */
            for (Iterator<Fitting> i = availMPipes.keySet().iterator(); i
                    .hasNext();) {
                Fitting f = i.next();
                if (start.instanceOf(f)) {
                    /*
                     * If this fitting is an instance of the start, get the list
                     * of nodes
                     */
                    List<MPipe> l = availMPipes.get(f);

                    /* Remove this list of nodes from the available nodes */
                    i.remove();

                    for (Iterator<MPipe> j = l.iterator(); j.hasNext();) {
                        MPipe mPipe = j.next();
                        if (null == mPipe) {
                            boolean w = weldCasings(mPipeFittings, start, p,
                                    availMPipes, availCasings);
                            if (w) {
                                welded = true;
                            }
                        } else if (pmi.matchesPolicy(mPipe.getPipeSpec()
                                .getNode(), p)) {
                            MPipeFitting mpf = new MPipeFitting(mPipe, start);
                            boolean w = mPipeFittings.add(mpf);
                            if (w) {
                                welded = true;
                            }
                        }
                    }
                    tryAgain = true;
                    break;
                }
            }
        } while (tryAgain);

        return welded;
    }

    private boolean weldCasings(List<MPipeFitting> mPipeFittings,
            Fitting start, Policy p, Map<Fitting, List<MPipe>> availMPipes,
            Map<MPipe, MPipe> availCasings) {
        UvmContextImpl upi = UvmContextImpl.getInstance();
        LocalPolicyManager pmi = upi.policyManager();

        boolean welded = false;

        boolean tryAgain;
        do {
            tryAgain = false;
            for (Iterator<MPipe> i = availCasings.keySet().iterator(); i
                    .hasNext();) {
                MPipe insideMPipe = i.next();
                CasingPipeSpec ps = (CasingPipeSpec) insideMPipe.getPipeSpec();
                Fitting f = ps.getInput();

                if (!pmi.matchesPolicy(ps.getNode(), p)) {
                    i.remove();
                } else if (start.instanceOf(f)) {
                    MPipe outsideMPipe = availCasings.get(insideMPipe);
                    i.remove();
                    int s = mPipeFittings.size();
                    mPipeFittings.add(new MPipeFitting(insideMPipe, start,
                            outsideMPipe));
                    CasingPipeSpec cps = (CasingPipeSpec) insideMPipe
                            .getPipeSpec();
                    Fitting insideFitting = cps.getOutput();

                    boolean w = weldMPipes(mPipeFittings, insideFitting, p,
                            availMPipes, availCasings);

                    if (w) {
                        welded = true;
                        mPipeFittings.add(new MPipeFitting(outsideMPipe,
                                insideFitting));
                    } else {
                        while (mPipeFittings.size() > s) {
                            mPipeFittings.remove(mPipeFittings.size() - 1);
                        }
                    }

                    tryAgain = true;
                    break;
                }
            }
        } while (tryAgain);

        return welded;
    }

    private void clearCache() {
        chains.clear();
    }
    
    private void removeDuplicates(Policy policy, List<MPipeFitting> chain) {
        LocalPolicyManager pmi = LocalUvmContextFactory.context()
                .policyManager();
        LocalNodeManager nodeManager = LocalUvmContextFactory.context()
                .nodeManager();

        Set<String> enabledNodes = nodeManager.getEnabledNodes(policy);

        Map<String, Integer> numParents = new HashMap<String, Integer>();
        Map<MPipeFitting, Integer> fittingDistance = new HashMap<MPipeFitting, Integer>();

        for (Iterator<MPipeFitting> i = chain.iterator(); i.hasNext();) {
            MPipeFitting mpf = i.next();

            Policy nodePolicy = mpf.mPipe.node().getTid().getPolicy();

            if (nodePolicy == null) {
                continue;
            }

            String nodeName = mpf.mPipe.node().getNodeDesc().getName();
            /* Remove the items that are not enabled in this policy */
            if (!enabledNodes.contains(nodeName)) {
                i.remove();
                continue;
            }

            Integer n = numParents.get(nodeName);
            int distance = pmi.getNumParents(policy, nodePolicy);

            if (distance < 0) {
                /* Removing nodes that are not in this policy */
                logger.info("The policy " + policy.getName()
                        + " is not a child of " + nodePolicy.getName());
                i.remove();
                continue;
            }

            fittingDistance.put(mpf, distance);

            /* If an existing node is closer then this node, remove this node. */
            if (n == null) {
                /*
                 * If we haven't seen another node at any distance, add it to
                 * the hash
                 */
                numParents.put(nodeName, distance);
                continue;
            } else if (distance == n) {
                /* Keep nodes at the same distance */
                continue;
            } else if (distance < n) {
                /*
                 * Current node is closer then the other one, have to remove the
                 * other node done on another iteration
                 */
                numParents.put(nodeName, distance);
            }
        }

        for (Iterator<MPipeFitting> i = chain.iterator(); i.hasNext();) {
            MPipeFitting mpf = i.next();

            Policy nodePolicy = mpf.mPipe.node().getTid().getPolicy();

            /* Keep items in the NULL Racks */
            if (nodePolicy == null) {
                continue;
            }

            String nodeName = mpf.mPipe.node().getNodeDesc().getName();

            Integer n = numParents.get(nodeName);

            if (n == null) {
                logger
                        .info("Programming error, numParents null for non-null policy.");
                continue;
            }

            Integer distance = fittingDistance.get(mpf);

            if (distance == null) {
                logger
                        .info("Programming error, distance null for a fitting.");
                continue;
            }

            if (distance > n) {
                i.remove();
            } else if (distance < n) {
                logger
                        .info("Programming error, numParents missing minimum value");
            }
        }

    }
    
}
