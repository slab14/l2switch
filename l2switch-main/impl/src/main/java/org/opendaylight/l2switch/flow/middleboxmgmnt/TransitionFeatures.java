/*
 * Copyright (c) 2018 Slab and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.l2switch.flow.middlebox;

public class TransitionFeatures {
    String mbox;
    String key;

    public TransitionFeatures(String mbox, String key) {
	this.mbox = mbox;
	this.key = key;
    }

    public TransitionFeatures(String policyEntry) {
	String[] transitionItems = policyEntry.split(":");
	this.mbox = transitionItems[0];
	this.key = transitionItems[1];
    }

    public String getMbox() {
	return this.mbox;
    }

    public String getKey() {
	return this.key;
    }

    public void setMbox(String mbox) {
	this.mbox = mbox;
    }

    public void setKey(String key) {
	this.key = key;
    }

}
