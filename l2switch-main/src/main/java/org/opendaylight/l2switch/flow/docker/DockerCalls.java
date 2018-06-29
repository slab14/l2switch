/*
 * Copyright © 2017 slab and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */


package org.opendaylight.l2switch.flow.docker;

import java.io.IOException;
import org.opendaylight.l2switch.flow.docker.utils.ExecShellCmd;

public class DockerCalls {

    public String findExistingContainers() {
	String cmd = "/usr/bin/sudo /usr/bin/docker ps --format '{{.Names}}'";
	ExecShellCmd obj = new ExecShellCmd();
	String output=obj.exeCmd(cmd);
	return output;
    }

    public String remoteFindExistingContainers(String ip, String docker_port) {
	String cmd = String.format("curl -s -X GET http://%s:%s/containers/json | jq '.' | grep -A 1 --no-group-separator 'Names' | awk -F '\"' '{ print $2 }' | awk -F '/' '{ print $2 }'", ip, docker_port);
	String[] newCmd = {"/bin/sh", "-c", cmd};
	ExecShellCmd obj = new ExecShellCmd();
	String output=obj.exeCmd(newCmd);
	return output;
    }    
    
    public void startContainer(String name, String image) {
	String cmd = String.format("/usr/bin/sudo /usr/bin/docker run -itd --name %s %s", name, image);
	ExecShellCmd obj = new ExecShellCmd();
	String output=obj.exeCmd(cmd);
	System.out.println("New Container Started "+name);
    }

    public void remoteStartContainer(String ip, String docker_port, String cont_name, String container_image) {
	String cmd = String.format("/usr/bin/curl -s -X POST -H \"Content-Type: application/json\" http://%s:%s/v1.37/containers/create?name=%s -d \'{\"Image\": \"%s\", \"Cmd\": [\"/bin/sh\"], \"HostConfig\": {\"AutoRemove\": true}, \"Tty\": true}\'", ip, docker_port, cont_name, container_image);
	String[] newCmd = {"/bin/sh", "-c", cmd};
	ExecShellCmd obj = new ExecShellCmd();
	String output=obj.exeCmd(newCmd);
	cmd=String.format("/usr/bin/curl -s -X POST http://%s:%s/v1.37/containers/%s/start", ip, docker_port, cont_name);
	String[] newCmd2={"/bin/sh", "-c", cmd};
	output=obj.exeCmd(newCmd2);
	System.out.println("New Container Started "+cont_name);
    }    

    public void installOVSBridge(String name){
	String cmd=String.format("/usr/bin/sudo /usr/bin/ovs-vsctl --may-exist add-br %s", name);
	ExecShellCmd obj = new ExecShellCmd();
	String output=obj.exeCmd(cmd);
	System.out.println("Added Bridge "+name);
    }

    public void remoteInstallOVSBridge(String ip, String ovs_port, String name){
	String cmd=String.format("/usr/bin/sudo /usr/bin/ovs-vsctl --db=tcp:%s:%s --may-exist add-br %s", ip, ovs_port, name);
	ExecShellCmd obj = new ExecShellCmd();
	String output=obj.exeCmd(cmd);
	System.out.println("Added Bridge "+name);
    }    

    public void addExternalPort(String bridge, String iface){
	String cmd=String.format("/usr/bin/sudo /usr/bin/ovs-vsctl --may-exist add-port %s %s -- set Interface %s ofport_request=1", bridge, iface, iface);
	ExecShellCmd obj = new ExecShellCmd();
	String output=obj.exeCmd(cmd);
	System.out.println("Added port: to bridge "+bridge+" for interface "+iface);
    }

    public void remoteAddExternalPort(String ip, String ovs_port, String bridge, String iface){
	String cmd=String.format("/usr/bin/sudo /usr/bin/ovs-vsctl --db=tcp:%s:%s --may-exist add-port %s %s -- set Interface %s ofport_request=1", ip, ovs_port, bridge, iface, iface);
	ExecShellCmd obj = new ExecShellCmd();
	String output=obj.exeCmd(cmd);
	System.out.println("Added port: to bridge "+bridge+" for interface "+iface);
    }    

    public void addContainerPort(String bridge, String name, String iface) {
	String cmd = String.format("/usr/bin/sudo /usr/bin/ovs-docker add-port %s %s %s", bridge, iface, name);
	ExecShellCmd obj = new ExecShellCmd();
	String output = obj.exeCmd(cmd);
	System.out.println("Added interface "+iface+" to container "+name);
    }

    public void remoteAddContainerPort(String bridge, String name, String iface, String ip, String ovs_port, String docker_port, String cont_ip) {
	String ip_arg=String.format("--ipaddress=%s", cont_ip);
	try {
	    ProcessBuilder pb = new ProcessBuilder("/users/slab/IoT_Sec_Gateway/ovs_remote/ovs-docker-remote", "add-port", bridge, iface, name, ip, ovs_port, docker_port, ip_arg);
	    Process p = pb.start();
	    int errCode=p.waitFor();
	} catch (InterruptedException e) {
	    e.printStackTrace();
	} catch (IOException e) {
	    e.printStackTrace();
	}
	System.out.println("Added interface "+iface+" to container "+name+" with IP address "+cont_ip);
    }


    public void addContainerPort(String bridge, String name, String iface, String ip) {
	String cmd = String.format("/usr/bin/sudo /usr/bin/ovs-docker add-port %s %s %s --ipaddress=%s", bridge, iface, name, ip);
	ExecShellCmd obj = new ExecShellCmd();
	String output = obj.exeCmd(cmd);
	System.out.println("Added interface "+iface+" to container "+name);
    }    

    public void remoteAddContainerPort(String bridge, String name, String iface, String ip, String ovs_port, String docker_port) {
	try {
	    ProcessBuilder pb = new ProcessBuilder("/users/slab/IoT_Sec_Gateway/ovs_remote/ovs-docker-remote", "add-port", bridge, iface, name, ip, ovs_port, docker_port);
	    Process p = pb.start();
	    int errCode=p.waitFor();
	} catch (InterruptedException e) {
	    e.printStackTrace();
	} catch (IOException e) {
	    e.printStackTrace();
	}	
	System.out.println("Added interface "+iface+" to container "+name);
    }    
    

    public String findContOfPort(String bridge, String name, String iface) {
	String cmd = String.format("/usr/bin/sudo /usr/bin/ovs-vsctl --data=bare --no-heading --columns=name find interface external_ids:container_id=%s external_ids:container_iface=%s", name, iface);
	ExecShellCmd obj = new ExecShellCmd();
	String ovsPort=obj.exeCmd(cmd);
	ovsPort=ovsPort.replaceAll("\n","");
	System.out.println("OVS Port: "+ovsPort);

	cmd=String.format("/usr/bin/sudo /usr/bin/ovs-ofctl show %s | grep %s | awk -F '(' '{ print $1 }' | sed 's/ //g'", bridge, ovsPort);
	String[] pipeCmd={"/bin/sh", "-c", cmd};
	String ofPort=obj.exeCmd(pipeCmd);
	ofPort=ofPort.replaceAll("\n","");
	System.out.println("OF Port: "+ofPort);
	return ofPort;
    }

    public String remoteFindContOfPort(String ip, String ovs_port, String bridge_remote_port, String name, String iface) {
	String cmd = String.format("/usr/bin/sudo /usr/bin/ovs-vsctl --db=tcp:%s:%s --data=bare --no-heading --columns=name find interface external_ids:container_id=%s external_ids:container_iface=%s", ip, ovs_port, name, iface);
	ExecShellCmd obj = new ExecShellCmd();
	String ovsPort=obj.exeCmd(cmd);
	ovsPort=ovsPort.replaceAll("\n","");
	System.out.println("OVS Port: "+ovsPort);

	cmd=String.format("/usr/bin/sudo /usr/bin/ovs-ofctl show tcp:%s:%s | grep %s | awk -F '(' '{ print $1 }' | sed 's/ //g'", ip, bridge_remote_port, ovsPort);
	String[] pipeCmd={"/bin/sh", "-c", cmd};
	String ofPort=obj.exeCmd(pipeCmd);
	ofPort=ofPort.replaceAll("\n","");
	System.out.println("OF Port: "+ofPort);
	return ofPort;
    }
    
    public String findExternalOfPort(String bridge, String iface) {
	String cmd=String.format("/usr/bin/sudo /usr/bin/ovs-ofctl show %s | grep %s | awk -F '(' '{ print $1 }' | sed 's/ //g'", bridge, iface);
	String[] pipeCmd={"/bin/sh", "-c", cmd};
	ExecShellCmd obj = new ExecShellCmd();
	String ofPort=obj.exeCmd(pipeCmd);
	ofPort=ofPort.replaceAll("\n","");
	System.out.println("OF Port: "+ofPort);
	return ofPort;
    }

    public String remoteFindExternalOfPort(String ip, String bridge_remote_port, String iface) {
	String cmd=String.format("/usr/bin/sudo /usr/bin/ovs-ofctl show tpc:%s:%s | grep %s | awk -F '(' '{ print $1 }' | sed 's/ //g'", ip, bridge_remote_port, iface);
	String[] pipeCmd={"/bin/sh", "-c", cmd};
	ExecShellCmd obj = new ExecShellCmd();
	String ofPort=obj.exeCmd(pipeCmd);
	ofPort=ofPort.replaceAll("\n","");
	System.out.println("OF Port: "+ofPort);
	return ofPort;
    }    

    public void addFlow(String bridge, String in_port, String out_port) {
	String cmd=String.format("/usr/bin/sudo /usr/bin/ovs-ofctl add-flow %s 'priority=100 in_port=%s actions=output:%s'", bridge, in_port, out_port);
	ExecShellCmd obj = new ExecShellCmd();
	String output = obj.exeCmd(cmd);
	System.out.println("Added flow "+in_port+" to "+out_port);
    }
    
    public void remoteAddFlow(String ip, String remote_bridge_port, String in_port, String out_port) {
	String cmd=String.format("/usr/bin/sudo /usr/bin/ovs-ofctl add-flow tcp:%s:%s 'priority=100 in_port=%s actions=output:%s'", ip, remote_bridge_port, in_port, out_port);
	ExecShellCmd obj = new ExecShellCmd();
	String output = obj.exeCmd(cmd);
	System.out.println("Added flow "+in_port+" to "+out_port);
    }
    

    public void addFlow2D(String bridge, String port1, String port2) {
	String cmd=String.format("/usr/bin/sudo /usr/bin/ovs-ofctl add-flow %s 'priority=100 in_port=%s actions=output:%s'", bridge, port1, port2);
	ExecShellCmd obj = new ExecShellCmd();
	String output = obj.exeCmd(cmd);
	cmd=String.format("/usr/bin/sudo /usr/bin/ovs-ofctl add-flow %s 'priority=100 in_port=%s actions=output:%s'", bridge, port2, port1);
	output = obj.exeCmd(cmd);
	System.out.println("Added flow "+port1+" <==> "+port2);
    }

    public void remoteAddFlow2D(String ip, String remote_bridge_port, String port1, String port2) {
	String cmd=String.format("/usr/bin/sudo /usr/bin/ovs-ofctl add-flow tcp:%s:%s 'priority=100 in_port=%s actions=output:%s'", ip, remote_bridge_port, port1, port2);
	ExecShellCmd obj = new ExecShellCmd();
	String output = obj.exeCmd(cmd);
	cmd=String.format("/usr/bin/sudo /usr/bin/ovs-ofctl add-flow tcp:%s:%s 'priority=100 in_port=%s actions=output:%s'", ip, remote_bridge_port, port2, port1);
	output = obj.exeCmd(cmd);
	System.out.println("Added flow "+port1+" <==> "+port2);
    }    

    public void setupRemoteSwitch(String ip, String ovs_port, String bridge, String remote_bridge_port) {
	String cmd=String.format("/usr/bin/sudo /usr/bin/ovs-vsctl --db=tcp:%s:%s set-controller %s ptcp:%s");
	ExecShellCmd obj = new ExecShellCmd();
	String output = obj.exeCmd(cmd);
	System.out.println("Setup bridge "+bridge+" for remote operation, listening on port "+remote_bridge_port);
    }

    public void shutdownContainer(String name, String bridge_name) {
	ExecShellCmd obj = new ExecShellCmd();
	String cmd = String.format("/usr/bin/sudo /usr/bin/docker kill %s", name);
	String output=obj.exeCmd(cmd);
	cmd = String.format("/usr/bin/sudo /usr/bin/docker rm %s", name);
	output=obj.exeCmd(cmd);
	cmd = String.format("/usr/bin/sudo /usr/bin/ovs-docker del-ports %s %s", bridge_name, name);
	output=obj.exeCmd(cmd);
    }

    /*    
    public void remoteShutdownContainer(String ip, String docker_port, String name, String bridge, String ovs_port) {
	ExecShellCmd obj = new ExecShellCmd();
	String cmd = String.format("/usr/bin/curl -s -X POST http://%s:%s/v1.37/containers/%s/kill", ip, docker_port, name);
	String[] newCmd = {"/bin/bash", "-c", cmd};
	String output=obj.exeCmd(newCmd);
	try {
	    ProcessBuilder pb = new ProcessBuilder("/users/slab/IoT_Sec_Gateway/ovs_remote/ovs-docker-remote", "del-ports", bridge, name, ip, ovs_port, docker_port);
	    Process p = pb.start();
	    int errCode=p.waitFor();
	} catch (InterruptedException e) {
	    e.printStackTrace();
	} catch (IOException e) {
	    e.printStackTrace();
	}
    } 
    */

    public void remoteShutdownContainer(String ip, String docker_port, String name) {
	ExecShellCmd obj = new ExecShellCmd();
	String cmd = String.format("/usr/bin/curl -s -X POST http://%s:%s/v1.37/containers/%s/kill", ip, docker_port, name);
	String[] newCmd = {"/bin/bash", "-c", cmd};
	String output=obj.exeCmd(newCmd);
    }    

    
    public void removeBridges(String bridge_name) {
	String cmd = String.format("/usr/bin/sudo /usr/bin/ovs-ofctl del-flows %s", bridge_name);
	ExecShellCmd obj = new ExecShellCmd();
	String output=obj.exeCmd(cmd);
	cmd = String.format("/usr/bin/sudo /usr/bin/ovs-vsctl --if-exists del-br %s", bridge_name);
	output=obj.exeCmd(cmd);
    }

    public void remoteRemoveBridges(String ip, String remote_bridge_port, String bridge, String ovs_port) {
	String cmd = String.format("/usr/bin/sudo /usr/bin/ovs-ofctl del-flows tcp:%s:%s", ip, remote_bridge_port);
	ExecShellCmd obj = new ExecShellCmd();
	String output=obj.exeCmd(cmd);
	cmd = String.format("/usr/bin/sudo /usr/bin/ovs-vsctl --db=tcp:%s:%s --if-exists del-br %s", ip, ovs_port, bridge);
	output=obj.exeCmd(cmd);
    }    
    
}
