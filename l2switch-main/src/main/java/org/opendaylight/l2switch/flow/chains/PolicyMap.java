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

public class PolicyMap {
    public HashMap<MacGroup, PolicyStatus> policyMap = new HashMap<MacGroup, PolicyStatus>();
}
