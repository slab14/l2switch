/*
 * Copyright (c) 2018 Slab and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.l2switch.flow.middlebox;

import org.opendaylight.l2switch.flow.middlebox.TransitionFeatures;

public class MsgAnalysis {
    private String msg;
    private TransitionFeatures feature;

    public MsgAnalysis(String msg, TransitionFeatures feature) {
	this.msg = msg;
	this.feature = feature;
    }


    public MsgAnalysis(String msg, String policyEntry) {
	this.msg = msg;
	TransitionFeatures inputFeatures = new TransitionFeatures(policyEntry);
	this.feature = inputFeatures;
    }

    public String getMsg(){
	return msg;
    }

    public TransitionFeatures getFeatures(){
	return feature;
    }

    public void setMsg(String msg) {
	this.msg = msg;
    }

    public void setFeatures(TransitionFeatures feature) {
	this.feature = feature;
    }

    public void setFeatures(String policyEntry) {
	TransitionFeatures inputFeatures = new TransitionFeatures(policyEntry);
	this.feature = inputFeatures;
    }

    public boolean analyze() {
	boolean trigger = false;
	if (feature.getMbox().equals("snort")) {
	    trigger = processSnortMsg(feature.getKey());
	}
	return trigger;
    }

    private boolean processSnortMsg(String transitionKey) {
	boolean out = false;
	String[] snortAlertParts = msg.split(",");
	String snortMsg = snortAlertParts[4];
	if (snortMsg.toLowerCase().contains(transitionKey.toLowerCase())) {
	    out = true;
	}
	return out;
    }

}
