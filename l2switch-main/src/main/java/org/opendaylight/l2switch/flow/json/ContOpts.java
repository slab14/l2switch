/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.l2switch.flow.json;

import org.opendaylight.l2switch.flow.json.Archive;

public class ContOpts {
    public String contName;
    public String ip;
    public String hostFS;
    public String contFS;
    public Archive[] archives;

    public ContOpts(String name, String ip, String hostFS, String contFS, Archive[] archives) {
	this.contName=name;
	this.ip=ip;
	this.hostFS=hostFS;
	this.contFS=contFS;
	this.archives=archives;
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

    public Archive[] getArchives(){
	return this.archives;
    }
}
