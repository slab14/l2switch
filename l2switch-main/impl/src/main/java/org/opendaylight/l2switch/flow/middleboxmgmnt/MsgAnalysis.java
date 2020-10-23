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
import org.opendaylight.l2switch.flow.middlebox.BuildTar;
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

    public MsgAnalysis(String msg, TransitionFeatures feature) {
	this.msg = msg;
	this.feature = feature;
    }


    public MsgAnalysis(String msg, String policyEntry) {
	this.msg = msg;
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
		
	}
	if (feature.getMbox().equals("radio")) {
	    trigger = processRadioMsg(feature.getKey());
	}
	if (feature.getMbox().equals("learn_radio")) {
	    trigger = processLearnRadioMsg(feature.getKey());
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

    private boolean processLearnRadioMsg(String transitionkey){
	boolean out = false;
	String[] radioMsgParts = msg.split(",");
	String modelData = radioMsgParts[0].replace(';', ',');
	String protoData = radioMsgParts[1];
	String portData = radioMsgParts[2];
	if(!(modelData.substring(0,6).equals("states") && protoData.substring(0,5).equals("#type")))
	    return out;
   	int curStateIndex = policyMap.get(srcMac).getStateNum();	
	File model = new File(policy.getProtections()[curStateIndex].getAddFiles()[0]);
	File proto = new File(policy.getProtections()[curStateIndex].getAddFiles()[1]);
	File port = new File(policy.getProtections()[curStateIndex].getAddFiles()[2]);	
	try{
	    FileWriter writeModel = new FileWriter(model, false);
	    writeModel.write(modelData);
	    writeModel.close();
	    FileWriter writeProto = new FileWriter(proto, false);
	    writeProto.write(protoData);
	    writeProto.close();
	    FileWriter writePort = new FileWriter(port, false);
	    writePort.write(portData);
	    writePort.close();
	}catch(IOException e){
	    System.out.println("IOException: "+e.getMessage());
	}
	ExecShellCmd obj = new ExecShellCmd();
	String radio_rules_loc = "/etc/sec_gate/radio/local.rules";

	String cmd = String.format("/usr/bin/python3 /etc/sec_gate/radio/newModel2Rule.py -M %s -P %s -s %s -n 'RADIO' -R %s", policy.getProtections()[curStateIndex].getAddFiles()[0], policy.getProtections()[curStateIndex].getAddFiles()[1], policy.getProtections()[curStateIndex].getAddFiles()[2], radio_rules_loc);
	model.delete();
	proto.delete();
	port.delete();
	String output = obj.exeCmd(cmd);
	BuildTar tarTool = new BuildTar();
    	int nextStateIndex = policyMap.get(srcMac).getStateNum() + 1;
    	String tarpath = policy.getProtections()[nextStateIndex].getImageOpts()[0].getArchives()[0].getTar();	    
	tarTool.file2Tar(tarpath, radio_rules_loc);
	out=true;
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

}
