/*
 * Copyright (c) 2018 Slab and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.l2switch.flow.middlebox;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;

import org.opendaylight.l2switch.flow.chain.ServiceChain;
import org.opendaylight.l2switch.flow.chain.NewFlows;
import org.opendaylight.l2switch.flow.json.DevPolicy;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.l2switch.flow.json.PolicyParser;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Arrays;
import org.opendaylight.l2switch.flow.chain.PolicyStatus;
import org.opendaylight.l2switch.flow.json.ContOpts;
import org.opendaylight.l2switch.flow.docker.DockerCalls;
import org.opendaylight.l2switch.flow.ReactiveFlowWriter;
import org.opendaylight.l2switch.flow.chain.RuleDescriptor;
import org.opendaylight.l2switch.NativeStuff;
import java.io.DataInputStream;
import org.opendaylight.l2switch.flow.ovs.ActionSet;

public class AlertHandler extends Thread {

    private Socket socket;
    private String dataplaneIP;
    private String dockerPort;
    private String ovsPort;
    private String OFversion;
    private DevPolicy[] devPolicy;
    private HashMap<String, PolicyStatus> policyMap;
    private String ovsBridge_remotePort;
    private ReactiveFlowWriter flowWriter;
    private HashMap<String,Boolean> processing;
    
    AlertHandler(Socket socket) {
        this.socket = socket;
    }

    AlertHandler(Socket socket, String dataplaneIP, String dockerPort,
		 String ovsPort, String OFversion, 
		 String ovsBridge_remotePort, DevPolicy[] devPolicy,
		 HashMap<String, PolicyStatus> policyMap,
		 ReactiveFlowWriter flowWriter,
		 HashMap<String,Boolean> processing) {
        this.socket = socket;
	this.dataplaneIP=dataplaneIP;
	this.dockerPort=dockerPort;
	this.ovsPort=ovsPort;
	this.OFversion=OFversion;
	this.devPolicy=devPolicy;
	this.policyMap=policyMap;
	this.ovsBridge_remotePort=ovsBridge_remotePort;
	this.flowWriter=flowWriter;
	this.processing=processing;
    }

    @Override
    public void run() {
        try {
            // Get input stream
            //BufferedReader in = new BufferedReader( new InputStreamReader( socket.getInputStream() ) );
	    DataInputStream in = new DataInputStream( socket.getInputStream() );
	    StringBuilder sb = new StringBuilder();
	    String policyID="";
	    String alert="";
	    byte[] msg;
	    byte[] inLen = new byte[4];
	    int encrLen;
	    int bytesRead=0;

            // Read lines from client until the client closes the connection
	    bytesRead = in.read(inLen,0,4);
	    if (bytesRead==4) {
		//System.out.println(Arrays.toString(inLen));
		//encrLen=ByteBuffer.wrap(inLen).getInt();
		encrLen = ((inLen[0] & 0xFF) << 0) | ((inLen[1] & 0xFF) << 8) | ((inLen[2] & 0xFF) << 16 ) | ((inLen[3] & 0xFF) << 24 );
		//System.out.println("msg rx length = "+encrLen);
		msg = new byte[encrLen];
		bytesRead=0;
		bytesRead = in.read(msg);
		if (bytesRead == encrLen) {
		    //Perform actions based upon received message
		    //System.out.println("Got Data: "+ Arrays.toString(msg));
		    NativeStuff cfunc = new NativeStuff();
		    //String processedLine = processMsg(line);
		    String processedLine = cfunc.decrypt(msg, encrLen);
		    //System.out.println("Converted Data: "+processedLine);
		    if (processedLine.contains("Policy ID:")) {
			policyID=processedLine.substring(processedLine.indexOf("Policy ID:")+10, processedLine.indexOf(";"));
		    }
		    if (processedLine.contains("Alert:")) {
			alert=processedLine.substring(processedLine.indexOf("Alert:")+6);
		    }
	    
		    //System.out.println(policyID);
		    //System.out.println(alert);
		}
	    }
	    String srcMac=findKey(Integer.parseInt(policyID));
	    System.out.println("Current state: "+this.policyMap.get(srcMac).getStateNum());

            // Close our connection
            in.close();
            //out.close();
            socket.close();


	    if (!this.processing.get(this.socket.getRemoteSocketAddress().toString()).booleanValue()) {
		if (checkForTransitions(policyID) && !alert.equals("")) {
		    this.processing.replace(this.socket.getRemoteSocketAddress().toString(), true);
		    //String srcMac=findKey(Integer.parseInt(policyID));
		    // Alert Msg Analysis
		    MsgAnalysis analyzer = new MsgAnalysis(alert, devPolicy[Integer.parseInt(policyID)].getTransition()[policyMap.get(srcMac).getStateNum()]);
		    if(analyzer.analyze()) {
			//get old container names & images
			String[] oldContNames=getContNames(policyID, srcMac);
			String[] oldContImages=getContImages(policyID, srcMac);
			//transition to next state
			this.policyMap.get(srcMac).transitionState();
			System.out.println("State after transition is: "+this.policyMap.get(srcMac).getCurState());
			ServiceChain scWorker = new ServiceChain(this.dataplaneIP, this.dockerPort,
								 this.ovsPort, this.OFversion,
								 this.ovsBridge_remotePort,
								 this.devPolicy[Integer.parseInt(policyID)],
								 policyID,
								 this.policyMap.get(srcMac).getCurState(),
								 this.policyMap.get(srcMac).getNCR(),
								 this.policyMap.get(srcMac).getInNCR(),
								 this.policyMap.get(srcMac).getOutNCR());
			
			if(isNewImage(oldContImages, policyID, srcMac)) {
			    // perform actions
			    NewFlows updates = scWorker.setupNextChain();
			    //remove old containers (and ovs-ports and OF routes)
			    DockerCalls docker = new DockerCalls();
			    String ovsBridge = docker.remoteFindBridge(this.dataplaneIP,
								   this.ovsPort);
			    for (String name: oldContNames){
				docker.remoteShutdownContainer(this.dataplaneIP, this.dockerPort,
							       name, ovsBridge, this.ovsPort,
							       this.ovsBridge_remotePort,
							       this.OFversion);
			    }
			    //Write routing rules
			    ActionSet actions = new ActionSet("signkernel", "verifykernel");
			    for(RuleDescriptor rule:updates.rules){
				//this.flowWriter.writeFlows(rule);
				this.flowWriter.writeNewActionFlows(rule, actions.getAction1(), actions.getAction2());
				actions.switchActionOrder();
			    }
			} else {
			    // same image, update and restart. keep ports & routing rules.
			    scWorker.updateRunningChain();
			}
			this.policyMap.get(srcMac).updateSetup(true);
		    }
		    this.processing.replace(this.socket.getRemoteSocketAddress().toString(), false);
		}
	    }
        } catch( Exception e ) {
            e.printStackTrace();
        }
    }

    private boolean checkForTransitions(String policyID){
	boolean out=false;
	if (policyID.length()<1){
	    return out;
	}
	int IDnum=Integer.parseInt(policyID);
	// ensure ID is valid
	if (IDnum<this.policyMap.size()) {
	    // get Key for hashmap
	    String key=findKey(IDnum);
	    out=this.policyMap.get(key).getCanTransition();
	}
	return out;
    }

    private String findKey(int devNum){
	Iterator iterator = this.policyMap.entrySet().iterator();
	String out="";
	while (iterator.hasNext()) {
	    Map.Entry mapElement = (Map.Entry)iterator.next();
	    PolicyStatus policyData = (PolicyStatus)mapElement.getValue();
	    if (policyData.devNum==devNum) {
		//found policy data
		out=(String)mapElement.getKey();
		break;
	    }
	}
	return out;
    }

    private String[] getContNames(String policyID, String key){
	int IDnum=Integer.parseInt(policyID);
	ContOpts[] contOpts = this.devPolicy[IDnum].getProtections()[this.policyMap.get(key).getStateNum()].getImageOpts();
	if (IDnum>contOpts.length) {
	    IDnum=contOpts.length;
	}
	String[] out = new String[contOpts.length];
	int i=0;
	for (i=0; i<contOpts.length; i++) {
	    out[i]=contOpts[i].getContName();
	}
	return out;
    }

    private String[] getContImages(String policyID, String key) {
	int IDnum=Integer.parseInt(policyID);
	String[] images = this.devPolicy[IDnum].getProtections()[this.policyMap.get(key).getStateNum()].getImages();
	return images;
    }

    private boolean isNewImage(String[] oldImages, String policyID, String key) {
	boolean out=true;
	int IDnum=Integer.parseInt(policyID);
	int i=0;
	String[] newImages = this.devPolicy[IDnum].getProtections()[this.policyMap.get(key).getStateNum()].getImages();
	if (oldImages.length == newImages.length) {
	    for (i=0; i<newImages.length; i++) {
		if (!oldImages[i].equals(newImages[i]))
		    return out;
	    }
	    out=false;
	}
	return out;
    }

    private String processMsg(String rxMsg) {
	StringBuilder out = new StringBuilder(rxMsg);
	out=out.reverse();
	return out.toString();
    }

    
}

