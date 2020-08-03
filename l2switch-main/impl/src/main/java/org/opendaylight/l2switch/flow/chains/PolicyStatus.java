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
import org.opendaylight.l2switch.NativeStuff;

public class PolicyStatus {
    //    public MacAddress destMac;
    public String destMac;
    public boolean setup;
    public String[] states;
    public String curState;
    public boolean canTransition;
    public int devNum;
    public String iot_IP;
    //private int stateNum;
    private int maxStates;
    private NodeConnectorRef ncr;
    private NodeConnectorRef inNCR;
    private NodeConnectorRef outNCR;
    private NativeStuff cfunc = new NativeStuff();

    public PolicyStatus(String destMac, String[] states, int devNum) {
	this.destMac=destMac;
	this.states=states;
	this.devNum=devNum;
	this.maxStates=this.states.length -1;
	this.setup=false;
	if (this.maxStates>0){
	    this.canTransition=true;
	}else{
	    this.canTransition=false;
	}
	//this.stateNum=0;
	//curState=states[stateNum];
	curState=states[0];
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
	    cfunc.transitionState(devNum);
	    /*
	    if (this.maxStates > this.stateNum) {	    
		this.stateNum++;
	    }
	    */
	    int stateVal=getStateNum();
	    if (stateVal<this.maxStates) {
		/*
		this.stateNum=this.maxStates;
	    } else {
		*/
		this.canTransition=true;
	    }
	    this.curState=this.states[stateVal];
	    this.setup=false;
	}
    }

    public String getIOT(){
    	return this.iot_IP;
    }

    public void setIOT(String iot_IP){
    	this.iot_IP = iot_IP;
    }

    public int getStateNum(){
	//return this.stateNum;
	return cfunc.getState(devNum);
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
