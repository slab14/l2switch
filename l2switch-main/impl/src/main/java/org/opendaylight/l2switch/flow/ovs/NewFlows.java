/*
 * Copyright © 2020 slab and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */


package org.opendaylight.l2switch.flow.ovs;

import java.io.IOException;
import org.opendaylight.l2switch.flow.docker.utils.ExecShellCmd;
import org.opendaylight.l2switch.flow.ovs.FlowRule;
import org.opendaylight.l2switch.flow.ovs.VSwitch;


public class NewFlows {
    public void writeNewFlow(VSwitch vswitch, FlowRule rule, String action) {
	String matchAction = String.format("priority=%s in_port=%s,dl_%s=%s  actions=%s,output:%", rule.getPriority(), rule.getMatchPort(), rule.getLoc(), rule.getMatchMAC(), action, rule.getActionPort());
	String cmd = "";
	String opts = "";
	if(vswitch.getOFver().equals("13")){
	    opts="-O Openflow13";
	}
	cmd = String.format("/usr/bin/sudo /usr/bin/ovs-ofctl %s add-flow tcp:%s:%s '%s'", opts, vswitch.getIP(), vswitch.getPort(), matchAction);
	String[] newCmd = {"/bin/sh", "-c", cmd};
	ExecShellCmd obj = new ExecShellCmd();
	obj.exeCmd(newCmd);
}


    public void writeNewFlow(String ip, String ovsBridge_remotePort, FlowRule rule, String action, String OF_version) {
	String matchAction = String.format("priority=%s in_port=%s,dl_%s=%s  actions=%s,output:%", rule.getPriority(), rule.getMatchPort(), rule.getLoc(), rule.getMatchMAC(), action, rule.getActionPort());
	String cmd = "";
	String opts = "";
	if(OF_version.equals("13")){
	    opts="-O Openflow13";
	}
	cmd = String.format("/usr/bin/sudo /usr/bin/ovs-ofctl %s add-flow tcp:%s:%s '%s'", opts, ip, ovsBridge_remotePort, matchAction);
	String[] newCmd = {"/bin/sh", "-c", cmd};
	ExecShellCmd obj = new ExecShellCmd();
	obj.exeCmd(newCmd);
}    
