/*
 * Copyright © 2018 slab and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.l2switch.flow.chain;

import org.opendaylight.l2switch.flow.chain.MacGroup;
import org.opendaylight.l2switch.flow.chain.PolicyStatus;
import java.util.Map;
import java.util.HashMap;
import org.opendaylight.l2switch.flow.json.PolicyParser;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;

public class PolicyMapBuilder {
    public PolicyParser policy;

    public PolicyMapBuilder(PolicyParser policy) {
	this.policy=policy;
    }

    public HashMap<String, PolicyStatus> build(){
	HashMap<String, PolicyStatus> policyMap = new HashMap<String, PolicyStatus>();
	int n = policy.parsed.n;
	for (int i = 0; i < n; i++) {
	    String inMac = policy.parsed.devices[i].inMAC;
	    String outMac = policy.parsed.devices[i].outMAC;	    
	    //MacGroup grpMac = new MacGroup(inMac, outMac);
	    PolicyStatus grpStats = new PolicyStatus(outMac, false, i);
	    policyMap.put(inMac, grpStats);
	}
	return policyMap;
    }

    private MacAddress str2Mac(String inMAC) {
	MacAddress outMac = new MacAddress(inMAC);
	return outMac;
    }

}
