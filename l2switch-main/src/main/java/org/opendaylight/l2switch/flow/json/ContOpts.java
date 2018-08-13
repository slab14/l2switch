/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.l2switch.flow.json;

public class ContOpts {
    public String contName;
    public String ip;
    public String hostFS;
    public String contFS;

    public ContOpts(String name, String ip, String hostFS, String contFS) {
	this.contName=name;
	this.ip=ip;
	this.hostFS=hostFS;
	this.contFS=contFS;
    }

    public String getContName() {
	return this.contName;
    }

    public String getIP() {
	return this.ip;
    }

    public String getHostFS() {
	return this.hostFS;
    }

    public String getContFS() {
	return this.contFS;
    }
}
