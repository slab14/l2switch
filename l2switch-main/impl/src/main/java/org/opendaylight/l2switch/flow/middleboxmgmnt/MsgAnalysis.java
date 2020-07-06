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


public class MsgAnalysis {
    private String msg;
    private TransitionFeatures feature;
    private NmapParser nmapParser;
    private String srcMac;
    private HashMap<String, PolicyStatus> policyMap;
    private DevPolicy policy;

    public MsgAnalysis(String msg, TransitionFeatures feature) {
	this.msg = msg;
	this.feature = feature; //transition: <feature>:<transitionkey>
    }


    public MsgAnalysis(String msg, String policyEntry) {
	this.msg = msg;
	//System.out.println("This is the TransitionEntry: " + policyEntry);
	TransitionFeatures inputFeatures = new TransitionFeatures(policyEntry);
	this.feature = inputFeatures;
    }

    public MsgAnalysis(String msg, DevPolicy policy, HashMap<String, PolicyStatus> policyMap, String srcMac){
    	this.msg = msg;
    	this.policyMap = policyMap;
    	this.srcMac = srcMac;
    	this.policy = policy;
    	String policyEntry = policy.getTransition()[policyMap.get(srcMac).getStateNum()];
    	//System.out.println("This is the TransitionEntry: " + policyEntry);
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

    	/*Timestamp ts = new Timestamp(System.currentTimeMillis());
    	File f = new File(tarpath);
    	File old = new File(tarpath+"."+ts.getTime());
    	if(f.exists()){
    		
    		f.renameTo(old);
    		//if there user has a another tar file with the same name, '.old' it to avoid conflict
    		//there is no logic to compare the tars
    	}
    	
		try{
			Iterator itr = offendingPorts.iterator();
			FileWriter writer = new FileWriter(f.getParent()+"/local.rules", false);
			while(itr.hasNext()){
				String snortRule = String.format("drop tcp any any -> 192.1.1.0/24 %s (msg: \"TCP packet rejected\"; sid:2000002; rev:3;) \n", itr.next().toString());
				System.out.println(snortRule);		
    			writer.write(snortRule);

    		}

    		writer.close();
    		OutputStream tar_output = new FileOutputStream(f);
            ArchiveOutputStream my_tar_ball = new ArchiveStreamFactory().createArchiveOutputStream(ArchiveStreamFactory.TAR, tar_output);
            File tar_input_file= new File(f.getParent()+"/local.rules");
            TarArchiveEntry tar_file = new TarArchiveEntry("local.rules");
            tar_file.setSize(tar_input_file.length());
            my_tar_ball.putArchiveEntry(tar_file);
            IOUtils.copy(new FileInputStream(tar_input_file), my_tar_ball);
            my_tar_ball.closeArchiveEntry();            
            my_tar_ball.finish(); 
            tar_output.close();

		}catch(IOException e){
			System.out.println("Something went wrong while reading the tarpath: " + tarpath);
			System.out.println(e.getMessage());
		}catch(ArchiveException ae){
			System.out.println("Archive exception");
			System.out.println(ae.getMessage());
		}    	*/
    }

    public boolean analyze() {
	boolean trigger = false;
	if (feature.getMbox().equals("snort")) {
	    trigger = processSnortMsg(feature.getKey());
	}
	if (feature.getMbox().equals("nmap")) {
		trigger = processNmapMsg(feature.getKey());
		
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
	    List<String> offendingPorts = new ArrayList<>();
	    
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
					
				}else{
					offendingPorts.add(port);
				}
			}

		}else if (transitionKey.substring(0, transitionKey.indexOf("_")).equals("CVE")){
			// future impl to include NSE scripts which check for CVEs
		}
		
	    if(!offendingPorts.isEmpty()){
	    	if(policyMap.get(srcMac).getCanTransition()){
	    		//System.out.println("Current policy state index: " + policyMap.get(srcMac).getStateNum());
	    		int nextStateIndex = policyMap.get(srcMac).getStateNum() + 1;
	    		String tarpath = policy.getProtections()[nextStateIndex].getImageOpts()[0].getArchives()[0].getTar();
	    		buildTar(tarpath, offendingPorts);
	    	}else{
	    		System.out.println("Error - expecting a state after nmap but no transition in JSON. \n Please check your JSON.");
	    	}
	    	out = true; 
	    }
     
    return out;
    }

}
