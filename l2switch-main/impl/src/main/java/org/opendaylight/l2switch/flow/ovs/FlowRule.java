/*
 * Copyright (c) 2020 slab and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.l2switch.flow.FlowRule;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;

public class FlowRule {
    private String priority;
    private String matchPort;
    private String matchMAC;
    private String loc;
    private String actionPort;

    public FlowRule(String priority, String matchPort, String matchMAC, String loc, String actionPort){
	this.priority = priority;
	this.matchPort = matchPort;
	this.matchMAC = matchMAC;
	this.loc=loc;
	this.actionPort = actionPort;
    }

    public FlowRule(String priority, String matchPort, MacAddress matchMAC, String loc, String actionPort){
	this.priority = priority;
	this.matchPort = matchPort;
	this.matchMAC = matchMAC.getValue();
	this.loc=loc;
	this.actionPort = actionPort;
    }

    public String getPriority(){
	return priority;
    }

    public String getMatchPort(){
	return matchPort;
    }

    public String getMatchMAC(){
	return matchMAC;
    }

    public String getLoc(){
	return loc;
    }

    public String getActionPort(){
	return actionPort;
    }

    public void setPriority(String priority){
	this.priority=priority;
    }

    public void setPriority(int priority){
	this.priority=String.valueOf(priority);
    }

    public void setMatchPort(String port){
	matchPort=port;
    }

    public void setMatchMAC(String newMAC){
	matchMAC = newMAC;
    }

    public void setMatchMAC(MacAddress newMAC){
	matchMAC = newMAC.getValue();
    }

    public void setLoc(String loc){
	this.loc=loc;
    }

    public void setActionPort(String port){
	actionPort=port;
    }

}
