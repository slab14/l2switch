/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.l2switch.flow.json;

public class ContOpts {
    public String contImage;
    public String[] contOpts;

    public ContOpts(String contImage, String[] opts) {
	this.contImage=contImage;
	this.contOpts=opts;
    }

    public String getContImage() {
	return this.contImage;
    }

    public String[] getContOpts() {
	return this.contOpts;
    }
}
