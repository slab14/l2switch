/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.l2switch.flow.json;

import org.opendaylight.l2switch.flow.json.ContOpts;

public class ProtectionDetails {
    public String state;
    public String chain;
    public String[] images;
    public ContOpts[] imageOpts;

    public ProtectionDetails(String state, String chain, String[] images, ContOpts[] imageOpts) {
	this.state=state;
	this.chain=chain;
	this.images=images;
	this.imageOpts=imageOpts;
    }

    public String getState() {
	return this.state;
    }
    
    public String getChain(){
	return this.chain;
    }

    public String[] getImages(){
	return this.images;
    }

    public ContOpts[] getImageOpts() {
	return this.imageOpts;
    }

}
