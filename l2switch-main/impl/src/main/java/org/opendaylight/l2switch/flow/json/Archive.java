/*
 * Copyright (c) 2018 slab and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.l2switch.flow.json;

public class Archive {
    public String tar;
    public String path;

    public Archive(String tar, String path) {
	this.tar=tar;
	this.path=path;
    }

    public String getTar() {
	return this.tar;
    }

    public String getPath() {
	return this.path;
    }

}
