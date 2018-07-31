/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.l2switch.flow;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import java.math.BigInteger;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import org.opendaylight.l2switch.util.InstanceIdentifierUtils;
import org.opendaylight.openflowplugin.api.OFConstants;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.OutputActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.output.action._case.OutputActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.TableKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.AddFlowInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.AddFlowOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.FlowTableRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.SalFlowService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.FlowCookie;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.FlowModFlags;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.FlowRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.InstructionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.ApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.apply.actions._case.ApplyActions;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.apply.actions._case.ApplyActionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.InstructionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.ethernet.match.fields.EthernetDestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.ethernet.match.fields.EthernetSourceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.EthernetMatch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.EthernetMatchBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.opendaylight.l2switch.flow.docker.DockerCalls;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.regex.Pattern;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.l2switch.flow.containers.Containers;

/**
 * Implementation of
 * FlowWriterService{@link org.opendaylight.l2switch.flow.FlowWriterService},
 * that builds required flow and writes to configuration data store using
 * provided DataBrokerService
 * {@link org.opendaylight.controller.sal.binding.api.data.DataBrokerService}
 */
public class FlowWriterServiceImpl implements FlowWriterService {
    private static final Logger LOG = LoggerFactory.getLogger(FlowWriterServiceImpl.class);
    private final String FLOW_ID_PREFIX = "L2switch-";
    private SalFlowService salFlowService;
    private short flowTableId;
    private int flowPriority;
    private int flowIdleTimeout;
    private int flowHardTimeout;

    private AtomicLong flowIdInc = new AtomicLong();
    private AtomicLong flowCookieInc = new AtomicLong(0x2a00000000000000L);
    private final Integer DEFAULT_TABLE_ID = 0;
    private final Integer DEFAULT_PRIORITY = 10;
    private final Integer DEFAULT_HARD_TIMEOUT = 0;
    private final Integer DEFAULT_IDLE_TIMEOUT = 0;

    private String dataplaneIP = "127.0.0.1";
    private String dockerPort = "4243";
    private String ovsPort = "6677";
    private int containerCounter = 0;
    private HashMap<String, ArrayList<String>> macAddrMap = new HashMap<String, ArrayList<String>>();
    public static final InstanceIdentifier<Nodes> NODES_IID = InstanceIdentifier.builder(Nodes.class).build();
    
    public FlowWriterServiceImpl(SalFlowService salFlowService) {
        Preconditions.checkNotNull(salFlowService, "salFlowService should not be null.");
        this.salFlowService = salFlowService;
    }

    public void setFlowTableId(short flowTableId) {
        this.flowTableId = flowTableId;
    }

    public void setFlowPriority(int flowPriority) {
        this.flowPriority = flowPriority;
    }

    public void setFlowIdleTimeout(int flowIdleTimeout) {
        this.flowIdleTimeout = flowIdleTimeout;
    }

    public void setFlowHardTimeout(int flowHardTimeout) {
        this.flowHardTimeout = flowHardTimeout;
    }

    public void setDataplaneIPAddr(String ip) {
	this.dataplaneIP = ip;
    }

    public void setDockerPort(String port) {
	this.dockerPort = port;
    }

    /**
     * Writes a flow that forwards packets to destPort if destination mac in
     * packet is destMac and source Mac in packet is sourceMac. If sourceMac is
     * null then flow would not set any source mac, resulting in all packets
     * with destMac being forwarded to destPort.
     *
     * @param sourceMac
     * @param destMac
     * @param destNodeConnectorRef
     */
    @Override
    public void addMacToMacFlow(MacAddress sourceMac, MacAddress destMac,
				NodeConnectorRef destNodeConnectorRef,
				NodeConnectorRef sourceNodeConnectorRef) {

        Preconditions.checkNotNull(destMac, "Destination mac address should not be null.");
        Preconditions.checkNotNull(destNodeConnectorRef, "Destination port should not be null.");

        // do not add flow if both macs are same.
        if (sourceMac != null && destMac.equals(sourceMac)) {
            LOG.info("In addMacToMacFlow: No flows added. Source and Destination mac are same.");
            return;
        }

        // get flow table key
        TableKey flowTableKey = new TableKey((short) flowTableId);

        // build a flow path based on node connector to program flow
        InstanceIdentifier<Flow> flowPath = buildFlowPath(destNodeConnectorRef,
							  flowTableKey);

        // build a flow that target given mac id
        Flow flowBody = createMacToMacFlow(flowTableKey.getId(),
					   flowPriority, sourceMac, destMac,
					   destNodeConnectorRef, sourceNodeConnectorRef);

        // commit the flow in config data
        writeFlowToConfigData(flowPath, flowBody);
    }

    /**
     * Writes mac-to-mac flow on all ports that are in the path between given
     * source and destination ports. It uses path provided by
     * org.opendaylight.l2switch.loopremover.topology.NetworkGraphService to
     * find a links
     * {@link org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link}
     * between given ports. And then writes appropriate flow on each port that
     * is covered in that path.
     *
     * @param sourceMac
     * @param sourceNodeConnectorRef
     * @param destMac
     * @param destNodeConnectorRef
     */
    @Override
    public void addBidirectionalMacToMacFlows(MacAddress sourceMac, NodeConnectorRef sourceNodeConnectorRef,
            MacAddress destMac, NodeConnectorRef destNodeConnectorRef) {
        Preconditions.checkNotNull(sourceMac, "Source mac address should not be null.");
        Preconditions.checkNotNull(sourceNodeConnectorRef, "Source port should not be null.");
        Preconditions.checkNotNull(destMac, "Destination mac address should not be null.");
        Preconditions.checkNotNull(destNodeConnectorRef, "Destination port should not be null.");

        if (sourceNodeConnectorRef.equals(destNodeConnectorRef)) {
            LOG.info("In addMacToMacFlowsUsingShortestPath: No flows added. Source and Destination ports are same.");
            return;
        }

	// Proxy Password Demo -- non-elegant version
	if(!(inMap(macAddrMap, sourceMac.getValue(), destMac.getValue())) || !(inMap(macAddrMap, destMac.getValue(), sourceMac.getValue()))){
	    if (macAddrMap.get(sourceMac.getValue())==null) {
		macAddrMap.put(sourceMac.getValue(), new ArrayList<String>());
	    }
	    if (macAddrMap.get(destMac.getValue())==null) {
		macAddrMap.put(destMac.getValue(), new ArrayList<String>());
	    }
	    macAddrMap.get(sourceMac.getValue()).add(destMac.getValue());
	    macAddrMap.get(destMac.getValue()).add(sourceMac.getValue());	    
	    String container_name = "demo"+containerCounter;
	    String iface1 = "eth1";
	    String iface2 = "eth2";	    
	    ++containerCounter;
	    Containers containerCalls = new Containers(dataplaneIP, dockerPort, ovsPort, "13");
	    // IPS: Snort Container
	    String IPS_Name=container_name+"_snort";
	    containerCalls.startContainer_bind(IPS_Name, "snort_ping_alert", "/mnt/slab/snort/log/", "/var/log/snort/");
	    // Proxy: Squid Container
	    String Proxy_Name=container_name+"_squid";
	    //TODO: find proxy container to use configured with authentication
	    containerCalls.startContainer_bind(Proxy_Name, "squid", "/mnt/slab/squid/log/", "/var/log/squid/"); 
	    String ovsBridge = containerCalls.getOVSBridge();
	    // Add 2 ports to Snort container
	    containerCalls.addPortOnContainer(ovsBridge, IPS_Name, iface1);
	    containerCalls.addPortOnContainer(ovsBridge, IPS_Name, iface2);
	    // Add 1 port to Squid container
	    String ProxyIP="10.1.2.1/16";
	    containerCalls.addPortOnContainer(ovsBridge, Proxy_Name, iface1, ProxyIP);
	    String ovsBridge_remotePort = "6634";
	    // Get OpenFlow Port #s
	    String IPScontOFPortNum1 = containerCalls.getContOFPortNum(ovsBridge_remotePort, IPS_Name, iface1);
	    String IPScontOFPortNum2 = containerCalls.getContOFPortNum(ovsBridge_remotePort, IPS_Name, iface2);
	    String ProxycontOFPortNum = containerCalls.getContOFPortNum(ovsBridge_remotePort, Proxy_Name, iface1);
	    // Get MAC Addresses
	    String IPSMAC1 = containerCalls.getContMAC_fromPort(ovsBridge_remotePort, IPS_Name, IPScontOFPortNum1);
	    MacAddress IPSMac1 = containerCalls.str2Mac(IPSMAC1);
	    String IPSMAC2 = containerCalls.getContMAC_fromPort(ovsBridge_remotePort, IPS_Name, IPScontOFPortNum2);
	    MacAddress IPSMac2 = containerCalls.str2Mac(IPSMAC2);
	    String ProxyMAC = containerCalls.getContMAC_fromPort(ovsBridge_remotePort, Proxy_Name, ProxycontOFPortNum);
	    MacAddress ProxyMac = containerCalls.str2Mac(ProxyMAC);
	    macAddrMap.get(sourceMac.getValue()).add(IPSMAC1);
	    macAddrMap.put(IPSMAC1, new ArrayList<String>());
	    macAddrMap.get(IPSMAC1).add(sourceMac.getValue());
	    macAddrMap.get(destMac.getValue()).add(IPSMAC2);
	    macAddrMap.put(IPSMAC2, new ArrayList<String>());
	    macAddrMap.get(IPSMAC2).add(destMac.getValue());
	    macAddrMap.get(IPSMAC2).add(ProxyMAC);
	    macAddrMap.put(ProxyMAC, new ArrayList<String>());
	    macAddrMap.get(ProxyMAC).add(IPSMAC2);
	    macAddrMap.get(ProxyMAC).add(destMac.getValue());
	    macAddrMap.get(destMac.getValue()).add(ProxyMAC);
	    String nodeStr = containerCalls.getNodeString(destNodeConnectorRef);
	    NodeConnectorRef IPScontNodeConnectorRef1 = containerCalls.getContainerNodeConnectorRef(nodeStr, IPScontOFPortNum1);
	    NodeConnectorRef IPScontNodeConnectorRef2 = containerCalls.getContainerNodeConnectorRef(nodeStr, IPScontOFPortNum2);
	    NodeConnectorRef ProxycontNodeConnectorRef = containerCalls.getContainerNodeConnectorRef(nodeStr, ProxycontOFPortNum);
	    // Add Routing (SRC <-IPS-> Proxy; Proxy <-> DST)
	    addMacToMacFlow(sourceMac, ProxyMac, IPScontNodeConnectorRef1, sourceNodeConnectorRef);
	    addMacToMacFlow(sourceMac, ProxyMac, ProxycontNodeConnectorRef, IPScontNodeConnectorRef2);
	    addMacToMacFlow(ProxyMac, destMac, destNodeConnectorRef, ProxycontNodeConnectorRef);
	    addMacToMacFlow(destMac, ProxyMac, ProxycontNodeConnectorRef, destNodeConnectorRef);
	    addMacToMacFlow(ProxyMac, sourceMac, IPScontNodeConnectorRef2, ProxycontNodeConnectorRef);
	    addMacToMacFlow(ProxyMac, sourceMac, sourceNodeConnectorRef, IPScontNodeConnectorRef1);
	    // Add direct route (SRC <-IPS-> DST)
	    addMacToMacFlow(sourceMac, destMac, IPScontNodeConnectorRef1, sourceNodeConnectorRef);
	    addMacToMacFlow(sourceMac, destMac, destNodeConnectorRef, IPScontNodeConnectorRef2);
	    addMacToMacFlow(destMac, sourceMac, IPScontNodeConnectorRef2, destNodeConnectorRef);
	    addMacToMacFlow(destMac, sourceMac, sourceNodeConnectorRef, IPScontNodeConnectorRef1);
	    // Add routing for arps and others to include Proxy container
	    String srcPort = containerCalls.getPortFromNodeConnectorRef(sourceNodeConnectorRef);
	    String dstPort = containerCalls.getPortFromNodeConnectorRef(destNodeConnectorRef);	    
	    containerCalls.addDirectContainerRouting(ovsBridge_remotePort, Proxy_Name, iface1, srcPort);
	    containerCalls.addDirectContainerRouting(ovsBridge_remotePort, Proxy_Name, iface1, dstPort);
	}
	
	// This is for routing host to host flows through a middlebox
	/*
	if(!(inMap(macAddrMap, sourceMac.getValue(), destMac.getValue())) || !(inMap(macAddrMap, destMac.getValue(), sourceMac.getValue()))){
	    if (macAddrMap.get(sourceMac.getValue())==null) {
		macAddrMap.put(sourceMac.getValue(), new ArrayList<String>());
	    }
	    if (macAddrMap.get(destMac.getValue())==null) {
		macAddrMap.put(destMac.getValue(), new ArrayList<String>());
	    }
	    macAddrMap.get(sourceMac.getValue()).add(destMac.getValue());
	    macAddrMap.get(destMac.getValue()).add(sourceMac.getValue());	    
	    String container_name = "demo"+containerCounter;
	    String iface1 = "eth1";
	    String iface2 = "eth2";	    
	    ++containerCounter;
	    Containers containerCalls = new Containers(dataplaneIP, dockerPort, ovsPort, "13");
	    //containerCalls.startContainer(container_name, "snort_ping_alert");
	    containerCalls.startContainer_bind(container_name, "snort_ping_alert", "/mnt/slab/snort/log/", "/var/log/snort/");  
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
	    //String srcPort = getPortFromNodeConnectorRef(sourceNodeConnectorRef);
	    //String dstPort = getPortFromNodeConnectorRef(destNodeConnectorRef);	    
	    //containerCalls.addDirectContainerRouting(ovsBridge_remotePort, container_name, iface1, srcPort);
	    //containerCalls.addDirectContainerRouting(ovsBridge_remotePort, container_name, iface2, dstPort);
	}
	*/
	
	// This is for host to host routing, with adding a container accessible by each of the hosts
	/*
        // add destMac-To-sourceMac flow on source port
        addMacToMacFlow(destMac, sourceMac, sourceNodeConnectorRef, destNodeConnectorRef);

        // add sourceMac-To-destMac flow on destination port
        addMacToMacFlow(sourceMac, destMac, destNodeConnectorRef, sourceNodeConnectorRef);

	//Add Docker Container -- directly contactable 
	if(!(inMap(macAddrMap, sourceMac.getValue(), destMac.getValue()))){
	    if (macAddrMap.get(sourceMac.getValue())==null) {
		macAddrMap.put(sourceMac.getValue(), new ArrayList<String>());
	    }
	    if (macAddrMap.get(destMac.getValue())==null) {
		macAddrMap.put(destMac.getValue(), new ArrayList<String>());
	    }
	    macAddrMap.get(sourceMac.getValue()).add(destMac.getValue());	    
	    String container_name = "demo"+containerCounter;
	    String iface = "eth1";
	    ++containerCounter;
	    Containers containerCalls = new Containers(dataplaneIP, dockerPort, ovsPort, "13");
	    //containerCalls.startContainer(container_name, "busybox", "/bin/sh");
	    containerCalls.startContainer_bind(container_name, "squid", "/bin/sh", "/mnt/slab/squid/log/", "/var/log/squid/");
	    String ovsBridge = containerCalls.getOVSBridge();	    
	    containerCalls.addPortOnContainer(ovsBridge, container_name, iface, "10.0.6.1/16");	    
	    String ovsBridge_remotePort = "6634";
	    String contOFPortNum = containerCalls.getContOFPortNum(ovsBridge_remotePort, container_name, iface); 
	    String contMAC = containerCalls.getContMAC_fromPort(ovsBridge_remotePort, container_name, contOFPortNum);
	    MacAddress contMac = containerCalls.str2Mac(contMAC);
	    macAddrMap.get(sourceMac.getValue()).add(contMAC);
	    Pattern pattern = Pattern.compile(":");
	    Uri destPortUri = destNodeConnectorRef.getValue().firstKeyOf(NodeConnector.class, NodeConnectorKey.class).getId();
	    String[] outPort = pattern.split(destPortUri.getValue());
	    NodeConnectorRef contNodeConnectorRef = containerCalls.getContainerNodeConnectorRef(String.format("%s:%s", outPort[0], outPort[1]), contOFPortNum);
	    addMacToMacFlow(destMac, contMac, contNodeConnectorRef, destNodeConnectorRef);
	    addMacToMacFlow(contMac, destMac, destNodeConnectorRef, contNodeConnectorRef);
	    containerCalls.addDirectContainerRouting(ovsBridge_remotePort, container_name, iface, outPort[2]);	    
	}
	*/
	
    }

    /**
     * @param nodeConnectorRef
     * @return
     */
    private InstanceIdentifier<Flow> buildFlowPath(NodeConnectorRef nodeConnectorRef, TableKey flowTableKey) {

        // generate unique flow key
        FlowId flowId = new FlowId(FLOW_ID_PREFIX+String.valueOf(flowIdInc.getAndIncrement()));
        FlowKey flowKey = new FlowKey(flowId);

        return InstanceIdentifierUtils.generateFlowInstanceIdentifier(nodeConnectorRef, flowTableKey, flowKey);
    }

    /**
     * @param tableId
     * @param priority
     * @param sourceMac
     * @param destMac
     * @param destPort
     * @return {@link org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder}
     *         builds flow that forwards all packets with destMac to given port
     */
    private Flow createMacToMacFlow(Short tableId, int priority, MacAddress sourceMac, MacAddress destMac,
				    NodeConnectorRef destPort, NodeConnectorRef sourcePort) {

        // start building flow
        FlowBuilder macToMacFlow = new FlowBuilder() //
                .setTableId(tableId) //
                .setFlowName("mac2mac");

        // use its own hash code for id.
        macToMacFlow.setId(new FlowId(Long.toString(macToMacFlow.hashCode())));

        // create a match that has mac to mac ethernet match
        EthernetMatchBuilder ethernetMatchBuilder = new EthernetMatchBuilder() //
                .setEthernetDestination(new EthernetDestinationBuilder() //
                        .setAddress(destMac) //
                        .build());
        // set source in the match only if present
        if (sourceMac != null) {
            ethernetMatchBuilder.setEthernetSource(new EthernetSourceBuilder().setAddress(sourceMac).build());
        }
        EthernetMatch ethernetMatch = ethernetMatchBuilder.build();
	
        //Match match = new MatchBuilder().setEthernetMatch(ethernetMatch).build();
	MatchBuilder matchBuilder = new MatchBuilder().setEthernetMatch(ethernetMatch);
	NodeConnectorId srcPort = sourcePort.getValue().firstKeyOf(NodeConnector.class, NodeConnectorKey.class).getId();
	matchBuilder.setInPort(srcPort);
	Match match = matchBuilder.build();
	
        Uri destPortUri = destPort.getValue().firstKeyOf(NodeConnector.class, NodeConnectorKey.class).getId();


        Action outputToControllerAction = new ActionBuilder() //
                .setOrder(0)
                .setAction(new OutputActionCaseBuilder() //
                        .setOutputAction(new OutputActionBuilder() //
                                .setMaxLength(0xffff) //
                                .setOutputNodeConnector(destPortUri) //
                                .build()) //
                        .build()) //
                .build();

        // Create an Apply Action
        ApplyActions applyActions = new ApplyActionsBuilder().setAction(ImmutableList.of(outputToControllerAction))
                .build();

        // Wrap our Apply Action in an Instruction
        Instruction applyActionsInstruction = new InstructionBuilder() //
                .setOrder(0)
                .setInstruction(new ApplyActionsCaseBuilder()//
                        .setApplyActions(applyActions) //
                        .build()) //
                .build();

        // Put our Instruction in a list of Instructions
        macToMacFlow.setMatch(match) //
                .setInstructions(new InstructionsBuilder() //
                        .setInstruction(ImmutableList.of(applyActionsInstruction)) //
                        .build()) //
                .setPriority(priority) //
                .setBufferId(OFConstants.OFP_NO_BUFFER) //
                .setHardTimeout(flowHardTimeout) //
                .setIdleTimeout(flowIdleTimeout) //
                .setCookie(new FlowCookie(BigInteger.valueOf(flowCookieInc.getAndIncrement())))
                .setFlags(new FlowModFlags(false, false, false, false, false));


        return macToMacFlow.build();
    }

    /**
     * Starts and commits data change transaction which modifies provided flow
     * path with supplied body.
     *
     * @param flowPath
     * @param flow
     * @return transaction commit
     */
    private Future<RpcResult<AddFlowOutput>> writeFlowToConfigData(InstanceIdentifier<Flow> flowPath, Flow flow) {
        final InstanceIdentifier<Table> tableInstanceId = flowPath.<Table>firstIdentifierOf(Table.class);
        final InstanceIdentifier<Node> nodeInstanceId = flowPath.<Node>firstIdentifierOf(Node.class);
        final AddFlowInputBuilder builder = new AddFlowInputBuilder(flow);
	
        builder.setNode(new NodeRef(nodeInstanceId));
        builder.setFlowRef(new FlowRef(flowPath));
        builder.setFlowTable(new FlowTableRef(tableInstanceId));
        builder.setTransactionUri(new Uri(flow.getId().getValue()));
        return salFlowService.addFlow(builder.build());
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
}
