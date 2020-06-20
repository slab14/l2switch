/*
 * Copyright (c) 2018 Slab and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.l2switch.flow.middlebox;

import org.opendaylight.l2switch.flow.middlebox.TransitionFeatures;
import org.opendaylight.l2switch.flow.middlebox.NmapParser;


import java.util.Iterator; 
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

public class MsgAnalysis {
    private String msg;
    private TransitionFeatures feature;
    private NmapParser nmapParser;

    public MsgAnalysis(String msg, TransitionFeatures feature) {
	this.msg = msg;
	this.feature = feature; //transition: <feature>:<transitionkey>
    }


    public MsgAnalysis(String msg, String policyEntry) {
	this.msg = msg;
	System.out.println("This is the TransitionEntry: " + policyEntry);
	TransitionFeatures inputFeatures = new TransitionFeatures(policyEntry);
	this.feature = inputFeatures;
    }

    public String getMsg(){
	return msg;
    }

    public TransitionFeatures getFeatures(){
	return feature;
    }

    public void setMsg(String msg) {
	this.msg = msg;
    }

    public void setFeatures(TransitionFeatures feature) {
	this.feature = feature;
    }

    public void setFeatures(String policyEntry) {
	TransitionFeatures inputFeatures = new TransitionFeatures(policyEntry);
	this.feature = inputFeatures;
    }

    public boolean analyze() {
	boolean trigger = false;
	if (feature.getMbox().equals("snort")) {
	    trigger = processSnortMsg(feature.getKey());
	}
	if (feature.getMbox().equals("nmap")) {
		trigger = processNmapMsg(feature.getKey());
		// ----snort tarring, cleanup
	}
	return trigger;
    }

    private boolean processSnortMsg(String transitionKey) {
	boolean out = false;
	String[] snortAlertParts = msg.split(",");
	String snortMsg = snortAlertParts[4];
	if (snortMsg.toLowerCase().contains(transitionKey.toLowerCase())) {
	    out = true;
	}
	return out;
    }

    private boolean processNmapMsg(String transitionKey) {
    // the log file is written to twice by nmap which means we need to save the first part of the alert (msg_part1)
	    boolean out = false;
	    List<String> offendingPorts = new ArrayList<>();;
	    System.out.println("processNmapMsg()...");
    	System.out.println("String starts with: " + msg.substring(0,5));
    	System.out.println("Ends with: " + msg.substring(msg.length() - 5));
		nmapParser = new NmapParser(msg);
		if (transitionKey.substring(0, transitionKey.indexOf("_")).equals("openports")) { //open ports list in <regex>
			
			List<String> allowedPorts = Arrays.asList(transitionKey.substring(transitionKey.indexOf("_") +1, transitionKey.length()).split(","));
			System.out.print("Allowed ports: ");
			for(String s:allowedPorts){
				System.out.print("|" + s + "|");
			}
			System.out.println("");
			List<String> openPorts = nmapParser.getOpenPorts(); 
			for (String port : openPorts){
				if(allowedPorts.contains(port.toString())){
					System.out.println("This port is allowed: " + port.toString());
				}else{
					System.out.println("This port not allowed: " + port.toString());
					offendingPorts.add(port);
				}
			}

		}else if (transitionKey.substring(0, transitionKey.indexOf("_")).equals("CVE")){
			// future impl to include NSE scripts which check for CVEs
		}
		/*Iterator itr = nmapParser.getOpenPorts().iterator();
		List openPorts = nmapParser.getOpenPorts(); 
	    while(itr.hasNext()){
	        System.out.println(itr.next());
	    }*/
	    if(!offendingPorts.isEmpty()){
	    	Iterator it = offendingPorts.iterator();
	    	while(it.hasNext()){
	    		System.out.println(it.next());
	    	}
	    }
     
    return out;
    }

}
