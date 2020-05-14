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



public class AlertHandler extends Thread {

    private Socket socket;
    private String dataplaneIP;
    private String dockerPort;
    private String ovsPort;
    private String OFversion;
    private DevPolicy[] devPolicy;
    private HashMap<String, PolicyStatus> policyMap;
    private String ovsBridge_remotePort;
    
    AlertHandler(Socket socket) {
        this.socket = socket;
    }

    AlertHandler(Socket socket, String dataplaneIP, String dockerPort,
		 String ovsPort, String OFversion, 
		 String ovsBridge_remotePort, DevPolicy[] devPolicy,
		 HashMap<String, PolicyStatus> policyMap) {
        this.socket = socket;
	this.dataplaneIP=dataplaneIP;
	this.dockerPort=dockerPort;
	this.ovsPort=ovsPort;
	this.OFversion=OFversion;
	this.devPolicy=devPolicy;
	this.policyMap=policyMap;
	this.ovsBridge_remotePort=ovsBridge_remotePort;
    }

    @Override
    public void run() {
        try {
            //System.out.println( "Received a connection" );

            // Get input and output streams
            BufferedReader in = new BufferedReader( new InputStreamReader( socket.getInputStream() ) );
            //PrintWriter out = new PrintWriter( socket.getOutputStream() );

            // Write out our header to the client
            //out.println( "Controller Alert Handler" );
            //out.flush();

	    String policyID="";
	    String alert="";

            // Read lines from client until the client closes the connection or we receive an empty line
            String line = in.readLine();
            while( line != null && line.length() > 0 ) {
		//Perform actions based upon received message
		//System.out.println("Got Data: "+ line);
		// send message back.
                //out.println( "Echo: " + line );
                //out.flush();
		if (line.contains("Policy ID:")) {
		    policyID=line.substring(10);
		}
		if (line.contains("Alert:")) {
		    alert=line.substring(line.indexOf("Alert:")+6);
		}
                line = in.readLine();
            }

            // Close our connection
            in.close();
            //out.close();
            socket.close();

	    if (checkForTransitions(policyID)) {
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
		scWorker.setupNextChain();
		//remove old containers (and ovs-ports)
		DockerCalls docker = new DockerCalls();
		String ovsBridge = docker.remoteFindBridge(dataplaneIP, ovsPort);
		for (String name: oldContNames){
		    docker.remoteShutdownContainer(this.dataplaneIP, this.dockerPort, name,
						   ovsBridge, this.ovsPort);
		}
		//TODO: add routing rules
		this.policyMap.get(srcMac).updateSetup(true);
	    }

        } catch( Exception e ) {
            e.printStackTrace();
        }
    }

    private boolean checkForTransitions(String policyID){
	int IDnum=Integer.parseInt(policyID);
	boolean out=false;
	// ensure ID is valid
	if (IDnum<=this.policyMap.size()) {
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
	String[] out = new String[contOpts.length];
	int i=0;
	for (i=0; i<=contOpts.length; i++) {
	    out[i]=contOpts[i].getContName();
	}
	return out;
    }
	
     
}

