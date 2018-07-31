/*
 * Copyright © 2017 slab and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.l2switch.flow.demos;

import org.opendaylight.l2switch.flow.containers.Containers;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import java.util.ArrayList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import java.util.regex.Pattern;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;


public class DemoRoutes {
    
    // This is for routing host to host flows through a middlebox
    public void DemoSnort(int counter, String dataplaneIP, String dockerPort, String ovsPort, String containerImage, String hostPath, String contPath){
	String container_name = "demo"+counter;
	String iface1 = "eth1";
	String iface2 = "eth2";	    
	Containers containerCalls = new Containers(dataplaneIP, dockerPort, ovsPort, "13");
	//containerCalls.startContainer_bind(container_name, "snort_ping_alert", "/mnt/slab/snort/log/", "/var/log/snort/");
	containerCalls.startContainer_bind(container_name, containerImage, hostPath, contPath);
	String ovsBridge = containerCalls.getOVSBridge();	    //TODO make this a part of contstructor
	containerCalls.addPortOnContainer(ovsBridge, container_name, iface1);
	containerCalls.addPortOnContainer(ovsBridge, container_name, iface2);
	String ovsBridge_remotePort = "6634";
	String contOFPortNum1 = containerCalls.getContOFPortNum(ovsBridge_remotePort, container_name, iface1);
	String contOFPortNum2 = containerCalls.getContOFPortNum(ovsBridge_remotePort, container_name, iface2);
	String contMAC1 = containerCalls.getContMAC_fromPort(ovsBridge_remotePort, container_name, contOFPortNum1);
	MacAddress contMac1 = containerCalls.str2Mac(contMAC1);
	String contMAC2 = containerCalls.getContMAC_fromPort(ovsBridge_remotePort, container_name, contOFPortNum2);
	MacAddress contMac2 = containerCalls.str2Mac(contMAC2);

	//TODO: move these calls out of here
	/*
	macAddrMap.get(sourceMac.getValue()).add(contMAC1);
	macAddrMap.put(contMAC1, new ArrayList<String>());
	macAddrMap.get(contMAC1).add(sourceMac.getValue());
	macAddrMap.get(destMac.getValue()).add(contMAC2);
	macAddrMap.put(contMAC2, new ArrayList<String>());
	macAddrMap.get(contMAC2).add(destMac.getValue());
	String nodeStr = containerCalls.getNodeString(destNodeConnectorRef);
	NodeConnectorRef contNodeConnectorRef1 = containerCalls.getContainerNodeConnectorRef(nodeStr, contOFPortNum1);
	NodeConnectorRef contNodeConnectorRef2 = containerCalls.getContainerNodeConnectorRef(nodeStr, contOFPortNum2);
	addMacToMacFlow(sourceMac, destMac, contNodeConnectorRef1, sourceNodeConnectorRef);
	addMacToMacFlow(sourceMac, destMac, destNodeConnectorRef, contNodeConnectorRef2);
	addMacToMacFlow(destMac, sourceMac, contNodeConnectorRef2, destNodeConnectorRef);
	addMacToMacFlow(destMac, sourceMac, sourceNodeConnectorRef, contNodeConnectorRef1);
	*/
    }

	
	// This is for host to host routing, with adding a container accessible by each of the hosts
    public void DemoSquid(int counter, String dataplaneIP, String dockerPort, String ovsPort, String containerImage, String hostPath, String contPath) {

	//TODO: figure out what to do with this part
	/*
        // add destMac-To-sourceMac flow on source port
        addMacToMacFlow(destMac, sourceMac, sourceNodeConnectorRef, destNodeConnectorRef);

        // add sourceMac-To-destMac flow on destination port
        addMacToMacFlow(sourceMac, destMac, destNodeConnectorRef, sourceNodeConnectorRef);
	*/
	
	//Add Docker Container -- directly contactable 
	String container_name = "demo"+counter;
	String iface = "eth1";
	Containers containerCalls = new Containers(dataplaneIP, dockerPort, ovsPort, "13");
	//containerCalls.startContainer(container_name, "busybox", "/bin/sh");
	//containerCalls.startContainer_bind(container_name, "squid", "/bin/sh", "/mnt/slab/squid/log/", "/var/log/squid/");
	containerCalls.startContainer_bind(container_name, containerImage, hostPath, contPath);
	String ovsBridge = containerCalls.getOVSBridge();	    
	containerCalls.addPortOnContainer(ovsBridge, container_name, iface, "10.0.6.1/16");	    
	String ovsBridge_remotePort = "6634";
	String contOFPortNum = containerCalls.getContOFPortNum(ovsBridge_remotePort, container_name, iface); 
	String contMAC = containerCalls.getContMAC_fromPort(ovsBridge_remotePort, container_name, contOFPortNum);
	MacAddress contMac = containerCalls.str2Mac(contMAC);

	//TODO: figure out what to do with these calls
	/*
	macAddrMap.get(sourceMac.getValue()).add(contMAC);
	Pattern pattern = Pattern.compile(":");
	Uri destPortUri = destNodeConnectorRef.getValue().firstKeyOf(NodeConnector.class, NodeConnectorKey.class).getId();
	String[] outPort = pattern.split(destPortUri.getValue());
	NodeConnectorRef contNodeConnectorRef = containerCalls.getContainerNodeConnectorRef(String.format("%s:%s", outPort[0], outPort[1]), contOFPortNum);
	addMacToMacFlow(destMac, contMac, contNodeConnectorRef, destNodeConnectorRef);
	addMacToMacFlow(contMac, destMac, destNodeConnectorRef, contNodeConnectorRef);
	containerCalls.addDirectContainerRouting(ovsBridge_remotePort, container_name, iface, outPort[2]);	    
	*/
    }
    
}
