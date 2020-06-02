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
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

import org.opendaylight.l2switch.flow.chain.ServiceChain;
import org.opendaylight.l2switch.flow.chain.NewFlows;
import org.opendaylight.l2switch.flow.json.DevPolicy;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.l2switch.flow.json.PolicyParser;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import org.opendaylight.l2switch.flow.chain.PolicyStatus;
import org.opendaylight.l2switch.flow.json.ContOpts;
import org.opendaylight.l2switch.flow.docker.DockerCalls;
import org.opendaylight.l2switch.flow.ReactiveFlowWriter;
import org.opendaylight.l2switch.flow.chain.RuleDescriptor;
import org.opendaylight.l2switch.NativeStuff;

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
            //System.out.println( "Received a connection" );

            // Get input and output streams
            BufferedReader in = new BufferedReader( new InputStreamReader( socket.getInputStream() ) );
            //PrintWriter out = new PrintWriter( socket.getOutputStream() );
	    
	    String policyID="";
	    String alert="";

            // Read lines from client until the client closes the connection or we receive an empty line
            String line = in.readLine();
            while( line != null && line.length() > 0 ) {
		//Perform actions based upon received message
		System.out.println("Got Data: "+ line);
		NativeStuff cfunc = new NativeStuff();
		cfunc.helloNative();
		String processedLine = processMsg(line);
		cfunc.revData(line, line.length());
		System.out.println("line after jni: "+line);
		System.out.println("Converted Data: "+processedLine);
		if (processedLine.contains("Policy ID:")) {
		    policyID=processedLine.substring(processedLine.indexOf("Policy ID:")+10, processedLine.indexOf(";"));
		}
		if (processedLine.contains("Alert:")) {
		    alert=processedLine.substring(processedLine.indexOf("Alert:")+6);
		}
                line = in.readLine();
            }

	    System.out.println(policyID);
	    System.out.println(alert);	    

            // Close our connection
            in.close();
            //out.close();
            socket.close();

	    if (!this.processing.get(this.socket.getRemoteSocketAddress().toString()).booleanValue()) {
		if (checkForTransitions(policyID) && !alert.equals("")) {
		    this.processing.replace(this.socket.getRemoteSocketAddress().toString(), true);
		    String srcMac=findKey(Integer.parseInt(policyID));
		    //get old container names
		    String[] oldContNames=getContNames(policyID, srcMac);
		    //transition to next state
		    this.policyMap.get(srcMac).transitionState();
		    // perform actions
		    ServiceChain scWorker = new ServiceChain(this.dataplaneIP, this.dockerPort,
							     this.ovsPort, this.OFversion,
							     this.ovsBridge_remotePort,
							     this.devPolicy[Integer.parseInt(policyID)],
							     policyID,
							     this.policyMap.get(srcMac).getCurState(),
							     this.policyMap.get(srcMac).getNCR(),
							     this.policyMap.get(srcMac).getInNCR(),
							     this.policyMap.get(srcMac).getOutNCR());
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
		    for(RuleDescriptor rule:updates.rules){
			this.flowWriter.writeFlows(rule);
		    }
		    this.policyMap.get(srcMac).updateSetup(true);
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
	

    private String processMsg(String rxMsg) {
	StringBuilder out = new StringBuilder(rxMsg);
	out=out.reverse();
	return out.toString();
    }

    
}

