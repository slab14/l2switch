/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.l2switch;

import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.NotificationService;
import org.opendaylight.l2switch.flow.FlowWriterServiceImpl;
import org.opendaylight.l2switch.flow.InitialFlowWriter;
import org.opendaylight.l2switch.flow.ReactiveFlowWriter;
import org.opendaylight.l2switch.inventory.InventoryReader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.SalFlowService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2switch.l2switch.config.rev140528.L2switchConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yangtools.concepts.Registration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.opendaylight.l2switch.flow.docker.DockerCalls;
import java.util.Scanner;
import org.opendaylight.l2switch.flow.json.PolicyParser;
import org.opendaylight.l2switch.flow.json.GetFile;
import java.io.FileNotFoundException;
import java.io.IOException;
import org.opendaylight.l2switch.flow.chain.ServiceChain;
import org.opendaylight.l2switch.flow.chain.PolicyMapBuilder;
import java.util.HashMap;
import org.opendaylight.l2switch.flow.chain.MacGroup;
import org.opendaylight.l2switch.flow.chain.PolicyStatus;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.l2switch.flow.middlebox.AlertReceiver;
import org.opendaylight.l2switch.flow.json.DevPolicy;
import org.opendaylight.yangtools.yang.common.Uint8;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.l2switch.NativeStuff;

public class L2SwitchMainProvider {
    private static final Logger LOG = LoggerFactory.getLogger(L2SwitchMainProvider.class);
    private Registration topoNodeListherReg;
    private Registration reactFlowWriterReg;

    private final DataBroker dataService;
    private final NotificationService notificationService;
    private final SalFlowService salFlowService;
    private final L2switchConfig mainConfig;
    private String dataplaneIP;//"127.0.0.1";
    private String dockerPort;//"4243";
    private String ovsPort;//="6677";
    private String remote_ovs_port;//="6634";
    private String OFversion;//="13";
    private String alertPort; //"6969"
    private PolicyParser policy;
    private HashMap<String, PolicyStatus> policyMap = new HashMap<String, PolicyStatus>();
    private AlertReceiver mboxAlertServer = new AlertReceiver();
    private boolean pkt_signing = false;
    private boolean prestart = false;

    public L2SwitchMainProvider(final DataBroker dataBroker,
            final NotificationService notificationPublishService,
            final SalFlowService salFlowService, final L2switchConfig config) {
        this.dataService = dataBroker;
        this.notificationService = notificationPublishService;
        this.salFlowService = salFlowService;
        this.mainConfig = config;
	this.dataplaneIP=config.getDataplaneIP();
	this.dockerPort=config.getDockerPort();
	this.ovsPort=config.getOvsPort();
	this.remote_ovs_port=config.getRemoteOvsPort();
	this.OFversion=config.getOFversion();
	this.alertPort=config.getAlertPort();
    }

    public void init() {
	GetFile policyReader=new GetFile();
	String jsonString=new String();
	try {
	    jsonString=policyReader.readFile(mainConfig.getPolicyFile());
	} catch (FileNotFoundException e) {
	    System.out.println("Error: "+e);
	} catch (IOException e) {
	    System.out.println("Error: "+e);
	}
	policy=new PolicyParser(jsonString);
	PolicyMapBuilder mapBuilder = new PolicyMapBuilder(policy);
	policyMap=mapBuilder.build();
    //System.out.println("number of devices: " + policy.parsed.getN());
    //System.out.println("cont: " + policy.parsed.devices[0].getProtections()[0].images[0]);
    
	
        // Setup FlowWrtierService
        FlowWriterServiceImpl flowWriterService = new FlowWriterServiceImpl(salFlowService);
        flowWriterService.setFlowTableId(Uint16.valueOf(mainConfig.getReactiveFlowTableId()).shortValue());
        flowWriterService.setFlowPriority(mainConfig.getReactiveFlowPriority().intValue());
        flowWriterService.setFlowIdleTimeout(mainConfig.getReactiveFlowIdleTimeout().intValue());
        flowWriterService.setFlowHardTimeout(mainConfig.getReactiveFlowHardTimeout().intValue());

        // Setup InventoryReader
        InventoryReader inventoryReader = new InventoryReader(dataService);

        // Write initial flows
        if (mainConfig.isIsInstallDropallFlow()) {
            LOG.info("L2Switch will install a dropall flow on each switch");
            
            InitialFlowWriter initialFlowWriter = new InitialFlowWriter(salFlowService);
            initialFlowWriter.setFlowTableId(Uint16.valueOf(mainConfig.getDropallFlowTableId()).shortValue());
            initialFlowWriter.setFlowPriority(mainConfig.getDropallFlowPriority().intValue());
            initialFlowWriter.setFlowIdleTimeout(mainConfig.getDropallFlowIdleTimeout().intValue());
            initialFlowWriter.setFlowHardTimeout(mainConfig.getDropallFlowHardTimeout().intValue());
            topoNodeListherReg = initialFlowWriter.registerAsDataChangeListener(dataService);
        }
        else {
            LOG.info("Dropall flows will not be installed");             
        }

        if (mainConfig.isIsLearningOnlyMode()) {
            LOG.info("L2Switch is in Learning Only Mode");            
        }
        else {
            // Setup reactive flow writer
            LOG.info("L2Switch will react to network traffic and install flows");
            System.out.println("L2Switch will react to network traffic and install flows");
            //check if pkt_signing is enabled in config
            if(mainConfig.isPktSigning()){
                LOG.info("YANG leaf: pkt_signing - enabled");                
                pkt_signing = true;

            }else{
                LOG.info("WARNING - pkt_signing is off which may affect container connectivity!");                
            }
            
            if(prestart){
                prestartOption();

            }

            ReactiveFlowWriter reactiveFlowWriter = new ReactiveFlowWriter(inventoryReader,
                                   flowWriterService,
                                   dataplaneIP, dockerPort,
                                   ovsPort, remote_ovs_port,
                                   OFversion, policy, policyMap, pkt_signing, prestart);
            
            
            reactFlowWriterReg = notificationService.registerNotificationListener(reactiveFlowWriter);

	    // Start up listening socket to receive middlebox alters
	    mboxAlertServer.setPort(Integer.parseInt(this.alertPort));
	    setupAlertReceiver(reactiveFlowWriter);
	    mboxAlertServer.startServer();
		    
        }

	// Setup maxStateDB
	NativeStuff cfunc = new NativeStuff();
	cfunc.initState(findMaxStates(policy), policy.parsed.n);
	
	System.out.println("\nReady");
        LOG.info("L2SwitchMain initialized.");        
    }


    //NEEDED to build back out the FULL contNCR: ncrs[i]=this.containerCalls.getContainerNodeConnectorRef(this.nodeStr, contOFPorts[i]);

    public void prestartOption(){
        HashMap<String, NodeConnectorRef[]> srcMac2contNCR = new HashMap<String, NodeConnectorRef[]>();
        String placeholder_IP = "0.0.0.0";
        NodeConnectorRef[] contNCRs;

        for (int i = 0; i < policy.parsed.getN(); i++){
            String contImageName = policy.parsed.devices[i].getProtections()[0].images[0];
            String contName = policy.parsed.devices[i].getProtections()[0].getImageOpts()[0].contName;            
            String contType = policy.parsed.devices[i].getProtections()[0].getChain().split("-")[0];
            System.out.println("Image: " + contImageName + " ContName: " + contName + " contType: " + contType);
            //contNCRs = policy.parsed.devices[i].getProtections()[0].getChain
            ServiceChain scWorker = new ServiceChain(dataplaneIP, dockerPort, ovsPort, OFversion, remote_ovs_port, policy.parsed.devices[i], String.valueOf(i), placeholder_IP); 
            srcMac2contNCR.put(policy.parsed.devices[i].inMAC, scWorker.pre_start());
        }

        // Print keys and values
        for (String i : srcMac2contNCR.keySet()) {
          System.out.println("key: " + i + " value: " + srcMac2contNCR.get(i));
        }
    }

    public void close() {
        if (reactFlowWriterReg != null) {
            reactFlowWriterReg.close();
        }

        if (topoNodeListherReg != null) {
            topoNodeListherReg.close();
        }
	mboxAlertServer.stopServer();	
	DockerCalls docker = new DockerCalls();
	String output = docker.remoteFindExistingContainers(dataplaneIP, dockerPort);
	Iterable<String> sc = () -> new Scanner(output).useDelimiter("\n");
	for(String line : sc) {
	    String name = line.replace("\'","");
	    if (!name.equals("")){
		String ovsBridge = docker.remoteFindBridge(dataplaneIP, ovsPort);
		docker.remoteShutdownContainer(dataplaneIP, dockerPort, name, ovsBridge, ovsPort);
	    }
	}
	docker.remoteDeleteFlows(dataplaneIP, remote_ovs_port, OFversion);
        LOG.info("L2SwitchMain (instance {}) torn down.", this);
    }

    private void setupAlertReceiver(ReactiveFlowWriter flowWriter){
	mboxAlertServer.setDataplaneIP(dataplaneIP);
	mboxAlertServer.setDockerPort(dockerPort);
	mboxAlertServer.setOvsPort(ovsPort);
	mboxAlertServer.setOFversion(OFversion);
	mboxAlertServer.setOvsBridgeRemotePort(remote_ovs_port);
	mboxAlertServer.setPolicy(policy.parsed.devices);
	mboxAlertServer.setPolicyMap(policyMap);
	mboxAlertServer.setFlowWriter(flowWriter);
    }

    private int[] findMaxStates(PolicyParser policy) {
	int i=0;
	int max = policy.parsed.n;
	int[] maxStates = new int[max];
	for(i=0; i<max; i++) {
	    maxStates[i]=policy.parsed.devices[i].states.length-1;
	}
	return maxStates;
    }
}

