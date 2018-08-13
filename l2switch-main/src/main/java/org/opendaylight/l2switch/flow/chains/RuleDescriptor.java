/*
 * Copyright (c) 2018 Slab and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.l2switch.flow.chain;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;

public class RuleDescriptor {
    public MacAddress inMac;
    public NodeConnectorRef inNCR;
    public MacAddress outMac;
    public NodeConnectorRef outNCR;

    public RuleDescriptor(NodeConnectorRef inNCR, MacAddress inMac, NodeConnectorRef outNCR, MacAddress outMac) {
	this.inNCR=inNCR;	
	this.inMac=inMac;
	this.outNCR=outNCR;
	this.outMac=outMac;
    }

    public void changeNCRs(NodeConnectorRef newInNCR, NodeConnectorRef newOutNCR) {
	this.inNCR=newInNCR;
	this.outNCR=newOutNCR;
    }

    public void changeMacs(MacAddress newInMac, MacAddress newOutMac) {
	this.inMac=newInMac;
	this.outMac=newOutMac;
    }
    
}
