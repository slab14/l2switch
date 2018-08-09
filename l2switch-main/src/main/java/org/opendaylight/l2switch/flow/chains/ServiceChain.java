/*
 * Copyright (c) 2018 Slab and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.l2switch.flow.chain;

import org.opendaylight.l2switch.flow.docker.DockerCalls;
//import java.util.Map;
//import java.util.HashMap;
//import java.util.ArrayList;
//import java.util.regex.Pattern;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.l2switch.flow.containers.Containers;

public class ServiceChain {

    public ServiceChain() {}

    String remoteIP;
    String remoteDockerPort;
    String remoteOvsPort;
    String OpenFlowVersion;
    Containers containerCalls;
    String contName;
    String contImage;
    String[] ifaces;
    String[] routes;
    String nodeStr;
    String ovsBridge_remotePort;

    public ServiceChain(String dataplaneIP, String dockerPort, String ovsPort,
			String OFversion, String contName, String contImage,
			String[] ifaces, String[] routes, NodeConnectorRef ncr,
			String ovsBridge_remotePort) {
	this.remoteIP = dataplaneIP;
	this.remoteDockerPort=dockerPort;
	this.remoteOvsPort=ovsPort;
	this.OpenFlowVersion=OFversion;
	this.containerCalls=new Containers(dataplaneIP, dockerPort, ovsPort, OFversion);
	this.contName=contName;
	this.contImage=contImage;
	this.ifaces=ifaces;
	this.routes=routes;
	this.nodeStr=this.containerCalls.getNodeString(ncr);
	this.ovsBridge_remotePort=ovsBridge_remotePort;
    }

    public void startPassThroughCont() {
	this.containerCalls.startContainer(this.contName, this.contImage);
	for(String iface:this.ifaces){
	    this.containerCalls.addPortOnContainer(this.contName, iface);
	    this.containerCalls.disableContGRO(this.contName, iface);
	    for(String route:this.routes) {
		this.containerCalls.addRouteinCont(this.contName, iface, route);
	    }
	}
    }

    public String[] startPassThroughCont_getOFPort() {
	this.containerCalls.startContainer(this.contName, this.contImage);
	String[] OFports = new String[this.ifaces.length];
	for(int i=0; i<this.ifaces.length; i++){
	    OFports[i]=this.containerCalls.addPortOnContainer_get(this.contName, this.ifaces[i], this.ovsBridge_remotePort);
	    this.containerCalls.disableContGRO(this.contName, this.ifaces[i]);
	    for(String route:this.routes) {
		this.containerCalls.addRouteinCont(this.contName, this.ifaces[i], route);
	    }
	}
	return OFports;
    }    

    public NodeConnectorRef getContNodeConnectorRef(String iface) {
	String contOFPort=this.containerCalls.getContOFPortNum(this.ovsBridge_remotePort, this.contName, iface);
	NodeConnectorRef ncr=this.containerCalls.getContainerNodeConnectorRef(this.nodeStr, contOFPort);
	return ncr;
    }

    public NodeConnectorRef[] getContNodeConnectorRefs() {
	String[] contOFPorts = new String[this.ifaces.length];
	NodeConnectorRef[] ncrs = new NodeConnectorRef[this.ifaces.length];
	for (int i=0; i<this.ifaces.length; i++) {
	    contOFPorts[i]=this.containerCalls.getContOFPortNum(this.ovsBridge_remotePort, this.contName, this.ifaces[i]);
	    ncrs[i]=this.containerCalls.getContainerNodeConnectorRef(this.nodeStr, contOFPorts[i]);
	}
	return ncrs;
    }
    
}
