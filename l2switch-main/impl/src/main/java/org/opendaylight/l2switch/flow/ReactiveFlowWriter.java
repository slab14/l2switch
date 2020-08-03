/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.l2switch.flow;

import org.opendaylight.l2switch.inventory.InventoryReader;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.arp.rev140528.ArpPacketListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.arp.rev140528.ArpPacketReceived;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.arp.rev140528.arp.packet.received.packet.chain.packet.ArpPacket;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.packet.chain.grp.PacketChain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.packet.chain.grp.packet.chain.packet.RawPacket;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140528.ethernet.packet.received.packet.chain.packet.EthernetPacket;
import org.opendaylight.l2switch.flow.chain.ServiceChain;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.regex.Pattern;
import org.opendaylight.l2switch.flow.json.PolicyParser;
import org.opendaylight.l2switch.flow.chain.NewFlows;
import org.opendaylight.l2switch.flow.chain.RuleDescriptor;
import org.opendaylight.l2switch.flow.chain.MacGroup;
import org.opendaylight.l2switch.flow.chain.PolicyStatus;
import org.opendaylight.l2switch.flow.ovs.VSwitch;
import org.opendaylight.l2switch.flow.ovs.FlowRule;
import org.opendaylight.l2switch.flow.ovs.ActionSet;


/**
 * This class listens to certain type of packets and writes a mac to mac flows.
 */
public class ReactiveFlowWriter implements ArpPacketListener {
    private final InventoryReader inventoryReader;
    private final FlowWriterService flowWriterService;
    private String dataplaneIP;
    private String dockerPort;
    private String ovsPort;
    private String remoteOVSPort;
    private String OFversion;
    private PolicyParser policy;
    private int counter=0;
    private boolean doOnce=true;
    private HashMap<String, PolicyStatus> policyMap;
    private VSwitch vswitch;
    private boolean pkt_signing;
    private boolean prestart;

    public ReactiveFlowWriter(InventoryReader inventoryReader,
			      FlowWriterService flowWriterService) {
	this.inventoryReader = inventoryReader;
        this.flowWriterService = flowWriterService;
    }

    public ReactiveFlowWriter(InventoryReader inventoryReader,
			      FlowWriterService flowWriterService,
			      String dataplaneIP, String dockerPort,
			      String ovsPort, String remoteOVSPort,
			      String OFversion, PolicyParser policy,
			      HashMap<String, PolicyStatus> policyMap,
                  boolean pkt_signing, boolean prestart) {
        this.inventoryReader = inventoryReader;
        this.flowWriterService = flowWriterService;
	this.dataplaneIP=dataplaneIP;
	this.dockerPort=dockerPort;
	this.ovsPort=ovsPort;
	this.remoteOVSPort=remoteOVSPort;
	this.OFversion=OFversion;
	this.policy=policy;
	this.policyMap=policyMap;
	this.vswitch=new VSwitch(dataplaneIP, remoteOVSPort, OFversion);
    this.pkt_signing = pkt_signing;
    this.prestart = prestart;
    }    

    /**
     * Checks if a MAC should be considered for flow creation.
     *
     * @param macToCheck
     *            MacAddress to consider
     * @return true if a MacAddess is broadcast or multicast, false if the
     *         MacAddress is unicast (and thus legible for flow creation).
     */

    private boolean ignoreThisMac(MacAddress macToCheck) {
        if (macToCheck == null) {
            return true;
        }
        String[] octets = macToCheck.getValue().split(":");
        short firstByte = Short.parseShort(octets[0], 16);

        /*
         * First bit in first byte for unicast and multicast is 1 Unicast and
         * multicast are handled by flooding, they are not legible for flow
         * creation
         */

        return (firstByte & 1) == 1;
    }

    private boolean ignoreThisMac(MacAddress macToCheck, PolicyStatus policyStat) {
        if (macToCheck == null) {
            return true;
        }
        String[] octets = macToCheck.getValue().split(":");
        short firstByte = Short.parseShort(octets[0], 16);

        if ((firstByte & 1) == 1) {
	    if (policyStat.destMac.equals("*")) {
		return false;
	    }
	    return true;
	}
	return false;
    }

    
    @Override
    public void onArpPacketReceived(ArpPacketReceived packetReceived) {
        if (packetReceived == null || packetReceived.getPacketChain() == null) {
            return;
        }
        RawPacket rawPacket = null;
        EthernetPacket ethernetPacket = null;
        ArpPacket arpPacket = null;
        for (PacketChain packet : packetReceived.getPacketChain()) {
            if (packet.getPacket() instanceof RawPacket) {
                rawPacket = (RawPacket) packet.getPacket();
            } else if (packet.getPacket() instanceof EthernetPacket) {
                ethernetPacket = (EthernetPacket) packet.getPacket();
            } else if (packet.getPacket() instanceof ArpPacket) {
                arpPacket = (ArpPacket) packet.getPacket();
            }
        }
        if (rawPacket == null || ethernetPacket == null || arpPacket == null) {
            return;
        }
        MacAddress destMac = ethernetPacket.getDestinationMac();
	NodeConnectorRef destNodeConnector=inventoryReader.getNodeConnector(rawPacket.getIngress().getValue().firstIdentifierOf(Node.class), ethernetPacket.getDestinationMac());
	if(destNodeConnector != null){
	    String srcMac = ethernetPacket.getSourceMac().getValue();
        String iot_IP = arpPacket.getSourceProtocolAddress();
	    
	    if (!ignoreThisMac(destMac, policyMap.get(srcMac))) {
		if (policyMap.containsKey(srcMac) && !policyMap.get(srcMac).setup) {
		    System.out.println("Got Mac source from policy file: "+srcMac);
            System.out.println("IoT IP from ARP packet: " + iot_IP);
		    int devNum = policyMap.get(srcMac).devNum;
		    NodeConnectorRef inNCR=rawPacket.getIngress();
		    policyMap.get(srcMac).setNCR(rawPacket.getIngress());
		    policyMap.get(srcMac).setInNCR(inNCR);
		    policyMap.get(srcMac).setOutNCR(destNodeConnector);
            policyMap.get(srcMac).setIOT(iot_IP);
		    String sourceRange=getCDIR(arpPacket.getSourceProtocolAddress(), "32");
		    String destRange=getCDIR(arpPacket.getDestinationProtocolAddress(), "32");
		    String[] routes={sourceRange, destRange};
            System.out.println("Routes: " + routes[0] + ":" + routes[1]);
            System.out.println("rawPacket.ingress:" +  rawPacket.getIngress().getValue());
            System.out.println("inNCR (which is the rawPacket.getIngress):" + inNCR);
            System.out.println("outNCR: " + destNodeConnector);
		    ServiceChain scWorker = new ServiceChain(this.dataplaneIP, this.dockerPort, this.ovsPort, this.OFversion, routes, rawPacket.getIngress(), this.remoteOVSPort, policy.parsed.devices[devNum], String.valueOf(devNum), inNCR, destNodeConnector, iot_IP);
            // routes, rawPacket.getIngress(), inNCR, destNodeConnector cannot be gathered during prestart
		    NewFlows updates = scWorker.setupChain();
		    ActionSet actions = new ActionSet("signkernel", "verifykernel");
		    for(RuleDescriptor rule:updates.rules){
                
			//writeFlows(rule); //Legacy

            //This check if we are signing packets with addHash/checkHash
            if (check_pkt_signing()){
                writeNewActionFlows(rule, actions.getAction1(), actions.getAction2());
            }else{
                writeNewActionFlows(rule);
            }
		    
			actions.switchActionOrder();
		    }
		    policyMap.get(srcMac).updateSetup(true);
		}
	    }
	}
    }


    
    
    /**
     * @return boolean of pkt_signing leaf in YANG config
     */
    
    public boolean check_pkt_signing(){
        return pkt_signing;
    }

   
    /**
     * @param rule rule created by servicechain
     * @param action1 signkernel
     * @param action2 verifykernel
     */
    public void writeNewActionFlows(RuleDescriptor rule, String action1, String action2){
	FlowRule matchAction;
	if(rule.outMac.equals("*")){
	    //TODO : conver NCR to String of OF port #
	    matchAction = new FlowRule("100", rule.inNCR, rule.inMac, "src", rule.outNCR);
	    flowWriterService.addBidirectionalFlowsNewActions(vswitch, matchAction, action1, action2);
	} else {
	    matchAction = new FlowRule("100", rule.inNCR, rule.inMac, "src", rule.outNCR);	    
	    //matchAction = new FlowRule("100", "1", rule.inMac, "src", "2");	    
	    flowWriterService.addBidirectionalFlowsNewActions(vswitch, matchAction, action1, action2);	    
	}
    }   

    /**
     * @param rule rule created by servicechain
     */

    public void writeNewActionFlows(RuleDescriptor rule){ // this allows us to use NewFlows.java for A/P type conts without an actionset TODO: allow actionset for a/x types
    FlowRule matchAction;
    if(rule.outMac.equals("*")){
        //TODO : conver NCR to String of OF port #
        matchAction = new FlowRule("100", rule.inNCR, rule.inMac, "src", rule.outNCR);
        flowWriterService.addBidirectionalFlowsNewActions(vswitch, matchAction);
    } else {
        matchAction = new FlowRule("100", rule.inNCR, rule.inMac, "src", rule.outNCR);      
        //matchAction = new FlowRule("100", "1", rule.inMac, "src", "2");       
        flowWriterService.addBidirectionalFlowsNewActions(vswitch, matchAction);      
    }
    } 

    
    private boolean inMap(HashMap<String, ArrayList<String>> m1, String testKey, String testVal) {
	if (m1.containsKey(testKey)){
	    ArrayList<String> listVals = m1.get(testKey);
	    if(listVals.contains(testVal))
		return true;
	    else
		return false;
	}
	else
	    return false;
    }

    private String getCDIR(String ip, String range) {
	String[] parts = ip.split("\\.");
	String output;
	switch(range) {
	case "8" : output=parts[0]+".0.0.0/"+range;
	    break;
	case "16" : output=parts[0]+"."+parts[1]+".0.0/"+range;
	    break;
	case "24": output=parts[0]+"."+parts[1]+"."+parts[2]+".0/"+range;
	    break;
	case "32" : output=ip+"/"+range;
	    break;
	default : output=ip+"/"+range;
	    break;
	}
	return output;
    }

    private boolean checkMacAddrs(MacAddress pktSrc, String policySrc, MacAddress pktDst, String policyDst){
	if((pktSrc.getValue().equals(policySrc) && pktDst.getValue().equals(policyDst))||(pktSrc.getValue().equals(policyDst) && pktDst.getValue().equals(policySrc))){
	    return true;
	}else{
	    return false;
	}
    }

    private boolean checkMacAddrs_strict(MacAddress pktSrc, String policySrc, MacAddress pktDst, String policyDst){
	if((pktSrc.getValue().equals(policySrc) && pktDst.getValue().equals(policyDst))){
	    return true;
	}else{
	    return false;
	}
    }

    private boolean checkMacAddrs_strict(MacGroup pktMacs, MacGroup policyMacs) {
	if((pktMacs.inMac.equals(policyMacs.inMac)) && (pktMacs.inMac.equals(policyMacs.inMac))){
	    return true;
	}else{
	    return false;
	}
    }    
}
