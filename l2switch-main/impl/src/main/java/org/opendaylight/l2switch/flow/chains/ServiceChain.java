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
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
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
    private String iot_IP;
    private String srcMac;
    private boolean prestart = false; // default false

    public ServiceChain(String dataplaneIP, String dockerPort, String ovsPort,
			String OFversion, String[] routes, NodeConnectorRef ncr,
			String ovsBridge_remotePort, DevPolicy devPolicy, String devNum,
			NodeConnectorRef inNCR, NodeConnectorRef outNCR, String iot_IP) {
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
	this.iot_IP=iot_IP; 
    }

    public ServiceChain(String dataplaneIP, String dockerPort, String ovsPort, 				//prestart
			String OFversion, String ovsBridge_remotePort, DevPolicy devPolicy, 
			String devNum, String iot_IP) {

	this.remoteIP = dataplaneIP;
	this.remoteDockerPort=dockerPort;
	this.remoteOvsPort=ovsPort;
	this.OpenFlowVersion=OFversion;
	this.containerCalls=new Containers(dataplaneIP, dockerPort, ovsPort, OFversion);
	//this.routes=routes;
	//this.nodeStr=this.containerCalls.getNodeString(ncr);
	this.ovsBridge_remotePort=ovsBridge_remotePort;
	this.devPolicy=devPolicy;
	this.protectDetails=devPolicy.getProtections()[0];
	this.curState=devPolicy.getFirstState();
	this.devNum=devNum;
	//this.inNCR=inNCR;
	//this.outNCR=outNCR;
	this.iot_IP=iot_IP; // should be 0.0.0.0
	this.prestart = true;
    }

    public ServiceChain(String dataplaneIP, String dockerPort, String ovsPort,
			String OFversion, String ovsBridge_remotePort,
			DevPolicy devPolicy, String devNum, String state, NodeConnectorRef ncr,
			NodeConnectorRef inNCR, NodeConnectorRef outNCR) {
	this.remoteIP = dataplaneIP;
	this.remoteDockerPort=dockerPort;
	this.remoteOvsPort=ovsPort;
	this.OpenFlowVersion=OFversion;
	this.containerCalls=new Containers(dataplaneIP, dockerPort, ovsPort, OFversion);
	this.ovsBridge_remotePort=ovsBridge_remotePort;
	this.devPolicy=devPolicy;
	int i=0;
	for (i=0; i<devPolicy.states.length; i++){
	    if (state.equals(devPolicy.states[i])) {
		break;
	    }
	}
	this.protectDetails=devPolicy.getProtections()[i];
	this.curState=devPolicy.getStates()[i];
	this.devNum=devNum;
	this.nodeStr=this.containerCalls.getNodeString(ncr);
	this.inNCR=inNCR;
	this.outNCR=outNCR;
    } // original       

    public NodeConnectorRef[] startPassThroughCont_getNCR(String contName, String contImage, String[] ifaces) {
	this.containerCalls.startContainer(contName, contImage, this.devNum);
	String[] OFports = new String[ifaces.length];
	NodeConnectorRef[] ncrs = new NodeConnectorRef[ifaces.length];	
	for(int i=0; i<ifaces.length; i++){
	    OFports[i]=this.containerCalls.addPortOnContainer_get(contName,
								  ifaces[i],
								  this.ovsBridge_remotePort);
	    ncrs[i]=this.containerCalls.getContainerNodeConnectorRef(this.nodeStr, OFports[i]);
	    this.containerCalls.disableContGRO(contName, ifaces[i]);
	    //for(String route:this.routes) {
	    //	this.containerCalls.addRouteinCont(contName, ifaces[i], route);
	    //}
	}
	this.containerCalls.setDefaultRouteinCont(contName, "eth0");
	return ncrs;
    }

    public NodeConnectorRef[] startPassThroughCont_getNCR(String contName, String contImage, String[] ifaces, String iot_IP) {
	this.containerCalls.startContainer(contName, contImage, this.devNum, iot_IP);
	String[] OFports = new String[ifaces.length];
	NodeConnectorRef[] ncrs = new NodeConnectorRef[ifaces.length];	
	for(int i=0; i<ifaces.length; i++){
	    OFports[i]=this.containerCalls.addPortOnContainer_get(contName,
								  ifaces[i],
								  this.ovsBridge_remotePort);
	    ncrs[i]=this.containerCalls.getContainerNodeConnectorRef(this.nodeStr, OFports[i]);
	    this.containerCalls.disableContGRO(contName, ifaces[i]);
	    //for(String route:this.routes) {
	    //	this.containerCalls.addRouteinCont(contName, ifaces[i], route);
	    //}
	}
	this.containerCalls.setDefaultRouteinCont(contName, "eth0");
	return ncrs;
    }

    public void createPassThroughCont(String contName, String contImage, String iot_IP) {
	this.containerCalls.createContainer(contName, contImage, this.devNum, iot_IP);
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

    public void attachArchiveToCont(String contName, String archiveFile, String contPath) {
	this.containerCalls.attachArchive(contName, archiveFile, contPath);
    }

    private void restartContProcess(String contName) {
	this.containerCalls.restartContProcess(contName);
    }

    public NodeConnectorRef[] startCreatedPassThroughCont(String contName, String[] ifaces) {
		this.containerCalls.startCreatedContainer(contName);
		String[] OFports = new String[ifaces.length];
		NodeConnectorRef[] ncrs = new NodeConnectorRef[ifaces.length];	
		for(int i=0; i<ifaces.length; i++){
		    OFports[i]=this.containerCalls.addPortOnContainer_get(contName, ifaces[i], this.ovsBridge_remotePort);
		    if(prestart){
		    	//ncrs[i] = OFports[i];
		    }else{
		    	ncrs[i]=this.containerCalls.getContainerNodeConnectorRef(this.nodeStr, OFports[i]);
		    }
		    this.containerCalls.disableContGRO(contName, ifaces[i]);
		    //for(String route:this.routes) {
		    //	this.containerCalls.addRouteinCont(contName, ifaces[i], route);
		    //}
		}
		this.containerCalls.setDefaultRouteinCont(contName, "eth0");	
		return ncrs;
    }

    
    public NodeConnectorRef[] startAccessibleCont_getNCR(String contName, String contImage, String[] ifaces, String ip, String iot_IP) { 
	this.containerCalls.startContainer(contName, contImage, this.devNum, iot_IP);
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

    public NodeConnectorRef[] pre_start(){
    	NodeConnectorRef[] contNCRs = null;
    	String[] chainLinks = getChain();
    	int chainLength = getChainLength();
    	for (int i=0; i<chainLength; i++) {
    		 if(chainLinks[i].equals("P")){
			//assumes that all passthrough middleboxes will utilize 2 interfaces
				String[] ifaces={"eth1", "eth2"};
				if(protectDetails.imageOpts[i].archives.length==0) {
				    if(protectDetails.imageOpts[i].hostFS.equals("") || protectDetails.imageOpts[i].contFS.equals("")){
						contNCRs=startPassThroughCont_getNCR(protectDetails.imageOpts[i].contName, protectDetails.images[i], ifaces, iot_IP);
				    } else {
						contNCRs=startPassThroughCont_getNCR(protectDetails.imageOpts[i].contName, protectDetails.images[i], ifaces, protectDetails.imageOpts[i].hostFS, protectDetails.imageOpts[i].contFS);
				    }
				}else{
				    if(protectDetails.imageOpts[i].hostFS.equals("") || protectDetails.imageOpts[i].contFS.equals("")){
						createPassThroughCont(protectDetails.imageOpts[i].contName, protectDetails.images[i], iot_IP);
				    } else {
						createPassThroughCont(protectDetails.imageOpts[i].contName, protectDetails.images[i], protectDetails.imageOpts[i].hostFS, protectDetails.imageOpts[i].contFS);
				    }
				    for(int j=0; j<protectDetails.imageOpts[i].archives.length; j++) {
						attachArchiveToCont(protectDetails.imageOpts[i].contName, protectDetails.imageOpts[i].archives[j].tar, protectDetails.imageOpts[i].archives[j].path);
				    }

				    contNCRs=startCreatedPassThroughCont(protectDetails.imageOpts[i].contName, ifaces);
				}
			
		    }else if(chainLinks[i].equals("A") || chainLinks[i].equals("X")){ //addressable proxy
				//assumes that middleboxes will utilize only 1 interface
				String[] ifaces={"eth1"};
				if(protectDetails.imageOpts[i].hostFS.equals("") || protectDetails.imageOpts[i].contFS.equals("")){
				    contNCRs = startAccessibleCont_getNCR(protectDetails.imageOpts[i].contName, protectDetails.images[i], ifaces, protectDetails.imageOpts[i].ip, iot_IP);
				} else { // this doesnt have IOT_IP!
				    contNCRs = startAccessibleCont_getNCR(protectDetails.imageOpts[i].contName, protectDetails.images[i], ifaces, protectDetails.imageOpts[i].ip, protectDetails.imageOpts[i].hostFS, protectDetails.imageOpts[i].contFS);
				}				
		    }
    	}
    	return contNCRs;
    }

    public NewFlows setupChain() {
	//MacAddress inMac=new MacAddress(devPolicy.inMAC);
	String inMac= devPolicy.inMAC;
	//MacAddress outMac=new MacAddress(devPolicy.outMAC);
	String outMac=devPolicy.outMAC;	
	int chainLength = getChainLength();
	String[] chainLinks = getChain();
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
			contNCRs=startPassThroughCont_getNCR(protectDetails.imageOpts[i].contName, protectDetails.images[i], ifaces, iot_IP);
		    } else {
			contNCRs=startPassThroughCont_getNCR(protectDetails.imageOpts[i].contName, protectDetails.images[i], ifaces,
							     protectDetails.imageOpts[i].hostFS, protectDetails.imageOpts[i].contFS);
		    }
		}else{
		    if(protectDetails.imageOpts[i].hostFS.equals("") || protectDetails.imageOpts[i].contFS.equals("")){
			createPassThroughCont(protectDetails.imageOpts[i].contName, protectDetails.images[i], iot_IP);
		    } else {
			createPassThroughCont(protectDetails.imageOpts[i].contName, protectDetails.images[i],
					      protectDetails.imageOpts[i].hostFS, protectDetails.imageOpts[i].contFS);
		    }
		    for(int j=0; j<protectDetails.imageOpts[i].archives.length; j++) {
			attachArchiveToCont(protectDetails.imageOpts[i].contName, protectDetails.imageOpts[i].archives[j].tar,
					    protectDetails.imageOpts[i].archives[j].path);
		    }
		    contNCRs=startCreatedPassThroughCont(protectDetails.imageOpts[i].contName, ifaces);
		}
		for(NodeConnectorRef newNode:contNCRs){
		    nodes.add(newNode);
		}
	    }
	    else if(chainLinks[i].equals("A") || chainLinks[i].equals("X")){ //addressable proxy
		groups.remove(groups.size()-1);
		//assumes that middlebox will utilize only 1 interface
		String[] ifaces={"eth1"};
		if(protectDetails.imageOpts[i].hostFS.equals("") || protectDetails.imageOpts[i].contFS.equals("")){
		    contNCRs = startAccessibleCont_getNCR(protectDetails.imageOpts[i].contName, protectDetails.images[i],
							  ifaces, protectDetails.imageOpts[i].ip, iot_IP);
		} else { // this doesnt have IOT_IP!
		    contNCRs = startAccessibleCont_getNCR(protectDetails.imageOpts[i].contName, protectDetails.images[i],
							  ifaces, protectDetails.imageOpts[i].ip, protectDetails.imageOpts[i].hostFS,
							  protectDetails.imageOpts[i].contFS);
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
	if(chainLength==1 && chainLinks[0].equals("X")){
	    // the cont has only 1 real interface to connect to
	    nodes.add(inNCR);
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
	    /*ruleInMac = groups.get(groupCnt).inMac;
	      ruleOutMac = groups.get(groupCnt).outMac;
	      RuleDescriptor lastRule=new RuleDescriptor(nodes.get(nodes.size()-2), ruleInMac, nodes.get(nodes.size()-1), ruleOutMac);
	      newRules.add(lastRule);*/
	    NewFlows updates=new NewFlows(newRules);
	    return updates;
	}else{
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
	
    }

    private int getChainLength() {
	int len = protectDetails.chain.split("-").length; // devPolicy.chain.split("-").length;
	return len;
    }

    private String[] getChain() {
	String[] chainElements = protectDetails.chain.split("-"); //	    devPolicy.chain.split("-");

	return chainElements;
    }


    public NewFlows setupNextChain() {
	//MacAddress inMac=new MacAddress(devPolicy.inMAC);
	String inMac= devPolicy.inMAC;
	//MacAddress outMac=new MacAddress(devPolicy.outMAC);
	String outMac=devPolicy.outMAC;	
	//find out about chain
	int chainLength = getChainLength();
	String[] chainLinks = getChain();
	ArrayList<RuleDescriptor> newRules=new ArrayList<RuleDescriptor>();
	ArrayList<MacGroup> groups=new ArrayList<MacGroup>();
	HashMap<Integer, Integer> macMap = new HashMap<>();
	ArrayList<NodeConnectorRef> nodes=new ArrayList<NodeConnectorRef>();
	MacAddress contMac;
	NodeConnectorRef[] contNCRs;	
	int groupCnt=0;
	MacGroup group0 = new MacGroup(inMac, outMac);
	groups.add(group0);
	nodes.add(inNCR);
	for (int i=0; i<chainLength; i++) {
	    if(chainLinks[i].equals("P")){
		//assumes that all passthrough middleboxes will utilize 2 interfaces
		String[] ifaces={"eth1", "eth2"};
		if(protectDetails.imageOpts[i].archives.length==0) {
		    if(protectDetails.imageOpts[i].hostFS.equals("") || protectDetails.imageOpts[i].contFS.equals("")){
			contNCRs=startPassThroughCont_getNCR(protectDetails.imageOpts[i].contName, protectDetails.images[i], ifaces, iot_IP);
		    } else {
			contNCRs=startPassThroughCont_getNCR(protectDetails.imageOpts[i].contName, protectDetails.images[i], ifaces, protectDetails.imageOpts[i].hostFS, protectDetails.imageOpts[i].contFS);
		    }
		}else{
		    if(protectDetails.imageOpts[i].hostFS.equals("") || protectDetails.imageOpts[i].contFS.equals("")){
			createPassThroughCont(protectDetails.imageOpts[i].contName, protectDetails.images[i], iot_IP);
		    } else {
			createPassThroughCont(protectDetails.imageOpts[i].contName, protectDetails.images[i], protectDetails.imageOpts[i].hostFS, protectDetails.imageOpts[i].contFS);
		    }
		    for(int j=0; j<protectDetails.imageOpts[i].archives.length; j++) {
			attachArchiveToCont(protectDetails.imageOpts[i].contName, protectDetails.imageOpts[i].archives[j].tar, protectDetails.imageOpts[i].archives[j].path);
		    }
		    contNCRs=startCreatedPassThroughCont(protectDetails.imageOpts[i].contName, ifaces);
		}
		for(NodeConnectorRef newNode:contNCRs){
		    nodes.add(newNode);
		}
	    }
	    else if(chainLinks[i].equals("A") || chainLinks[i].equals("X")){
		groups.remove(groups.size()-1);
		//assumes that all accessible middleboxes will utilize only 1 interface
		String[] ifaces={"eth1"};
		if(protectDetails.imageOpts[i].hostFS.equals("") || protectDetails.imageOpts[i].contFS.equals("")){
		    contNCRs = startAccessibleCont_getNCR(protectDetails.imageOpts[i].contName, protectDetails.images[i], ifaces, protectDetails.imageOpts[i].ip, iot_IP);
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
	if(chainLength==1 && chainLinks[0].equals("X")){
		
		nodes.add(inNCR);
	}else{
		nodes.add(outNCR);
	}
	
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

    public void updateRunningChain() {
	int chainLength = getChainLength();
	String[] chainLinks = getChain();
	for (int i=0; i<chainLength; i++) {
	    if(chainLinks[i].equals("P")){
		if(protectDetails.imageOpts[i].archives.length>0) {
		    for(int j=0; j<protectDetails.imageOpts[i].archives.length; j++) {
			System.out.println("Loading new files");
			attachArchiveToCont(protectDetails.imageOpts[i].contName, protectDetails.imageOpts[i].archives[j].tar, protectDetails.imageOpts[i].archives[j].path);
		    }
		    // execute command to kill process and restart it.
		    restartContProcess(protectDetails.imageOpts[i].contName);
		}
	    }
	}
    }

}
