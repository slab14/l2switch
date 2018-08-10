/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.l2switch.flow.json;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.opendaylight.l2switch.flow.json.PolicyFile;

public class PolicyParser {

    public PolicyFile parsed;
    
    public PolicyParser(String jsonPolicy){
	//Gson g = new Gson();
	Gson g = new GsonBuilder().create();
	this.parsed = g.fromJson(jsonPolicy, PolicyFile.class);
    }
    
}
