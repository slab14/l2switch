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

/**
 * This class listens to certain type of packets and writes a mac to mac flows.
 */
public class ReactiveFlowWriter implements ArpPacketListener {
    private InventoryReader inventoryReader;
    private FlowWriterService flowWriterService;
    private String dataplaneIP;
    private String dockerPort;
    private String ovsPort;
    private String remoteOVSPort;
    private String OFversion;
    private PolicyParser policy;
    private int counter=0;
    private boolean doOnce=true;

    public ReactiveFlowWriter(InventoryReader inventoryReader,
			      FlowWriterService flowWriterService) {
	this.inventoryReader = inventoryReader;
        this.flowWriterService = flowWriterService;
    }

    public ReactiveFlowWriter(InventoryReader inventoryReader,
			      FlowWriterService flowWriterService,
			      String dataplaneIP, String dockerPort,
			      String ovsPort, String remoteOVSPort,
			      String OFversion, PolicyParser policy) {
        this.inventoryReader = inventoryReader;
        this.flowWriterService = flowWriterService;
	this.dataplaneIP=dataplaneIP;
	this.dockerPort=dockerPort;
	this.ovsPort=ovsPort;
	this.remoteOVSPort=remoteOVSPort;
	this.OFversion=OFversion;
	this.policy=policy;
    }    

    /**
     * Checks if a MAC should be considered for flow creation
     *
     * @param macToCheck
     *            MacAddress to consider
     * @return true if a MacAddess is broadcast or multicast, false if the
     *         MacAddress is unicast (and thus legible for flow creation).
     */

    private boolean ignoreThisMac(MacAddress macToCheck) {
        if (macToCheck == null)
            return true;
        String[] octets = macToCheck.getValue().split(":");
        short first_byte = Short.parseShort(octets[0], 16);

        /*
         * First bit in first byte for unicast and multicast is 1 Unicast and
         * multicast are handled by flooding, they are not legible for flow
         * creation
         */

        return ((first_byte & 1) == 1);
    }

    @Override
    public void onArpPacketReceived(ArpPacketReceived packetReceived) {
        if (packetReceived == null || packetReceived.getPacketChain() == null) {
            return;
        }

        RawPacket rawPacket = null;
        EthernetPacket ethernetPacket = null;
        ArpPacket arpPacket = null;
        for (PacketChain packetChain : packetReceived.getPacketChain()) {
            if (packetChain.getPacket() instanceof RawPacket) {
                rawPacket = (RawPacket) packetChain.getPacket();
            } else if (packetChain.getPacket() instanceof EthernetPacket) {
                ethernetPacket = (EthernetPacket) packetChain.getPacket();
            } else if (packetChain.getPacket() instanceof ArpPacket) {
                arpPacket = (ArpPacket) packetChain.getPacket();
            }
        }
        if (rawPacket == null || ethernetPacket == null || arpPacket == null) {
            return;
        }
        MacAddress destMac = ethernetPacket.getDestinationMac();
        if (!ignoreThisMac(destMac)) {
	    /*
	    if(!(inMap(macAddrMap, sourceMac.getValue(), destMac.getValue())) || !(inMap(macAddrMap, destMac.getValue(), sourceMac.getValue()))){
		if (macAddrMap.get(sourceMac.getValue())==null) {
		    macAddrMap.put(sourceMac.getValue(), new ArrayList<String>());
		}
		if (macAddrMap.get(destMac.getValue())==null) {
		    macAddrMap.put(destMac.getValue(), new ArrayList<String>());
		}  
	    */
	    NodeConnectorRef destNodeConnector =inventoryReader.getNodeConnector(rawPacket.getIngress().getValue().firstIdentifierOf(Node.class), ethernetPacket.getDestinationMac());
	    if(destNodeConnector != null){
		if(doOnce){
		    if (checkMacAddrs(ethernetPacket.getSourceMac(), policy.parsed.devices[0].inMAC, ethernetPacket.getDestinationMac(), policy.parsed.devices[0].outMAC)){
			doOnce=false;
			String sourceRange=getCDIR(arpPacket.getSourceProtocolAddress(), "24");
			String destRange=getCDIR(arpPacket.getDestinationProtocolAddress(), "24");
			String contName="demo"+counter;
			String contImage=policy.parsed.devices[0].images[0];
			String[] ifaces={"eth1","eth2"};
			String[] routes={sourceRange, destRange};
			ServiceChain scWorker = new ServiceChain(this.dataplaneIP, this.dockerPort, this.ovsPort, this.OFversion, contName, contImage, ifaces, routes, rawPacket.getIngress(), this.remoteOVSPort);
			scWorker.startPassThroughCont();
			NodeConnectorRef[] ncrs = new NodeConnectorRef[2];
			ncrs=scWorker.getContNodeConnectorRefs();
			writeFlows(rawPacket.getIngress(), ethernetPacket.getSourceMac(), ncrs[0], ethernetPacket.getDestinationMac());
			writeFlows(ncrs[1], ethernetPacket.getSourceMac(), destNodeConnector, ethernetPacket.getDestinationMac());	    
	    //            writeFlows(rawPacket.getIngress(), ethernetPacket.getSourceMac(), ethernetPacket.getDestinationMac());
		    }
		}
	    }
        }
    }

    /**
     * Invokes flow writer service to write bidirectional mac-mac flows on a
     * switch.
     *
     * @param ingress
     *            The NodeConnector where the payload came from.
     * @param srcMac
     *            The source MacAddress of the packet.
     * @param destMac
     *            The destination MacAddress of the packet.
     */
    public void writeFlows(NodeConnectorRef ingress, MacAddress srcMac, MacAddress destMac) {
        NodeConnectorRef destNodeConnector = inventoryReader
                .getNodeConnector(ingress.getValue().firstIdentifierOf(Node.class), destMac);
        if (destNodeConnector != null) {
            flowWriterService.addBidirectionalMacToMacFlows(srcMac, ingress, destMac, destNodeConnector);
        }
    }

    public void writeFlows(NodeConnectorRef ingress, MacAddress srcMac, NodeConnectorRef egress, MacAddress destMac) {
        if (egress != null) {
            flowWriterService.addBidirectionalMacToMacFlows(srcMac, ingress, destMac, egress);
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
}
