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

public class AlertHandler extends Thread {

    private Socket socket;
    AlertHandler( Socket socket )
    {
        this.socket = socket;
    }

    @Override
    public void run()
    {
        try
        {
            System.out.println( "Received a connection" );

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
            while( line != null && line.length() > 0 )
            {
		//Perform actions based upon received message
		System.out.println("Got Data: "+ line);
		// send message back.
                //out.println( "Echo: " + line );
                //out.flush();
		if (line.contains("Policy ID:")) {
		    policyID=line.substring(10);
		} else {
		    alert=line;
		}
                line = in.readLine();
            }

            // Close our connection
            in.close();
            out.close();
            socket.close();

	    System.out.println("From: ", policyID);
	    System.out.println("alert: ", alert);

        }
        catch( Exception e )
        {
            e.printStackTrace();
        }
    }
}

