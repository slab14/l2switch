/*
 * Copyright (c) 2020 slab and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.l2switch.flow.ovs;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;

public class FlowRule {
    private String priority;
    private String matchPort;
    private String matchMAC;
    private String loc;
    private String actionPort;
    // consider how to handle 2 mac addresses

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

    public FlowRule(String priority, NodeConnectorRef matchPort, String matchMAC, String loc, NodeConnectorRef actionPort){
	String[] findOFport;
	this.priority = priority;
	findOFport = matchPort.getValue().firstKeyOf(NodeConnector.class).getId().getValue().split(":");
	this.matchPort = findOFport[findOFport.length-1];
	this.matchMAC = matchMAC;
	this.loc=loc;
	findOFport = actionPort.getValue().firstKeyOf(NodeConnector.class).getId().getValue().split(":");	
	this.actionPort = findOFport[findOFport.length-1];
    }
    
    public FlowRule(String priority, NodeConnectorRef matchPort, MacAddress matchMAC, String loc, NodeConnectorRef actionPort){
	String[] findOFport;
	this.priority = priority;
	findOFport = matchPort.getValue().firstKeyOf(NodeConnector.class).getId().getValue().split(":");
	this.matchPort = findOFport[findOFport.length-1];
	this.matchMAC = matchMAC.getValue();
	this.loc=loc;
	findOFport = actionPort.getValue().firstKeyOf(NodeConnector.class).getId().getValue().split(":");	
	this.actionPort = findOFport[findOFport.length-1];
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

    public void setMatchPort(NodeConnectorRef newPort){
	String[] findOFport;
        findOFport = newPort.getValue().firstKeyOf(NodeConnector.class).getId().getValue().split(":");
        this.matchPort = findOFport[findOFport.length-1];
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

    public void setActionPort(NodeConnectorRef newPort){
	String[] findOFport;
        findOFport = newPort.getValue().firstKeyOf(NodeConnector.class).getId().getValue().split(":");
        this.actionPort = findOFport[findOFport.length-1];
    }    

    public void switchDir(){
	String temp = matchPort;
        matchPort=actionPort;
	actionPort=temp;
        if (loc.equals("src")){
            loc="dst";
        } else {
            loc="src";
	}
    }

}
