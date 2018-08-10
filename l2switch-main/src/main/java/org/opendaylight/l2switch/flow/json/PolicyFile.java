/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.l2switch.flow.json;

public class PolicyFile {
    public String inMAC;
    public String outMAC;
    public String[] images;
    public String [][] imageOpts;

    public PolicyFile(String inMAC, String outMAC, String[] images, String[][] imageOpts){
	this.inMAC=inMAC;
	this.outMAC=outMAC;
	this.images=images;
	this.imageOpts=imageOpts;
    }

    public String getInMAC(){
	return this.inMAC;
    }

    public String getOutMAC(){
	return this.outMAC;
    }

    public String[] getImages(){
	return this.images;
    }

    public String[][] getImageOpts() {
	return this.imageOpts;
    }
    
}
