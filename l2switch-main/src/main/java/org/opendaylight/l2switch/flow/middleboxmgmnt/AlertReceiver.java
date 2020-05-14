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
import org.opendaylight.l2switch.flow.middlebox.AlertHandler;

import org.opendaylight.l2switch.flow.chain.ServiceChain;
import org.opendaylight.l2switch.flow.chain.NewFlows;
import org.opendaylight.l2switch.flow.json.DevPolicy;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.l2switch.flow.json.PolicyParser;
import java.util.Map;
import java.util.HashMap;
import org.opendaylight.l2switch.flow.chain.PolicyStatus;


public class AlertReceiver extends Thread {

    private ServerSocket serverSocket;
    private int port;
    private boolean running = false;
    private String dataplaneIP;
    private String dockerPort;
    private String ovsPort;
    private String OFversion;
    private DevPolicy[] devPolicy;
    private HashMap<String, PolicyStatus> policyMap;
    private String ovsBridge_remotePort;

    public AlertReceiver() {}

    public void setPort(int port) {
        this.port = port;
    }

    public void setDataplaneIP(String dataplaneIP){
	this.dataplaneIP=dataplaneIP;
    }

    public void setDockerPort(String dockerPort){
	this.dockerPort=dockerPort;
    }

    public void setOvsPort(String ovsPort){
	this.ovsPort=ovsPort;
    }

    public void setOFversion(String OFversion){
	this.OFversion=OFversion;
    }

    public void setOvsBridgeRemotePort(String ovsBridge_remotePort){
	this.ovsBridge_remotePort=ovsBridge_remotePort;
    }

    public void setPolicy(DevPolicy[] devPolicy){
	this.devPolicy=devPolicy;
    }

    public void setPolicyMap(HashMap<String, PolicyStatus> policyMap){
	this.policyMap=policyMap;
    }
    

    public void startServer() {
        try {
            serverSocket = new ServerSocket( port );
            this.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stopServer() {
        running = false;
        this.interrupt();
    }

    @Override
    public void run() {
        running = true;
        while(running) {
            try {
                // Call accept() to receive the next connection
                Socket socket = serverSocket.accept();
                // Pass the socket to the RequestHandler thread for processing
                //AlertHandler requestHandler = new AlertHandler(socket);
		AlertHandler requestHandler = new AlertHandler(socket, this.dataplaneIP,
							       this.dockerPort, this.ovsPort,
							       this.OFversion, this.ovsBridge_remotePort,
							       this.devPolicy, this.policyMap);
                requestHandler.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
