package com.metavize.tran.ids.options;

import org.apache.log4j.Logger;
import org.apache.log4j.Level;

import com.metavize.mvvm.tapi.event.*;
import com.metavize.tran.ids.IDSDetectionEngine;
import com.metavize.tran.ids.IDSRuleSignature;
import com.metavize.tran.ids.IDSSessionInfo;

public class DsizeOption extends IDSOption {
	
//	private IDSSessionInfo info;
	
	private static final Logger log = Logger.getLogger(DsizeOption.class);
	static {
		log.setLevel(Level.WARN);
	}
	
	int min;
	int max;
	public DsizeOption(IDSRuleSignature signature, String params) {
		super(signature, params);
		IDSSessionInfo info = signature.getSessionInfo();
		
		char ch = params.charAt(0);	
		String range[] = params.split("<>");
		try {
			if(range.length == 2) {
				min = Integer.parseInt(range[0].trim());
				max = Integer.parseInt(range[1].trim());
			}
		 	else if(ch == '<') {
				min = 0;
				max = Integer.parseInt(params.substring(1).trim());
		 	}
			else if(ch == '>') {
				min = Integer.parseInt(params.substring(1).trim());
				max = Integer.MAX_VALUE;
			}
			else 
				min = max = Integer.parseInt(params.trim());
		}
		catch(NumberFormatException e) { 
			log.error("Invalid Dsize param: " + params);
			min = 0;
			max = Integer.MAX_VALUE;
		}
	}
	
	public boolean runnable() {
		return true;
	}

	public boolean run() {	
		IPDataEvent event = super.getSignature().getSessionInfo().getEvent();
		int size = event.data().remaining();
		if(min <= size && max >= size)
			return true;
		return false;
	}
}
