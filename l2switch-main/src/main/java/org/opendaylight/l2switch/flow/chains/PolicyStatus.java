/*
 * Copyright © 2018 slab and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.l2switch.flow.chain;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;

public class PolicyStatus {
    //    public MacAddress destMac;
    public String destMac;
    public boolean setup;
    public String[] states;
    public String curState;
    public boolean canTransition;
    public int devNum;
    private int stateNum;
    private int stateMax;
    private NodeConnectorRef ncr;
    private NodeConnectorRef inNCR;
    private NodeConnectorRef outNCR;    

    public PolicyStatus(String destMac, String[] states, int devNum) {
	this.destMac=destMac;
	this.states=states;
	this.devNum=devNum;
	this.stateMax=this.states.length;		
	this.setup=false;
	this.stateNum=0;
	curState=states[stateNum];
	if (this.stateMax >1){
	    this.canTransition=true;
	}else{
	    this.canTransition=false;
	}
    }

    public void updateSetup(boolean newSetupVal){
	this.setup=newSetupVal;
    }

    public String getCurState(){
	return this.curState;
    }

    public boolean getCanTransition() {
	return this.canTransition;
    }

    public void transitionState() {
	if (this.canTransition) {
	    this.canTransition=false;
	    if (this.stateNum<this.stateMax) {
		this.stateNum++;
	    }
	    this.curState=this.states[stateNum];
	    if (this.stateNum>=this.stateMax) {
		this.stateNum=this.stateMax;
	    } else {
		this.canTransition=true;
	    }
	    this.setup=false;
	}
    }

    public int getStateNum(){
	return this.stateNum;
    }

    public void setNCR(NodeConnectorRef ncr) {
	this.ncr=ncr;
    }

    public NodeConnectorRef getNCR() {
	return this.ncr;
    }    

    public void setInNCR(NodeConnectorRef inNCR) {
	this.inNCR=inNCR;
    }

    public NodeConnectorRef getInNCR() {
	return this.inNCR;
    }        

    public void setOutNCR(NodeConnectorRef outNCR) {
	this.outNCR=outNCR;
    }

    public NodeConnectorRef getOutNCR() {
	return this.outNCR;
    }        

}
