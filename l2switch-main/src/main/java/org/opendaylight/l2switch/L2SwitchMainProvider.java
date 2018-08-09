/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.l2switch;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.l2switch.flow.FlowWriterServiceImpl;
import org.opendaylight.l2switch.flow.InitialFlowWriter;
import org.opendaylight.l2switch.flow.ReactiveFlowWriter;
import org.opendaylight.l2switch.inventory.InventoryReader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.SalFlowService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2switch.l2switch.config.rev140528.L2switchConfig;
import org.opendaylight.yangtools.concepts.Registration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.opendaylight.l2switch.flow.docker.DockerCalls;
import java.util.Scanner;

public class L2SwitchMainProvider {

    private final static Logger LOG = LoggerFactory.getLogger(L2SwitchMainProvider.class);
    private Registration topoNodeListherReg = null, reactFlowWriterReg = null;

    private final DataBroker dataService;
    private final NotificationProviderService notificationService;
    private final SalFlowService salFlowService;
    private final L2switchConfig mainConfig;
    private String dataplaneIP="127.0.0.1";
    private String dockerPort="4243";
    private String ovsPort="6677";
    private String remote_ovs_port="6634";
    private String OFversion="13";

    public L2SwitchMainProvider(final DataBroker dataBroker,
            final NotificationProviderService notificationService,
            final SalFlowService salFlowService, final L2switchConfig config) {
        this.dataService = dataBroker;
        this.notificationService = notificationService;
        this.salFlowService = salFlowService;
        this.mainConfig = config;
    }

    public void init() {
        // Setup FlowWrtierService
        FlowWriterServiceImpl flowWriterService = new FlowWriterServiceImpl(salFlowService);
        flowWriterService.setFlowTableId(mainConfig.getReactiveFlowTableId());
        flowWriterService.setFlowPriority(mainConfig.getReactiveFlowPriority());
        flowWriterService.setFlowIdleTimeout(mainConfig.getReactiveFlowIdleTimeout());
        flowWriterService.setFlowHardTimeout(mainConfig.getReactiveFlowHardTimeout());

        // Setup InventoryReader
        InventoryReader inventoryReader = new InventoryReader(dataService);

        // Write initial flows
        if (mainConfig.isIsInstallDropallFlow()) {
            LOG.info("L2Switch will install a dropall flow on each switch");
            InitialFlowWriter initialFlowWriter = new InitialFlowWriter(salFlowService);
            initialFlowWriter.setFlowTableId(mainConfig.getDropallFlowTableId());
            initialFlowWriter.setFlowPriority(mainConfig.getDropallFlowPriority());
            initialFlowWriter.setFlowIdleTimeout(mainConfig.getDropallFlowIdleTimeout());
            initialFlowWriter.setFlowHardTimeout(mainConfig.getDropallFlowHardTimeout());
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
            ReactiveFlowWriter reactiveFlowWriter = new ReactiveFlowWriter(inventoryReader,
									   flowWriterService,
									   dataplaneIP, dockerPort,
									   ovsPort, remote_ovs_port,
									   OFversion);
            reactFlowWriterReg = notificationService.registerNotificationListener(reactiveFlowWriter);
        }

        LOG.info("L2SwitchMain initialized.");
    }

    public void close() throws Exception {
        if(reactFlowWriterReg != null) {
            reactFlowWriterReg.close();
        }
        if(topoNodeListherReg != null) {
            topoNodeListherReg.close();
        }
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
	docker.remoteDeleteFlows(dataplaneIP, remote_ovs_port, "13");
        LOG.info("L2SwitchMain (instance {}) torn down.", this);
    }

}
