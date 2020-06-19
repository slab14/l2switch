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
import org.opendaylight.l2switch.flow.middlebox.QuoteHandler;

import org.opendaylight.l2switch.flow.json.DevPolicy;
import org.opendaylight.l2switch.flow.json.PolicyParser;
import java.util.Map;
import java.util.HashMap;
import org.opendaylight.l2switch.flow.chain.PolicyStatus;


public class QuoteReceiver extends Thread {

    private ServerSocket serverSocket;
    private int port;
    private boolean running = false;
    private DevPolicy[] devPolicy;
    private HashMap<String, PolicyStatus> policyMap;

    public QuoteReceiver() {}

    public void setPort(int port) {
        this.port = port;
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
		QuoteHandler quoteHandler = new QuoteHandler(socket, this.devPolicy, this.policyMap);
		quoteHandler.start();
		
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
