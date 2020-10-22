/*
 * Copyright (c) 2020 Slab and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.l2switch.flow.middlebox;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator; 
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MudParser{
    public ArrayList<String> parse(String path){
	ArrayList<String> portList = new ArrayList<String>();
	try (BufferedReader br = new BufferedReader(new FileReader(path))) {
	    String line;
	    while ((line = br.readLine()) != null) {
		String[] values = line.split(",");
		if(!values[0].equals("<gatewayMac>") && !values[1].equals("<gatewayMac>")){
		    int device = 0;
		    if(!values[device].equals("<deviceMac>"))
			device++;
		    if(!values[device].equals("<deviceMac>"))
			continue;
		    String port = values[device+6];
		    if(!portList.contains(port))
			portList.add(port);
		}
	    }
	}catch(IOException e){
	    System.out.println("IOException: " + e.getMessage());
	}
	return portList;
    }

}
