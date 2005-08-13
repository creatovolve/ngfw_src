package com.metavize.tran.ids;

import java.nio.*;
import java.util.List;
import java.util.Vector;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.log4j.Logger;
import org.apache.log4j.Level;

import com.metavize.mvvm.tapi.*;
import com.metavize.mvvm.tapi.event.*;
import com.metavize.mvvm.tran.firewall.IPMatcher;
import com.metavize.mvvm.tran.ParseException;
import com.metavize.mvvm.tran.PortRange;

public class IDSDetectionEngine {

	private int tmpcount = 0;
	private IDSRules rules = new IDSRules();
	Map<Integer,IDSSessionInfo> sessionInfoMap = new ConcurrentHashMap<Integer,IDSSessionInfo>();
	
	private static final Logger log = Logger.getLogger(IDSDetectionEngine.class);
	static {
		log.setLevel(Level.ERROR);
	}	
	private static IDSDetectionEngine instance = null; 
	public static IDSDetectionEngine instance() {
		if(instance == null) 
			instance = new IDSDetectionEngine();

		return instance;
	}

	private IDSDetectionEngine() {
		String test = "alert tcp 10.0.0.40-10.0.0.101 any -> 66.35.250.0/24 80 (content:\"slashdot\"; msg:\"OMG teH SLASHd0t\";)";
		String tesT = "alert tcp 10.0.0.1/24 any -> any any (content: \"spOOns|FF FF FF FF|spoons\"; msg:\"Matched binary FF FF FF and spoons\"; nocase;)";
		String TesT = "alert tcp 10.0.0.1/24 any -> any any (uricontent:\"slashdot\"; nocase; msg:\"Uricontent matched\";)";
		addRule(test);
		addRule(tesT);
		addRule(TesT);
	}

	public void addRule(String rule) {
		try {
			rules.addRule(rule);
		} catch (ParseException e) { 
			log.warn("Could not parse rule; " + e.getMessage()); 
		} catch (Exception e) {
			log.error("Some sort of really bad exception: " + e.getMessage());
			e.printStackTrace();
		}
	}

	public boolean processNewSession(IPNewSessionRequest session, Protocol protocol) {
		List<IDSRuleSignature> signatures = rules.matchesHeader(protocol, session.clientAddr(), session.clientPort(), session.serverAddr(), session.serverPort());
		if(signatures.size() > 0) {
			IDSSessionInfo info = getSessionInfo(session.id());
			if(info == null) {
				info = new IDSSessionInfo();
				info.setSignatures(signatures);
			}
			else
				info.setSignatures(signatures);
			session.attach(info);
		}
		else
			session.release();
		return false; // Fix me - not sure what I want to return
	}

	public IDSRules getRulesForTesting() {
		return rules;
	}
	
	public void handleChunk(IPDataEvent event, IPSession session, boolean isServer) {
		IDSSessionInfo info = (IDSSessionInfo) session.attachment();
		
		info.setSession(session);
		info.setEvent(event);
		info.setFlow(isServer);

		info.processSignatures();
	}

	public void mapSessionInfo(int id, IDSSessionInfo info) {
		sessionInfoMap.put(id,info);
	}

	public IDSSessionInfo getSessionInfo(int id) {
		return sessionInfoMap.get(id);
	}
}
