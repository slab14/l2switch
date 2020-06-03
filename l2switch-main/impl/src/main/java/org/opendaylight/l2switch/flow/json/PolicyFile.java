/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.l2switch.flow.json;

import java.util.HashMap;
import java.util.ArrayList;
import org.opendaylight.l2switch.flow.json.DevPolicy;

public class PolicyFile {
    public int n;
    public DevPolicy[] devices;

    public PolicyFile(int n, DevPolicy[] devices){
	this.n = n;
	this.devices=devices;
    }

    public int getN(){
	return this.n;
    }

    public DevPolicy[] getDevices(){
	return this.devices;
    }

}
