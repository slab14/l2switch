/*
 * Copyright (c) 2020 slab and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.l2switch.flow.ovs;

public class ActionSet {
    String action1;
    String action2;

    public ActionSet(String action1, String action2) {
	this.action1 = action1;
	this.action2 = action2;
    }

    public void setActions(String action1, String Action2) {
	this.action1 = action1;
	this.action2 = action2;
    }

    public String[] getActions() {
	String[] out = new String[2];
	out[0] = this.action1;
	out[1] = this.action2;
	return out;
    }

    public String getAction1() {
	return action1;
    }

    public String getAction2() {
	return action2;
    }

    public void switchActionOrder() {
	String temp = action1;
	action1=action2;
	action2=temp;
    }

}

