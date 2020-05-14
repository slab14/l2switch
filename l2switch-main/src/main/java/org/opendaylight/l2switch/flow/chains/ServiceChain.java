/*
 * Copyright (c) 2018 Slab and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.l2switch.flow.chain;

import org.opendaylight.l2switch.flow.docker.DockerCalls;
//import java.util.regex.Pattern;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.l2switch.flow.containers.Containers;
import org.opendaylight.l2switch.flow.chain.RuleDescriptor;
import org.opendaylight.l2switch.flow.chain.NewFlows;
import org.opendaylight.l2switch.flow.json.DevPolicy;
import org.opendaylight.l2switch.flow.json.ProtectionDetails;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.l2switch.flow.chain.MacGroup;

public class ServiceChain {

    private String remoteIP;
    private String remoteDockerPort;
    private String remoteOvsPort;
    private String OpenFlowVersion;
    private Containers containerCalls;
    private String[] routes;
    private String nodeStr;
    private String ovsBridge_remotePort;
    private DevPolicy devPolicy;
    private ProtectionDetails protectDetails;
    private String curState;
    private NodeConnectorRef inNCR;
    private NodeConnectorRef outNCR;
    private String devNum;

    public ServiceChain(String dataplaneIP, String dockerPort, String ovsPort,
			String OFversion, String[] routes, NodeConnectorRef ncr,
			String ovsBridge_remotePort, DevPolicy devPolicy, String devNum,
			NodeConnectorRef inNCR, NodeConnectorRef outNCR) {
	this.remoteIP = dataplaneIP;
	this.remoteDockerPort=dockerPort;
	this.remoteOvsPort=ovsPort;
	this.OpenFlowVersion=OFversion;
	this.containerCalls=new Containers(dataplaneIP, dockerPort, ovsPort, OFversion);
	this.routes=routes;
	this.nodeStr=this.containerCalls.getNodeString(ncr);
	this.ovsBridge_remotePort=ovsBridge_remotePort;
	this.devPolicy=devPolicy;
	this.protectDetails=devPolicy.getProtections()[0];
	this.curState=devPolicy.getFirstState();
	this.devNum=devNum;
	this.inNCR=inNCR;
	this.outNCR=outNCR;
    }

    public ServiceChain(String dataplaneIP, String dockerPort, String ovsPort,
			String OFversion,  NodeConnectorRef ncr,
			String ovsBridge_remotePort, DevPolicy devPolicy, String devNum,
			NodeConnectorRef inNCR, NodeConnectorRef outNCR) {
	this.remoteIP = dataplaneIP;
	this.remoteDockerPort=dockerPort;
	this.remoteOvsPort=ovsPort;
	this.OpenFlowVersion=OFversion;
	this.containerCalls=new Containers(dataplaneIP, dockerPort, ovsPort, OFversion);
	this.nodeStr=this.containerCalls.getNodeString(ncr);
	this.ovsBridge_remotePort=ovsBridge_remotePort;
	this.devPolicy=devPolicy;
	this.protectDetails=devPolicy.getProtections()[0];
	this.curState=devPolicy.getFirstState();
	this.devNum=devNum;
	this.inNCR=inNCR;
	this.outNCR=outNCR;
    }        

    public NodeConnectorRef[] startPassThroughCont_getNCR(String contName, String contImage, String[] ifaces) {
	this.containerCalls.startContainer(contName, contImage, this.devNum);
	String[] OFports = new String[ifaces.length];
	NodeConnectorRef[] ncrs = new NodeConnectorRef[ifaces.length];	
	for(int i=0; i<ifaces.length; i++){
	    OFports[i]=this.containerCalls.addPortOnContainer_get(contName, ifaces[i], this.ovsBridge_remotePort);
	    ncrs[i]=this.containerCalls.getContainerNodeConnectorRef(this.nodeStr, OFports[i]);
	    this.containerCalls.disableContGRO(contName, ifaces[i]);
	    //for(String route:this.routes) {
	    //	this.containerCalls.addRouteinCont(contName, ifaces[i], route);
	    //}
	}
	this.containerCalls.setDefaultRouteinCont(contName, "eth0");
	return ncrs;
    }

    public void createPassThroughCont(String contName, String contImage) {
	this.containerCalls.createContainer(contName, contImage);
    }    

    public NodeConnectorRef[] startPassThroughCont_getNCR(String contName, String contImage, String[] ifaces, String hostFS, String contFS) {
	this.containerCalls.startContainer_bind(contName, contImage, this.devNum, hostFS, contFS);
	String[] OFports = new String[ifaces.length];
	NodeConnectorRef[] ncrs = new NodeConnectorRef[ifaces.length];	
	for(int i=0; i<ifaces.length; i++){
	    OFports[i]=this.containerCalls.addPortOnContainer_get(contName, ifaces[i], this.ovsBridge_remotePort);
	    ncrs[i]=this.containerCalls.getContainerNodeConnectorRef(this.nodeStr, OFports[i]);
	    this.containerCalls.disableContGRO(contName, ifaces[i]);
	    //for(String route:this.routes) {
	    //	this.containerCalls.addRouteinCont(contName, ifaces[i], route);
	    //}
	}
	this.containerCalls.setDefaultRouteinCont(contName, "eth0");	
	return ncrs;
    }    

    public void createPassThroughCont(String contName, String contImage, String hostFS, String contFS) {
	this.containerCalls.createContainer_bind(contName, contImage, this.devNum, hostFS, contFS);
    }

    public void attachArchiveToPassThroughCont(String contName, String archiveFile, String contPath) {
	this.containerCalls.attachArchive(contName, archiveFile, contPath);
    }

    public NodeConnectorRef[] startCreatedPassThroughCont(String contName, String[] ifaces) {
	this.containerCalls.startCreatedContainer(contName);
	String[] OFports = new String[ifaces.length];
	NodeConnectorRef[] ncrs = new NodeConnectorRef[ifaces.length];	
	for(int i=0; i<ifaces.length; i++){
	    OFports[i]=this.containerCalls.addPortOnContainer_get(contName, ifaces[i], this.ovsBridge_remotePort);
	    ncrs[i]=this.containerCalls.getContainerNodeConnectorRef(this.nodeStr, OFports[i]);
	    this.containerCalls.disableContGRO(contName, ifaces[i]);
	    for(String route:this.routes) {
		this.containerCalls.addRouteinCont(contName, ifaces[i], route);
	    }
	}
	this.containerCalls.setDefaultRouteinCont(contName, "eth0");	
	return ncrs;
    }
    
    public NodeConnectorRef[] startAccessibleCont_getNCR(String contName, String contImage, String[] ifaces, String ip) {
	this.containerCalls.startContainer(contName, contImage, this.devNum);
	String[] OFports = new String[ifaces.length];
	NodeConnectorRef[] ncrs = new NodeConnectorRef[ifaces.length];	
	for(int i=0; i<ifaces.length; i++){
	    OFports[i]=this.containerCalls.addPortOnContainer_get(contName, ifaces[i], ip, this.ovsBridge_remotePort);
	    ncrs[i]=this.containerCalls.getContainerNodeConnectorRef(this.nodeStr, OFports[i]);
	    this.containerCalls.disableContGRO(contName, ifaces[i]);
	    this.containerCalls.updateArp(this.ovsBridge_remotePort, OFports[i]);	    
	    for(String route:this.routes) {
		this.containerCalls.addRouteinCont(contName, ifaces[i], route, ip);
	    }
	}
	this.containerCalls.setDefaultRouteinCont(contName, "eth0");	
	return ncrs;
    }

    public NodeConnectorRef[] startAccessibleCont_getNCR(String contName, String contImage, String[] ifaces, String ip, String hostFS, String contFS) {
	this.containerCalls.startContainer_bind(contName, contImage, this.devNum, hostFS, contFS);
	String[] OFports = new String[ifaces.length];
	NodeConnectorRef[] ncrs = new NodeConnectorRef[ifaces.length];	
	for(int i=0; i<ifaces.length; i++){
	    OFports[i]=this.containerCalls.addPortOnContainer_get(contName, ifaces[i], ip, this.ovsBridge_remotePort);
	    ncrs[i]=this.containerCalls.getContainerNodeConnectorRef(this.nodeStr, OFports[i]);
	    this.containerCalls.disableContGRO(contName, ifaces[i]);
	    this.containerCalls.updateArp(this.ovsBridge_remotePort, OFports[i]);
	    for(String route:this.routes) {
		this.containerCalls.addRouteinCont(contName, ifaces[i], route, ip);
	    }
	}
	this.containerCalls.setDefaultRouteinCont(contName, "eth0");	
	return ncrs;
    }                

    public NodeConnectorRef[] getContNodeConnectorRefs(String contName, String[] ifaces) {
	String[] contOFPorts = new String[ifaces.length];
	NodeConnectorRef[] ncrs = new NodeConnectorRef[ifaces.length];
	for (int i=0; i<ifaces.length; i++) {
	    contOFPorts[i]=this.containerCalls.getContOFPortNum(this.ovsBridge_remotePort, contName, ifaces[i]);
	    ncrs[i]=this.containerCalls.getContainerNodeConnectorRef(this.nodeStr, contOFPorts[i]);
	}
	return ncrs;
    }

    public MacAddress getContMacAddress(String contName, String iface) {
	String MAC = this.containerCalls.getContMAC_fromIface(this.ovsBridge_remotePort, contName, iface);
	MacAddress contMac = this.containerCalls.str2Mac(MAC);
	return contMac;
    }

    public void enableARPs(String contName, String[] ifaces, NodeConnectorRef ncrA, NodeConnectorRef ncrB){
	String portA=this.containerCalls.getPortFromNodeConnectorRef(ncrA);
	String portB=this.containerCalls.getPortFromNodeConnectorRef(ncrB);	
	this.containerCalls.addDirectContainerRouting(this.ovsBridge_remotePort, contName, ifaces[0], portA);
	this.containerCalls.addDirectContainerRouting(this.ovsBridge_remotePort, contName, ifaces[0], portB);		
    }	

    public NewFlows setupChain() {
	//MacAddress inMac=new MacAddress(devPolicy.inMAC);
	String inMac= devPolicy.inMAC;
	//MacAddress outMac=new MacAddress(devPolicy.outMAC);
	String outMac=devPolicy.outMAC;	
	int chainLength = getFirstChainLength();
	String[] chainLinks = getFirstChain();
	ArrayList<RuleDescriptor> newRules=new ArrayList<RuleDescriptor>();
	ArrayList<NodeConnectorRef> nodes=new ArrayList<NodeConnectorRef>();
	ArrayList<MacGroup> groups=new ArrayList<MacGroup>();
	HashMap<Integer, Integer> macMap = new HashMap<>();
	int groupCnt=0;
	MacGroup group0 = new MacGroup(inMac, outMac);
	groups.add(group0);
	nodes.add(inNCR);
	MacAddress contMac;
	NodeConnectorRef[] contNCRs;
	for (int i=0; i<chainLength; i++) {
	    if(chainLinks[i].equals("P")){
		//assumes that all passthrough middleboxes will utilize 2 interfaces
		String[] ifaces={"eth1", "eth2"};
		if(protectDetails.imageOpts[i].archives.length==0) {
		    if(protectDetails.imageOpts[i].hostFS.equals("") || protectDetails.imageOpts[i].contFS.equals("")){
			contNCRs=startPassThroughCont_getNCR(protectDetails.imageOpts[i].contName, protectDetails.images[i], ifaces);
		    } else {
			contNCRs=startPassThroughCont_getNCR(protectDetails.imageOpts[i].contName, protectDetails.images[i], ifaces, protectDetails.imageOpts[i].hostFS, protectDetails.imageOpts[i].contFS);
		    }
		}else{
		    if(protectDetails.imageOpts[i].hostFS.equals("") || protectDetails.imageOpts[i].contFS.equals("")){
			createPassThroughCont(protectDetails.imageOpts[i].contName, protectDetails.images[i]);
		    } else {
			createPassThroughCont(protectDetails.imageOpts[i].contName, protectDetails.images[i], protectDetails.imageOpts[i].hostFS, protectDetails.imageOpts[i].contFS);
		    }
		    for(int j=0; j<protectDetails.imageOpts[i].archives.length; j++) {
			attachArchiveToPassThroughCont(protectDetails.imageOpts[i].contName, protectDetails.imageOpts[i].archives[j].tar, protectDetails.imageOpts[i].archives[j].path);
		    }
		    contNCRs=startCreatedPassThroughCont(protectDetails.imageOpts[i].contName, ifaces);
		}
		for(NodeConnectorRef newNode:contNCRs){
		    nodes.add(newNode);
		}
	    }
	    else if(chainLinks[i].equals("A")){
		groups.remove(groups.size()-1);
		//assumes that all accessible middleboxes will utilize only 1 interface
		String[] ifaces={"eth1"};
		if(protectDetails.imageOpts[i].hostFS.equals("") || protectDetails.imageOpts[i].contFS.equals("")){
		    contNCRs = startAccessibleCont_getNCR(protectDetails.imageOpts[i].contName, protectDetails.images[i], ifaces, protectDetails.imageOpts[i].ip);
		} else {
		    contNCRs = startAccessibleCont_getNCR(protectDetails.imageOpts[i].contName, protectDetails.images[i], ifaces, protectDetails.imageOpts[i].ip, protectDetails.imageOpts[i].hostFS, protectDetails.imageOpts[i].contFS);
		}
		for(NodeConnectorRef newNode:contNCRs){
		    nodes.add(newNode);
		    // Intentionally adding 2x to match number of outputs from passthrough containers
		    nodes.add(newNode);		    
		}
		enableARPs(protectDetails.imageOpts[i].contName, ifaces, inNCR, outNCR);
		contMac = getContMacAddress(protectDetails.imageOpts[i].contName, ifaces[0]);
		MacGroup newGroupA = new MacGroup(inMac, contMac.getValue());
		MacGroup newGroupB = new MacGroup(contMac.getValue(), outMac);
		groups.add(newGroupA);
		groups.add(newGroupB);
		macMap.put(groupCnt, i);
		groupCnt++;
	    }
	}
	macMap.put(groupCnt,chainLength);
	nodes.add(outNCR);
	groupCnt=0;
	String ruleInMac;
	String ruleOutMac;
	for (int i=0; i<chainLength; i++) {
	    ruleInMac = groups.get(groupCnt).inMac;
	    ruleOutMac = groups.get(groupCnt).outMac;
	    RuleDescriptor newRule=new RuleDescriptor(nodes.get(2*i), ruleInMac, nodes.get((2*i)+1), ruleOutMac);
	    newRules.add(newRule);
	    if(macMap.get(groupCnt)<=i){
		groupCnt++;
	    }
	}
	ruleInMac = groups.get(groupCnt).inMac;
	ruleOutMac = groups.get(groupCnt).outMac;
	RuleDescriptor lastRule=new RuleDescriptor(nodes.get(nodes.size()-2), ruleInMac, nodes.get(nodes.size()-1), ruleOutMac);
	newRules.add(lastRule);
	NewFlows updates=new NewFlows(newRules);
	return updates;
    }

    private int getFirstChainLength() {
	int len = protectDetails.chain.split("-").length; // devPolicy.chain.split("-").length;
	return len;
    }

    private String[] getFirstChain() {
	String[] chainElements = protectDetails.chain.split("-"); //	    devPolicy.chain.split("-");

	return chainElements;
    }


    //public NewFlows setupNextChain() {
    public void setupNextChain(){
	/*
	//MacAddress inMac=new MacAddress(devPolicy.inMAC);
	String inMac= devPolicy.inMAC;
	//MacAddress outMac=new MacAddress(devPolicy.outMAC);
	String outMac=devPolicy.outMAC;	
	*/
	// Logic to see if there is another chain?

	//find out about chain
	int chainLength = getNextChainLength();
	String[] chainLinks = getNextChain();
	/*
	ArrayList<RuleDescriptor> newRules=new ArrayList<RuleDescriptor>();
	ArrayList<MacGroup> groups=new ArrayList<MacGroup>();
	HashMap<Integer, Integer> macMap = new HashMap<>();
	int groupCnt=0;
	MacGroup group0 = new MacGroup(inMac, outMac);
	groups.add(group0);
	nodes.add(inNCR);
	MacAddress contMac;
	*/
	ArrayList<NodeConnectorRef> nodes=new ArrayList<NodeConnectorRef>();
	NodeConnectorRef[] contNCRs;
	for (int i=0; i<chainLength; i++) {
	    if(chainLinks[i].equals("P")){
		//assumes that all passthrough middleboxes will utilize 2 interfaces
		String[] ifaces={"eth1", "eth2"};
		if(protectDetails.imageOpts[i].archives.length==0) {
		    if(protectDetails.imageOpts[i].hostFS.equals("") || protectDetails.imageOpts[i].contFS.equals("")){
			contNCRs=startPassThroughCont_getNCR(protectDetails.imageOpts[i].contName, protectDetails.images[i], ifaces);
		    } else {
			contNCRs=startPassThroughCont_getNCR(protectDetails.imageOpts[i].contName, protectDetails.images[i], ifaces, protectDetails.imageOpts[i].hostFS, protectDetails.imageOpts[i].contFS);
		    }
		}else{
		    if(protectDetails.imageOpts[i].hostFS.equals("") || protectDetails.imageOpts[i].contFS.equals("")){
			createPassThroughCont(protectDetails.imageOpts[i].contName, protectDetails.images[i]);
		    } else {
			createPassThroughCont(protectDetails.imageOpts[i].contName, protectDetails.images[i], protectDetails.imageOpts[i].hostFS, protectDetails.imageOpts[i].contFS);
		    }
		    for(int j=0; j<protectDetails.imageOpts[i].archives.length; j++) {
			attachArchiveToPassThroughCont(protectDetails.imageOpts[i].contName, protectDetails.imageOpts[i].archives[j].tar, protectDetails.imageOpts[i].archives[j].path);
		    }
		    contNCRs=startCreatedPassThroughCont(protectDetails.imageOpts[i].contName, ifaces);
		}
		for(NodeConnectorRef newNode:contNCRs){
		    nodes.add(newNode);
		}
	    }
	    /*
	    else if(chainLinks[i].equals("A")){
		groups.remove(groups.size()-1);
		//assumes that all accessible middleboxes will utilize only 1 interface
		String[] ifaces={"eth1"};
		if(protectDetails.imageOpts[i].hostFS.equals("") || protectDetails.imageOpts[i].contFS.equals("")){
		    contNCRs = startAccessibleCont_getNCR(protectDetails.imageOpts[i].contName, protectDetails.images[i], ifaces, protectDetails.imageOpts[i].ip);
		} else {
		    contNCRs = startAccessibleCont_getNCR(protectDetails.imageOpts[i].contName, protectDetails.images[i], ifaces, protectDetails.imageOpts[i].ip, protectDetails.imageOpts[i].hostFS, protectDetails.imageOpts[i].contFS);
		}
		for(NodeConnectorRef newNode:contNCRs){
		    nodes.add(newNode);
		    // Intentionally adding 2x to match number of outputs from passthrough containers
		    nodes.add(newNode);		    
		}
		enableARPs(protectDetails.imageOpts[i].contName, ifaces, inNCR, outNCR);
		contMac = getContMacAddress(protectDetails.imageOpts[i].contName, ifaces[0]);
		MacGroup newGroupA = new MacGroup(inMac, contMac.getValue());
		MacGroup newGroupB = new MacGroup(contMac.getValue(), outMac);
		groups.add(newGroupA);
		groups.add(newGroupB);
		macMap.put(groupCnt, i);
		groupCnt++;
	    }
	    */
	}
	/*
	macMap.put(groupCnt,chainLength);
	nodes.add(outNCR);
	groupCnt=0;
	String ruleInMac;
	String ruleOutMac;
	for (int i=0; i<chainLength; i++) {
	    ruleInMac = groups.get(groupCnt).inMac;
	    ruleOutMac = groups.get(groupCnt).outMac;
	    RuleDescriptor newRule=new RuleDescriptor(nodes.get(2*i), ruleInMac, nodes.get((2*i)+1), ruleOutMac);
	    newRules.add(newRule);
	    if(macMap.get(groupCnt)<=i){
		groupCnt++;
	    }
	}
	ruleInMac = groups.get(groupCnt).inMac;
	ruleOutMac = groups.get(groupCnt).outMac;
	RuleDescriptor lastRule=new RuleDescriptor(nodes.get(nodes.size()-2), ruleInMac, nodes.get(nodes.size()-1), ruleOutMac);
	newRules.add(lastRule);
	NewFlows updates=new NewFlows(newRules);
	return updates;
	*/
    }    
}
