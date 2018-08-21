/*
 * Copyright (c) 2018 Slab and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.l2switch.flow.chain;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;

public class MacGroup {
    public String inMac;
    public String outMac;

    public MacGroup(String inMac, String outMac){
	this.inMac=inMac;
	this.outMac=outMac;
    }
}
