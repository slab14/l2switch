/*
 * Copyright © 2017 slab and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.l2switch.flow.containers;

import org.opendaylight.l2switch.flow.docker.DockerCalls;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import java.util.regex.Pattern;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;

public class Containers {

    public Containers() {}

    String remoteIP;
    String remoteDockerPort;
    String remoteOvsPort;
    String OpenFlowVersion;
    
    public Containers(String dataplaneIP, String dockerPort, String ovsPort, String OFversion) {
	this.remoteIP = dataplaneIP;
	this.remoteDockerPort=dockerPort;
	this.remoteOvsPort=ovsPort;
	this.OpenFlowVersion=OFversion;
    }
    
    public static final InstanceIdentifier<Nodes> NODES_IID = InstanceIdentifier.builder(Nodes.class).build();	

    public void startContainer(String dataplaneIP, String dockerPort, String container_name, String containerImage) {
	DockerCalls docker = new DockerCalls();
	docker.remoteStartContainer(dataplaneIP, dockerPort, container_name, containerImage);
    }

    public void startContainer(String container_name, String containerImage) {
	DockerCalls docker = new DockerCalls();
	docker.remoteStartContainer(this.remoteIP, this.remoteDockerPort, container_name, containerImage);
    }

    public void startContainer(String container_name, String containerImage, String containerCmd) {
	DockerCalls docker = new DockerCalls();
	docker.remoteStartContainer(this.remoteIP, this.remoteDockerPort, container_name, containerImage, containerCmd);
    }        

    public void startContainer_bind(String container_name, String containerImage, String hostPath, String contPath) {
	DockerCalls docker = new DockerCalls();
	docker.remoteStartContainer_bind(this.remoteIP, this.remoteDockerPort, container_name, containerImage, hostPath, contPath);
    }
    
    public void startContainer_bind(String container_name, String containerImage, String containerCmd, String hostPath, String contPath) {
	DockerCalls docker = new DockerCalls();
	docker.remoteStartContainer(this.remoteIP, this.remoteDockerPort, container_name, containerImage, containerCmd, hostPath, contPath);
    }        
    
    public String getOVSBridge(String dataplaneIP, String ovsPort){
	DockerCalls docker = new DockerCalls();	
	String ovsBridge = docker.remoteFindBridge(dataplaneIP, ovsPort);
	ovsBridge=ovsBridge.replaceAll("\n","");
	return ovsBridge;
    }

    public String getOVSBridge(){
	DockerCalls docker = new DockerCalls();	
	String ovsBridge = docker.remoteFindBridge(this.remoteIP, this.remoteOvsPort);
	ovsBridge=ovsBridge.replaceAll("\n","");
	return ovsBridge;
    }    

    public void addPortOnContainer(String dataplaneIP, String dockerPort, String ovsPort, String ovsBridge, String container_name, String cont_iface) {
	DockerCalls docker = new DockerCalls();	
	docker.remoteAddContainerPort(ovsBridge, container_name, cont_iface, dataplaneIP, ovsPort, dockerPort);
    }

    public void addPortOnContainer(String ovsBridge, String container_name, String cont_iface) {
	DockerCalls docker = new DockerCalls();	
	docker.remoteAddContainerPort(ovsBridge, container_name, cont_iface, this.remoteIP, this.remoteOvsPort, this.remoteDockerPort);
    }    

    public void addPortOnContainer(String dataplaneIP, String dockerPort, String ovsPort, String ovsBridge, String container_name, String cont_iface, String contPortIP) {
	DockerCalls docker = new DockerCalls();	
	docker.remoteAddContainerPort(ovsBridge, container_name, cont_iface, dataplaneIP, ovsPort, dockerPort, contPortIP);
    }

    public void addPortOnContainer(String ovsBridge, String container_name, String cont_iface, String contPortIP) {
	DockerCalls docker = new DockerCalls();	
	docker.remoteAddContainerPort(ovsBridge, container_name, cont_iface, this.remoteIP, this.remoteOvsPort, this.remoteDockerPort, contPortIP);
    }
    
    public String getContOFPortNum(String dataplaneIP, String ovsPort, String ovsBridge_remotePort, String container_name, String cont_iface, String OFversion) {
	DockerCalls docker = new DockerCalls();		
	String contOFPort=docker.remoteFindContOfPort(dataplaneIP, ovsPort, ovsBridge_remotePort, container_name, cont_iface, OFversion);
	return contOFPort;
    }

    public String getContOFPortNum(String ovsBridge_remotePort, String container_name, String cont_iface) {
	DockerCalls docker = new DockerCalls();		
	String contOFPort=docker.remoteFindContOfPort(this.remoteIP, this.remoteOvsPort, ovsBridge_remotePort, container_name, cont_iface, this.OpenFlowVersion);
	return contOFPort;
    }    

    public String getContMAC_fromPort(String dataplaneIP, String dockerPort, String ovsBridge_remotePort, String container_name, String contOFPortNum, String OFversion) {
	DockerCalls docker = new DockerCalls();			
	String contMAC = docker.remoteFindContainerMACNewIface(container_name, dataplaneIP, dockerPort, ovsBridge_remotePort, contOFPortNum, OFversion);
	return contMAC;
    }

    public String getContMAC_fromPort(String ovsBridge_remotePort, String container_name, String contOFPortNum) {
	DockerCalls docker = new DockerCalls();			
	String contMAC = docker.remoteFindContainerMACNewIface(container_name, this.remoteIP, this.remoteDockerPort, ovsBridge_remotePort, contOFPortNum, this.OpenFlowVersion);
	return contMAC;
    }    

    public String getContMAC_fromIface(String dataplaneIP, String ovsPort,  String dockerPort, String ovsBridge_remotePort, String container_name, String cont_iface, String OFversion) {
	DockerCalls docker = new DockerCalls();		
	String contOFPortNum=docker.remoteFindContOfPort(dataplaneIP, ovsPort, ovsBridge_remotePort, container_name, cont_iface, OFversion);
	String contMAC = docker.remoteFindContainerMACNewIface(container_name, dataplaneIP, dockerPort, ovsBridge_remotePort, contOFPortNum, OFversion);
	return contMAC;
    }

    public String getContMAC_fromIface(String ovsBridge_remotePort, String container_name, String cont_iface) {
	DockerCalls docker = new DockerCalls();		
	String contOFPortNum=docker.remoteFindContOfPort(this.remoteIP, this.remoteOvsPort, ovsBridge_remotePort, container_name, cont_iface, this.OpenFlowVersion);
	String contMAC = docker.remoteFindContainerMACNewIface(container_name, this.remoteIP, this.remoteDockerPort, ovsBridge_remotePort, contOFPortNum, this.OpenFlowVersion);
	return contMAC;
    }        

    public MacAddress str2Mac(String inMAC) {
	MacAddress outMac = new MacAddress(inMAC);
	return outMac;
    }

    public String getNodeString(NodeConnectorRef ncr) {
	Pattern pattern = Pattern.compile(":");
	Uri uri = ncr.getValue().firstKeyOf(NodeConnector.class, NodeConnectorKey.class).getId();
	String[] nodeName = pattern.split(uri.getValue());
	return String.format("%s:%s", nodeName[0], nodeName[1]);
    }

    public String getPortFromNodeConnectorRef(NodeConnectorRef ncr) {
	Pattern pattern = Pattern.compile(":");
	Uri uri = ncr.getValue().firstKeyOf(NodeConnector.class, NodeConnectorKey.class).getId();
	String[] outPort = pattern.split(uri.getValue());
	return outPort[2];
    }

    public NodeConnectorRef getContainerNodeConnectorRef(String node, String port) {
	NodeId contNodeId = new NodeId(node);
	NodeConnectorId contNodeConnId = new NodeConnectorId(String.format("%s:%s", node, port));
	InstanceIdentifier<NodeConnector> contNodeConIId = NODES_IID.child(Node.class, new NodeKey(contNodeId)).child(NodeConnector.class, new NodeConnectorKey(contNodeConnId));
	NodeConnectorRef contNodeConnectorRef = new NodeConnectorRef(contNodeConIId);
	return contNodeConnectorRef;
    }

    public NodeConnectorRef getContainerNodeConnectorRef(String node, String dataplaneIP, String ovsPort, String ovsBridge_remotePort, String container_name, String cont_iface,String OFversion) {
	DockerCalls docker = new DockerCalls();		
	String port=docker.remoteFindContOfPort(dataplaneIP, ovsPort, ovsBridge_remotePort, container_name, cont_iface, OFversion);
	NodeId contNodeId = new NodeId(node);
	NodeConnectorId contNodeConnId = new NodeConnectorId(String.format("%s:%s", node, port));
	InstanceIdentifier<NodeConnector> contNodeConIId = NODES_IID.child(Node.class, new NodeKey(contNodeId)).child(NodeConnector.class, new NodeConnectorKey(contNodeConnId));
	NodeConnectorRef contNodeConnectorRef = new NodeConnectorRef(contNodeConIId);
	return contNodeConnectorRef;
    }

    public NodeConnectorRef getContainerNodeConnectorRef(String node, String ovsBridge_remotePort, String container_name, String cont_iface) {
	DockerCalls docker = new DockerCalls();		
	String port=docker.remoteFindContOfPort(this.remoteIP, this.remoteOvsPort, ovsBridge_remotePort, container_name, cont_iface, this.OpenFlowVersion);
	NodeId contNodeId = new NodeId(node);
	NodeConnectorId contNodeConnId = new NodeConnectorId(String.format("%s:%s", node, port));
	InstanceIdentifier<NodeConnector> contNodeConIId = NODES_IID.child(Node.class, new NodeKey(contNodeId)).child(NodeConnector.class, new NodeConnectorKey(contNodeConnId));
	NodeConnectorRef contNodeConnectorRef = new NodeConnectorRef(contNodeConIId);
	return contNodeConnectorRef;
    }    

    public void updateDefaultRoutes(String dataplaneIP, String ovsBridge_remotePort, String in_port, String newOutPort, String OFversion) {
	DockerCalls docker = new DockerCalls();		
	docker.remoteUpdateDefaultRoute(dataplaneIP, ovsBridge_remotePort, in_port, newOutPort, OFversion);
    }

    public void updateDefaultRoutes(String ovsBridge_remotePort, String in_port, String newOutPort) {
	DockerCalls docker = new DockerCalls();		
	docker.remoteUpdateDefaultRoute(this.remoteIP, ovsBridge_remotePort, in_port, newOutPort, this.OpenFlowVersion);
    }    

    public void addDirectContainer(String dataplaneIP, String dockerPort, String ovsPort, String containerName, String containerImage, String cont_iface, String contIP) {
	startContainer(dataplaneIP, dockerPort, containerName, containerImage);
	String ovsBridge = getOVSBridge(dataplaneIP, ovsPort);
	addPortOnContainer(dataplaneIP, dockerPort, ovsPort, ovsBridge, containerName, cont_iface, contIP);
    }

    public void addDirectContainer(String containerName, String containerImage, String cont_iface, String contIP) {
	startContainer(this.remoteIP, this.remoteDockerPort, containerName, containerImage);
	String ovsBridge = getOVSBridge(this.remoteIP, this.remoteOvsPort);
	addPortOnContainer(this.remoteIP, this.remoteDockerPort, this.remoteOvsPort, ovsBridge, containerName, cont_iface, contIP);
    }    

    public void addDirectContainerRouting(String dataplaneIP, String ovsPort, String ovsBridge_remotePort, String container_name, String cont_iface, String OFversion, String in_port){
	String newOutPort = getContOFPortNum(dataplaneIP, ovsPort, ovsBridge_remotePort, container_name, cont_iface, OFversion);
	updateDefaultRoutes(dataplaneIP, ovsBridge_remotePort, in_port, newOutPort, OFversion);
    }

    public void addDirectContainerRouting(String ovsBridge_remotePort, String container_name, String cont_iface, String in_port){
	String newOutPort = getContOFPortNum(this.remoteIP, this.remoteOvsPort, ovsBridge_remotePort, container_name, cont_iface, this.OpenFlowVersion);
	updateDefaultRoutes(this.remoteIP, ovsBridge_remotePort, in_port, newOutPort, this.OpenFlowVersion);
    }    

}
