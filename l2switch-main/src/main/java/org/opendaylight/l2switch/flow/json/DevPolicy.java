/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.l2switch.flow.json;

import org.opendaylight.l2switch.flow.json.ProtectionDetails;

public class DevPolicy {
    public String name;
    public String inMAC;
    public String outMAC;
    public String[] states;
    public String transition;
    public ProtectionDetails[] protections;

    public DevPolicy(String name, String inMAC, String outMAC, String[] states, String transition, ProtectionDetails[] protections) {
	this.name=name;
	this.inMAC=inMAC;
	this.outMAC=outMAC;
	this.states=states;
	this.transition=transition;
	this.protections=protections;
    }

    public String getName() {
	return this.name;
    }
    
    public String getInMAC(){
	return this.inMAC;
    }

    public String getOutMAC(){
	return this.outMAC;
    }

    public String[] getStates(){
	return this.states;
    }

    public String getTransition(){
	return this.transition;
    }

    public ProtectionDetails[] getProtections() {
	return this.protections;
    }

}
