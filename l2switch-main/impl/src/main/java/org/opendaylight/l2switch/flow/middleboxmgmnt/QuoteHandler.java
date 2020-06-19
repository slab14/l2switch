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

import org.opendaylight.l2switch.flow.json.DevPolicy;
import org.opendaylight.l2switch.flow.json.PolicyParser;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Arrays;
import org.opendaylight.l2switch.flow.chain.PolicyStatus;
import org.opendaylight.l2switch.flow.json.ContOpts;
import org.opendaylight.l2switch.NativeStuff;
import java.io.DataInputStream;

public class QuoteHandler extends Thread {

    private Socket socket;
    private DevPolicy[] devPolicy;
    private HashMap<String, PolicyStatus> policyMap;
    
    QuoteHandler(Socket socket) {
        this.socket = socket;
    }

    QuoteHandler(Socket socket, 
		 DevPolicy[] devPolicy,
		 HashMap<String, PolicyStatus> policyMap) {
        this.socket = socket;
	this.devPolicy=devPolicy;
	this.policyMap=policyMap;
    }

    @Override
    public void run() {
        try {
            // Get input stream
            //BufferedReader in = new BufferedReader( new InputStreamReader( socket.getInputStream() ) );
	    DataInputStream in = new DataInputStream( socket.getInputStream() );
	    StringBuilder sb = new StringBuilder();
	    String policyID="";
	    String sha1="";
	    byte[] msg;
	    byte[] inLen = new byte[4];
	    int encrLen;
	    int bytesRead=0;

	    bytesRead = in.read(inLen,0,4);
	    if (bytesRead==4) {
		//System.out.println(Arrays.toString(inLen));
		encrLen=ByteBuffer.wrap(inLen).getInt();
		encrLen = ((inLen[0] & 0xFF) << 0) | ((inLen[1] & 0xFF) << 8) | ((inLen[2] & 0xFF) << 16 ) | ((inLen[3] & 0xFF) << 24 );
		//System.out.println("msg rx length = "+encrLen);
		msg = new byte[encrLen];
		bytesRead=0;
		bytesRead = in.read(msg);
		if (bytesRead == encrLen) {
		    //Perform actions based upon received message
		    //System.out.println("Got Data: "+ Arrays.toString(msg));
		    NativeStuff cfunc = new NativeStuff();
		    String processedLine = cfunc.decrypt(msg, encrLen);
		    //System.out.println("Converted Data: "+processedLine);
		    if (processedLine.contains("ID:")) {
			policyID=processedLine.substring(processedLine.indexOf("ID:")+4, processedLine.indexOf(";"));
		    }
		    if (processedLine.contains("SHA1:")) {
			sha1=processedLine.substring(processedLine.indexOf("SHA1:")+6);
		    }
	    
		    //System.out.println(policyID);
		    //System.out.println(sha1);
		}
	    }

	    //TODO: currently only works for 1 container. will break if multiple
	    if (!sha1.equals("")){
		String srcMac=findKey(Integer.parseInt(policyID));
		String[] policySha1s=getContImagesSha1(policyID, srcMac);
		boolean found = false
		for(String hash : policySha1s) {
		    if(hash.equals(sha1)){
			found = true;
		    }
		}
		if (found) {
		    System.out.println("Good sha1");
		    this.policyMap.get(srcMac).setVerifiedStatus(true);
		} else {
		    System.out.println("Hash's don't match");
		}

	    }

	    
            in.close();
            //out.close();
            socket.close();
	    
        } catch( Exception e ) {
            e.printStackTrace();
        }
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

    private String[] getContImagesSha1(String policyID, String key) {
	int IDnum=Integer.parseInt(policyID);
	String[] sha1s = this.devPolicy[IDnum].getProtections()[this.policyMap.get(key).getStateNum()].getSha1();
	return sha1s;
    }


}

