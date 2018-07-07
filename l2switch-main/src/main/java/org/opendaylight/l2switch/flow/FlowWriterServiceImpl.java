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
    private final Integer DEFAULT_HARD_TIMEOUT = 3600;
    private final Integer DEFAULT_IDLE_TIMEOUT = 1800;

    private String dataplaneIP = "192.1.1.1";
    private String dockerPort = "4243";
    private String ovsPort = "6677";
    private int containerCounter = 0;
    private Map macAddrMap = new HashMap();
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

        // add destMac-To-sourceMac flow on source port
        addMacToMacFlow(destMac, sourceMac, sourceNodeConnectorRef, destNodeConnectorRef);

        // add sourceMac-To-destMac flow on destination port
        addMacToMacFlow(sourceMac, destMac, destNodeConnectorRef, sourceNodeConnectorRef);

	//Add Docker Container -- directly contactable 
	if(!(inMap(macAddrMap, sourceMac.getValue(), destMac.getValue()))){
	    macAddrMap.put(sourceMac.getValue(), destMac.getValue());
	    String container_name = "demo"+containerCounter;
	    String iface = "eth1";
	    ++containerCounter;
	    Containers containerCalls = new Containers();
	    containerCalls.startContainer(dataplaneIP, dockerPort, container_name, "busybox");
	    String ovsBridge = containerCalls.getOVSBridge(dataplaneIP, ovsPort);
	    containerCalls.addPortOnContainer(dataplaneIP, dockerPort, ovsPort, ovsBridge, container_name, iface, "10.0.6.1/16");
	    String ovsBridge_remotePort = "6634";
	    String contOFPortNum = containerCalls.getContOFPortNum(dataplaneIP, ovsPort, ovsBridge_remotePort, container_name, iface, "13");
	    String contMAC = containerCalls.getContMAC(dataplaneIP, dockerPort, ovsBridge_remotePort, container_name, contOFPortNum, "13");
	    MacAddress contMac = containerCalls.str2Mac(contMAC);
	    macAddrMap.put(sourceMac.getValue(), contMAC);
	    Pattern pattern = Pattern.compile(":");
	    Uri destPortUri = destNodeConnectorRef.getValue().firstKeyOf(NodeConnector.class, NodeConnectorKey.class).getId();
	    String[] outPort = pattern.split(destPortUri.getValue());
	    NodeConnectorRef contNodeConnectorRef = containerCalls.getContainerNodeConnectorRef(String.format("%s:%s", outPort[0], outPort[1]), contOFPortNum);
	    addMacToMacFlow(destMac, contMac, contNodeConnectorRef, destNodeConnectorRef);
	    addMacToMacFlow(contMac, destMac, destNodeConnectorRef, contNodeConnectorRef);
	    containerCalls.addDirectContainerRouting(dataplaneIP, ovsPort, ovsBridge_remotePort, container_name, iface, "13", outPort[2]);
	    /*
	    DockerCalls docker = new DockerCalls();
	    docker.remoteStartContainer(dataplaneIP, dockerPort, container_name, "busybox");
	    String ovsBridge = docker.remoteFindBridge(dataplaneIP, ovsPort);
	    ovsBridge=ovsBridge.replaceAll("\n","");
	    //docker.addContainerPort(ovsBridge, container_name, iface, "10.0.6.1/16");
	    docker.remoteAddContainerPort(ovsBridge, container_name, iface, dataplaneIP, ovsPort, dockerPort,  "10.0.6.1/16");
	    //String contMAC = docker.findContainerMACNewIface(container_name, iface);

	    String contOFPort=docker.remoteFindContOfPort(dataplaneIP, ovsPort, ovsBridge_remotePort, container_name, iface, "13");
	    String contMAC = docker.remoteFindContainerMACNewIface(container_name, dataplaneIP, dockerPort, ovsBridge_remotePort, contOFPort, "13");
	    MacAddress contMac = new MacAddress(contMAC);	    

	    //docker.addFlow2D(ovsBridge, outPort[2], contOFPort, "13");
	    macAddrMap.put(sourceMac.getValue(), contMAC);
	    NodeId contNodeId = new NodeId(String.format("%s:%s", outPort[0], outPort[1]));
	    NodeConnectorId contNodeConnId = new NodeConnectorId(String.format("%s:%s:%s", outPort[0], outPort[1], contOFPort));
	    InstanceIdentifier<NodeConnector> contNodeConIId = NODES_IID.child(Node.class, new NodeKey(contNodeId)).child(NodeConnector.class, new NodeConnectorKey(contNodeConnId));
	    NodeConnectorRef contNodeConnectorRef = new NodeConnectorRef(contNodeConIId);
	    addMacToMacFlow(destMac, contMac, contNodeConnectorRef, destNodeConnectorRef);
	    addMacToMacFlow(contMac, destMac, destNodeConnectorRef, contNodeConnectorRef);
	    docker.remoteUpdateDefaultRoute(dataplaneIP, ovsBridge_remotePort, outPort[2], contOFPort, "13");
	    */
	}
	
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

		
    private boolean inMap(Map m1, String testKey, String testVal) {
	if (m1.containsKey(testKey)){
	    if(m1.get(testKey).equals(testVal))
		return true;
	    else
		return false;
	}
	else
	    return false;
    }
}
