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
import org.opendaylight.l2switch.flow.middlebox.SnortRuleDB;
import org.opendaylight.l2switch.flow.chain.PolicyStatus;
import org.opendaylight.l2switch.flow.json.DevPolicy;
import org.opendaylight.l2switch.flow.json.ProtectionDetails;
import org.opendaylight.l2switch.flow.json.ContOpts;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.rauschig.jarchivelib.*;
import java.io.File;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import org.opendaylight.l2switch.flow.docker.utils.ExecShellCmd;

import java.sql.Timestamp;

import java.util.Iterator; 
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.lang.Thread;
import java.util.regex.Matcher;
import java.util.regex.Pattern;



public class MsgAnalysis {
    private String msg;
    private TransitionFeatures feature;
    private NmapParser nmapParser;
    private String srcMac;
    private HashMap<String, PolicyStatus> policyMap;
    private DevPolicy policy;
    private SnortRuleDB rulesDB = new SnortRuleDB();

    public MsgAnalysis(String msg, TransitionFeatures feature) {
	this.msg = msg;
	this.feature = feature; //transition: <feature>:<transitionkey>
    }


    public MsgAnalysis(String msg, String policyEntry) {
	this.msg = msg;
	TransitionFeatures inputFeatures = new TransitionFeatures(policyEntry);
	this.feature = inputFeatures;
    }

    public MsgAnalysis(String msg, DevPolicy policy, HashMap<String, PolicyStatus> policyMap, String srcMac){
    	this.msg = msg;
    	this.policyMap = policyMap;
    	this.srcMac = srcMac;
    	this.policy = policy;
    	String policyEntry = policy.getTransition()[policyMap.get(srcMac).getStateNum()];
    	System.out.println("Transitioning based upon: " + policyEntry);
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


    private void buildCVETar(String tarpath, List<String> entries){
    	File f = new File(tarpath); // the tar (rules_radio.tar)
    	File dest = new File(f.getParent()); //where to store the contents of tar (where to put local.rules)
    	File rules = new File(f.getParent() + "/local.rules");    	
    	if(f.exists()){
	    // if a rules_radio.tar already exists, there will almost certainly be conflict, so we old it
	    Timestamp ts = new Timestamp(System.currentTimeMillis());
	    File old = new File(tarpath+"."+ts.getTime());
	    f.renameTo(old);
	}
    	try{
	    FileWriter writer = new FileWriter(rules, false);
	    for(String entry:entries){
		writer.write(entry);
	    }
	    writer.close();
	    Archiver archiver = ArchiverFactory.createArchiver("tar");
	    archiver.create(f.getName(), dest, rules);	    		
	    rules.delete(); // delete created local.rules file	
	}catch(IOException ioe){
	    System.out.println("IOException: " + ioe.getMessage());
	}
    }
	    

    private void buildIPtablesTar(String tarpath, String type, int value){
    	File f = new File(tarpath); // the tar (rules_radio.tar)
    	File dest = new File(f.getParent()); //where to store the contents of tar (where to put local.rules)
    	File rules = new File(f.getParent() + "/setup_iptables.sh");
	String msg="";
    	if(f.exists()){
	    // if a rules_radio.tar already exists, there will almost certainly be conflict, so we old it
	    Timestamp ts = new Timestamp(System.currentTimeMillis());
	    File old = new File(tarpath+"."+ts.getTime());
	    f.renameTo(old);
	}
	if(type.equals("connlimit")){
	    msg = String.format("iptables -A FORWARD -p tcp -m tcp --tcp-flags FIN,SYN,RST,ACK SYN -m connlimit --connlimit-above %d --connlimit-mask 32 --connlimit-saddr -j REJECT --reject-with tcp-reset", value);
	}
    	try{
	    FileWriter writer = new FileWriter(rules, false);
	    writer.write(msg);
	    writer.close();
	    Archiver archiver = ArchiverFactory.createArchiver("tar");
	    archiver.create(f.getName(), dest, rules);	    		
	    rules.delete(); // delete created local.rules file	
	}catch(IOException ioe){
	    System.out.println("IOException: " + ioe.getMessage());
	}
    }
    
    private void buildTar(String tarpath, String msg){
    	File f = new File(tarpath); // the tar (rules_radio.tar)
    	File dest = new File(f.getParent()); //where to store the contents of tar (where to put local.rules)
    	File rules = new File(f.getParent() + "/local.rules");    	
    	if(f.exists()){
	    // if a rules_radio.tar already exists, there will almost certainly be conflict, so we old it
	    Timestamp ts = new Timestamp(System.currentTimeMillis());
	    File old = new File(tarpath+"."+ts.getTime());
	    f.renameTo(old);
	}
    	try{
	    FileWriter writer = new FileWriter(rules, false);
	    writer.write(msg);
	    writer.close();
	    Archiver archiver = ArchiverFactory.createArchiver("tar");
	    archiver.create(f.getName(), dest, rules);	    		
	    rules.delete(); // delete created local.rules file	
	}catch(IOException ioe){
	    System.out.println("IOException: " + ioe.getMessage());
	}
    }

    private void buildTar(String tarpath, List<String> offendingPorts){
    	File f = new File(tarpath); // the tar
    	File dest = new File(f.getParent()); //where to store the contents of tar (where to put local.rules)
    	File rules = new File(f.getParent() + "/local.rules");
    	if(f.exists()){
	    try{
		Archiver archiver = ArchiverFactory.createArchiver("tar");
		archiver.extract(f, dest);	    		
		if (rules.exists()){	    			
		    Iterator itr = offendingPorts.iterator();
		    FileWriter writer = new FileWriter(rules, true);
		    String notify = "#----Appended by NMAP middlebox----\n";
		    int sid = 0;
		    writer.write(notify);
		    while(itr.hasNext()){
			String snortRule = String.format("drop tcp any any -> 192.1.1.0/24 %s (msg: \"TCP packet rejected\"; sid:200000%s; rev:3;) \n", itr.next().toString(), String.valueOf(sid));
			System.out.println(snortRule);		
			writer.write(snortRule);
			sid+=1;
		    }
		    writer.close();		
		}
		Timestamp ts = new Timestamp(System.currentTimeMillis());
		File old = new File(tarpath+"."+ts.getTime());
		if(f.exists()){
		    f.renameTo(old);
		}
		archiver.create(f.getName(), new File(f.getParent()), rules);    		
		rules.delete(); // delete the extracted local.rules	    		
	    }catch(IOException ioe){
		System.out.println("problem extracting or problem writing: " + ioe.getMessage());
	    }
    	}else{
	    // we need to create a tar (for the demos, we assume it already exists)
	    try{
		Iterator itr = offendingPorts.iterator();
		FileWriter writer = new FileWriter(rules, false);
		String notify = "#----Appended by NMAP middlebox----\n";
		int sid = 0;
		writer.write(notify);
		while(itr.hasNext()){
		    String snortRule = String.format("drop tcp any any -> 192.1.1.0/24 %s (msg: \"TCP packet rejected\"; sid:200000%s; rev:3;) \n", itr.next().toString(), String.valueOf(sid));
		    System.out.println(snortRule);		
		    writer.write(snortRule);
		    sid+=1;
		}
		writer.close();		
		Archiver archiver = ArchiverFactory.createArchiver("tar");
		archiver.create(f.getName(), new File(f.getParent()), rules);	    		
		rules.delete(); // delete the extracted local.rules	    		
	    }catch(IOException ioe){
		System.out.println("problem extracting or problem writing: " + ioe.getMessage());
	    }
    	}
    }

    public boolean analyze() {
	boolean trigger = false;
	if (feature.getMbox().equals("snort")) {
	    trigger = processSnortMsg(feature.getKey());
	}
	if (feature.getMbox().equals("nmap")) {
	    trigger = processNmapMsg(feature.getKey());
		
	}
	if (feature.getMbox().equals("radio")) {
	    trigger = processRadioMsg(feature.getKey());
	}
	if (feature.getMbox().equals("dos")){
	    trigger = processDosMsg(feature.getKey());
	}
	return trigger;
    }

    private boolean processDosMsg(String transitionKey) {
    	boolean out = true;
	String type = transitionKey.substring(0, transitionKey.indexOf("-"));
	int connLimit = Integer.parseInt(transitionKey.substring(transitionKey.indexOf("_")+1));
	String[] dosMsgParts = msg.split("=");
	int connCount = Integer.parseInt(dosMsgParts[1].replaceAll("\\D+",""));
	if(connCount>connLimit){
	    System.out.println("Able to start more connections that policy allows");	    
	    int nextStateIndex = policyMap.get(srcMac).getStateNum() + 1;
	    String tarpath = policy.getProtections()[nextStateIndex].getImageOpts()[0].getArchives()[0].getTar();
	    buildIPtablesTar(tarpath, type, connLimit);
	}
    	return out;
    }
    
    private boolean processRadioMsg(String transitionkey) {
    	boolean out = true;
    	int nextStateIndex = policyMap.get(srcMac).getStateNum() + 1;
    	String tarpath = policy.getProtections()[nextStateIndex].getImageOpts()[0].getArchives()[0].getTar();
    	buildTar(tarpath, msg);
    	return out;
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
	List<String> offendingPorts = new ArrayList<>();
	List<String> CVEs = new ArrayList<String>();

	String type = transitionKey.substring(0, transitionKey.indexOf("_"));
	    
	if (type.equals("openports")) { //open ports list in <regex>
	    nmapParser = new NmapParser(msg,type);
	    List<String> allowedPorts = Arrays.asList(transitionKey.substring(transitionKey.indexOf("_") +1, transitionKey.length()).split(","));
	    System.out.print("Allowed ports: ");
	    for(String s:allowedPorts){
		System.out.print("|" + s + "|");
	    }
	    System.out.println("");
	    List<String> openPorts = nmapParser.getOpenPorts(); 
	    for (String port : openPorts){
		if(!allowedPorts.contains(port.toString())){				
		    offendingPorts.add(port);
		}
	    }
		
	} else if (type.equals("CVE")){
	    // future impl to include NSE scripts which check for CVEs
	    Matcher m = Pattern.compile("(?=(cve))").matcher(msg.toLowerCase());
	    while (m.find()){
		CVEs.add(msg.substring(m.start(), m.start()+12));
	    }
	    if(!CVEs.isEmpty())
		System.out.println("got cve request");
	}
		
	if(policyMap.get(srcMac).getCanTransition()){
	    out = true;
	    int nextStateIndex = policyMap.get(srcMac).getStateNum() + 1;
	    String tarpath = policy.getProtections()[nextStateIndex].getImageOpts()[0].getArchives()[0].getTar();
	    if(!offendingPorts.isEmpty()){
		buildTar(tarpath, offendingPorts);
	    }
	    else if(!CVEs.isEmpty()){
		System.out.println("get snort rule, add to tar");
		List<String>rules = new ArrayList<String>();
		for (String CVE:CVEs){
		    rules.add(rulesDB.getRule(CVE));
		}
		buildCVETar(tarpath, rules);
	    }
	}else{
	    System.out.println("Error - expecting a state after nmap but no transition in JSON. \n Please check your JSON.");
	}
	return out;
    }
    
}
