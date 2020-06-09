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

    ExecShellCmd obj = new ExecShellCmd();
    
    public String findExistingContainers() {
	String cmd = "/usr/bin/sudo /usr/bin/docker ps --format '{{.Names}}'";
	String output=obj.exeCmd(cmd);
	return output;
    }

    public String remoteFindExistingContainers(String ip, String docker_port) {
	String cmd = String.format("curl -s -X GET http://%s:%s/containers/json | jq '.' | grep -A 1 --no-group-separator 'Names' | awk -F '\"' '{ print $2 }' | awk -F '/' '{ print $2 }'", ip, docker_port);
	String[] newCmd = {"/bin/sh", "-c", cmd};
	String output=obj.exeCmd(newCmd);
	return output;
    }

    public String findContainerMAC(String name) {
	String cmd = String.format("/usr/bin/sudo /usr/bin/docker inspect %s --format={{.NetworkSettings.MacAddress}}", name);
	String[] newCmd = {"/bin/sh", "-c", cmd};
	String output=obj.exeCmd(newCmd);
	return output;
    }

    public String findContainerMACNewIface(String name, String iface) {
	String cmd = String.format("/usr/bin/sudo /usr/bin/docker inspect %s --format={{.State.Pid}}", name);
	String[] newCmd = {"/bin/sh", "-c", cmd};
	String pid=obj.exeCmd(newCmd);
	cmd = String.format("/usr/bin/sudo /usr/bin/nsenter -t %s -n ip addr | grep -A 2 %s | grep ether | awk -F ' ' '{ print $2 }'", pid, iface);
	String[] secondCmd = {"/bin/sh", "-c", cmd};
	String output = obj.exeCmd(secondCmd);
	return output;
    }

    public String remoteFindContainerMACNewIface(String name, String ip, String dockerPort, String ovsBridge_remotePort, String ofPort, String OF_version) {
	String cmd = "";
	if(OF_version.equals("13")){
	    cmd = String.format("/usr/bin/sudo /usr/bin/ovs-ofctl -O Openflow13 show tcp:%s:%s | grep '%s(' | awk -F ':' '{ print $1 }' | awk -F '-' '{ print $2 }' | sed 's/)//g'", ip, ovsBridge_remotePort, ofPort);
	} else {
	    cmd = String.format("/usr/bin/sudo /usr/bin/ovs-ofctl show tcp:%s:%s | grep '%s(' | awk -F ':' '{ print $1 }' | awk -F '-' '{ print $2 }' | sed 's/)//g'", ip, ovsBridge_remotePort, ofPort);
	}
	String[] newCmd = {"/bin/sh", "-c", cmd};
	String netName = obj.exeCmd(newCmd);
	cmd = String.format("/usr/bin/curl -s -X GET http://%s:%s/containers/%s/json | jq '.NetworkSettings.Networks' | grep %s -A 8 | grep MacAddress | awk -F '\"' '{ print $4 }'", ip, dockerPort, name, netName);
	String[] secondCmd = {"/bin/sh", "-c", cmd};
	String output = obj.exeCmd(secondCmd);
	return output;
    }

    public void updateDefaultRoute(String bridge, String inPort, String newOutPort, String OF_version){
	String cmd = "";
	if(OF_version.equals("13")){
	    cmd = String.format("/usr/bin/sudo /usr/bin/ovs-ofctl -O Openflow13 dump-flows %s 'in_port=%s' --no-name | grep CONTROLLER | awk -F ' ' '{ print $7 }' | sed 's/CONTROLLER/output:%s,CONTROLLER/'", bridge, inPort, newOutPort);
	} else {
	    cmd = String.format("/usr/bin/sudo /usr/bin/ovs-ofctl dump-flows %s 'in_port=%s' --no-name | grep CONTROLLER | awk -F ' ' '{ print $7 }' | sed 's/CONTROLLER/output:%s,CONTROLLER/'", bridge, inPort, newOutPort);
	}
	String[] newCmd = {"/bin/sh", "-c", cmd};
	String newAction = obj.exeCmd(newCmd);
	if(OF_version.equals("13")) {
	    cmd = String.format("/usr/bin/sudo /usr/bin/ovs-ofctl -O Openflow13 mod-flows --strict %s 'in_port=%s, priority=2, %s'", bridge, inPort, newAction);
	} else {
	    cmd = String.format("/usr/bin/sudo /usr/bin/ovs-ofctl -O Openflow13 mod-flows --strict %s 'in_port=%s, priority=2, %s'", bridge, inPort, newAction);
	}
	String[] secondCmd = {"/bin/sh", "-c", cmd};
	String output = obj.exeCmd(secondCmd);
    }

    public void remoteUpdateDefaultRoute(String ip, String ovsBridge_remotePort, String inPort, String newOutPort, String OF_version){
	String cmd = "";
	if(OF_version.equals("13")){
	    cmd = String.format("/usr/bin/sudo /usr/bin/ovs-ofctl -O Openflow13 dump-flows tcp:%s:%s 'in_port=%s' --no-name | grep CONTROLLER | awk -F ' ' '{ print $7 }' | sed 's/CONTROLLER/output:%s,CONTROLLER/'", ip, ovsBridge_remotePort,  inPort, newOutPort);
	} else {
	    cmd = String.format("/usr/bin/sudo /usr/bin/ovs-ofctl dump-flows tcp:%s:%s 'in_port=%s' --no-name | grep CONTROLLER | awk -F ' ' '{ print $7 }' | sed 's/CONTROLLER/output:%s,CONTROLLER/'", ip, ovsBridge_remotePort,  inPort, newOutPort);
	}
	String[] newCmd = {"/bin/sh", "-c", cmd};
	String newAction = obj.exeCmd(newCmd);
	if(OF_version.equals("13")) {
	    cmd = String.format("/usr/bin/sudo /usr/bin/ovs-ofctl -O Openflow13 mod-flows --strict tcp:%s:%s 'in_port=%s, priority=2, %s'", ip, ovsBridge_remotePort, inPort, newAction);
	} else {
	    cmd = String.format("/usr/bin/sudo /usr/bin/ovs-ofctl mod-flows --strict tcp:%s:%s 'in_port=%s, priority=2, %s'", ip, ovsBridge_remotePort, inPort, newAction);
	}
	String[] secondCmd = {"/bin/sh", "-c", cmd};
	String output = obj.exeCmd(secondCmd);
    }

    public void remoteUpdateArp(String ip, String ovsBridge_remotePort, String newOutPort, String OF_version){
	String cmd = "";
	if(OF_version.equals("13")){
	    cmd = String.format("/usr/bin/sudo /usr/bin/ovs-ofctl -O Openflow13 dump-flows tcp:%s:%s 'arp' --no-name | awk -F ' ' '{ print $7 }' | sed 's/CONTROLLER/output:%s,CONTROLLER/'", ip, ovsBridge_remotePort, newOutPort);
	} else {
	    cmd = String.format("/usr/bin/sudo /usr/bin/ovs-ofctl dump-flows tcp:%s:%s 'arp' --no-name | awk -F ' ' '{ print $7 }' | sed 's/CONTROLLER/output:%s,CONTROLLER/'", ip, ovsBridge_remotePort, newOutPort);
	}
	String[] newCmd = {"/bin/sh", "-c", cmd};
	String newAction = obj.exeCmd(newCmd);
	if(OF_version.equals("13")) {
	    cmd = String.format("/usr/bin/sudo /usr/bin/ovs-ofctl -O Openflow13 mod-flows --strict tcp:%s:%s 'arp, priority=1, %s'", ip, ovsBridge_remotePort, newAction);
	} else {
	    cmd = String.format("/usr/bin/sudo /usr/bin/ovs-ofctl mod-flows --strict tcp:%s:%s 'arp, priority=1, %s'", ip, ovsBridge_remotePort, newAction);
	}
	String[] secondCmd = {"/bin/sh", "-c", cmd};
	String output = obj.exeCmd(secondCmd);
    }        
    
    public void startContainer(String name, String image) {
	String cmd = String.format("/usr/bin/sudo /usr/bin/docker run -itd --name %s %s", name, image);
	String output=obj.exeCmd(cmd);
	System.out.println("New Container Started "+name);
    }

    public void remoteStartContainer(String ip, String docker_port, String cont_name, String container_image, String devNum) {
	String cmd = String.format("/usr/bin/curl -s -X POST -H \"Content-Type: application/json\" http://%s:%s/v1.37/containers/create?name=%s -d \'{\"Image\": \"%s\", \"Env\": [\"PROTECTION_ID=%s\"], \"HostConfig\": {\"AutoRemove\": true, \"CapAdd\": [\"NET_ADMIN\"], \"Privileged\": true, \"Devices\": [\"/dev/uhcallkmod\"]}, \"Tty\": true}\'", ip, docker_port, cont_name, container_image, devNum);
	String[] newCmd = {"/bin/sh", "-c", cmd};
	String output=obj.exeCmd(newCmd);
	cmd=String.format("/usr/bin/curl -s -X POST http://%s:%s/v1.37/containers/%s/start", ip, docker_port, cont_name);
	String[] newCmd2={"/bin/sh", "-c", cmd};
	output=obj.exeCmd(newCmd2);
	System.out.println("New Container Started "+cont_name);
    }

    public void remoteStartContainer_wCmd(String ip, String docker_port, String cont_name, String container_image, String cont_cmd) {
	String cmd = String.format("/usr/bin/curl -s -X POST -H \"Content-Type: application/json\" http://%s:%s/v1.37/containers/create?name=%s -d \'{\"Image\": \"%s\", \"Cmd\": [\"%s\"], \"HostConfig\": {\"AutoRemove\": true}, \"Tty\": true}\'", ip, docker_port, cont_name, container_image, cont_cmd);
	String[] newCmd = {"/bin/sh", "-c", cmd};
	String output=obj.exeCmd(newCmd);
	cmd=String.format("/usr/bin/curl -s -X POST http://%s:%s/v1.37/containers/%s/start", ip, docker_port, cont_name);
	String[] newCmd2={"/bin/sh", "-c", cmd};
	output=obj.exeCmd(newCmd2);
	System.out.println("New Container Started "+cont_name);
    }
    
    public void remoteStartContainer(String ip, String docker_port, String cont_name, String container_image) {
	String cmd = String.format("/usr/bin/curl -s -X POST -H \"Content-Type: application/json\" http://%s:%s/v1.37/containers/create?name=%s -d \'{\"Image\": \"%s\", \"HostConfig\": {\"AutoRemove\": true}, \"Tty\": true}\'", ip, docker_port, cont_name, container_image);
	String[] newCmd = {"/bin/sh", "-c", cmd};
	String output=obj.exeCmd(newCmd);
	cmd=String.format("/usr/bin/curl -s -X POST http://%s:%s/v1.37/containers/%s/start", ip, docker_port, cont_name);
	String[] newCmd2={"/bin/sh", "-c", cmd};
	output=obj.exeCmd(newCmd2);
	System.out.println("New Container Started "+cont_name);
    }

    public void remoteCreateContainer(String ip, String docker_port, String cont_name, String container_image) {
	String cmd = String.format("/usr/bin/curl -s -X POST -H \"Content-Type: application/json\" http://%s:%s/v1.37/containers/create?name=%s -d \'{\"Image\": \"%s\", \"HostConfig\": {\"AutoRemove\": true}, \"Tty\": true}\'", ip, docker_port, cont_name, container_image);
	String[] newCmd = {"/bin/sh", "-c", cmd};
	String output=obj.exeCmd(newCmd);
    }

    public void remoteCreateContainer(String ip, String docker_port, String cont_name, String container_image, String devNum) {
	String cmd = String.format("/usr/bin/curl -s -X POST -H \"Content-Type: application/json\" http://%s:%s/v1.37/containers/create?name=%s -d \'{\"Image\": \"%s\", \"Env\": [\"PROTECTION_ID=%s\"], \"HostConfig\": {\"AutoRemove\": true, \"CapAdd\": [\"NET_ADMIN\"], \"Privileged\": true, \"Devices\": [\"/dev/uhcallkmod\"]}, \"Tty\": true}\'", ip, docker_port, cont_name, container_image, devNum);
	String[] newCmd = {"/bin/sh", "-c", cmd};
	String output=obj.exeCmd(newCmd);
    }        

    public void remoteStartContainer(String ip, String docker_port, String cont_name, String container_image, String cont_cmd, String hostPath, String contPath) {
	String cmd = String.format("/usr/bin/curl -s -X POST -H \"Content-Type: application/json\" http://%s:%s/v1.37/containers/create?name=%s -d \'{\"Image\": \"%s\", \"Cmd\": [\"%s\"], \"HostConfig\": {\"AutoRemove\": true, \"Binds\": [\"%s:%s\"]}, \"Tty\": true}\'", ip, docker_port, cont_name, container_image, cont_cmd, hostPath, contPath);
	String[] newCmd = {"/bin/sh", "-c", cmd};
	String output=obj.exeCmd(newCmd);
	cmd=String.format("/usr/bin/curl -s -X POST http://%s:%s/v1.37/containers/%s/start", ip, docker_port, cont_name);
	String[] newCmd2={"/bin/sh", "-c", cmd};
	output=obj.exeCmd(newCmd2);
	System.out.println("New Container Started "+cont_name);
    }

    public void remoteCreateContainer_bind_wCmd(String ip, String docker_port, String cont_name, String container_image, String cont_cmd, String hostPath, String contPath) {
	String cmd = String.format("/usr/bin/curl -s -X POST -H \"Content-Type: application/json\" http://%s:%s/v1.37/containers/create?name=%s -d \'{\"Image\": \"%s\", \"Cmd\": [\"%s\"], \"HostConfig\": {\"AutoRemove\": true, \"Binds\": [\"%s:%s\"]}, \"Tty\": true}\'", ip, docker_port, cont_name, container_image, cont_cmd, hostPath, contPath);
	String[] newCmd = {"/bin/sh", "-c", cmd};
	String output=obj.exeCmd(newCmd);
    }

    public void remoteCreateContainer_bind(String ip, String docker_port, String cont_name, String container_image, String devNum, String hostPath, String contPath) {
	String cmd = String.format("/usr/bin/curl -s -X POST -H \"Content-Type: application/json\" http://%s:%s/v1.37/containers/create?name=%s -d \'{\"Image\": \"%s\", \"Env\": [\"PROTECTION_ID=%s\"], \"HostConfig\": {\"AutoRemove\": true, \"CapAdd\": [\"NET_ADMIN\"], \"Binds\": [\"%s:%s\"], \"Privileged\": true, \"Devices\": [\"/dev/uhcallkmod\"]}, \"Tty\": true}\'", ip, docker_port, cont_name, container_image, devNum, hostPath, contPath);
	String[] newCmd = {"/bin/sh", "-c", cmd};
	String output=obj.exeCmd(newCmd);
    }        
    
    public void remoteStartContainer_bind(String ip, String docker_port, String cont_name, String container_image, String hostPath, String contPath) {
	String cmd = String.format("/usr/bin/curl -s -X POST -H \"Content-Type: application/json\" http://%s:%s/v1.37/containers/create?name=%s -d \'{\"Image\": \"%s\", \"HostConfig\": {\"AutoRemove\": true, \"Binds\": [\"%s:%s\"]}, \"Tty\": true}\'", ip, docker_port, cont_name, container_image, hostPath, contPath);
	String[] newCmd = {"/bin/sh", "-c", cmd};
	String output=obj.exeCmd(newCmd);
	cmd=String.format("/usr/bin/curl -s -X POST http://%s:%s/v1.37/containers/%s/start", ip, docker_port, cont_name);
	String[] newCmd2={"/bin/sh", "-c", cmd};
	output=obj.exeCmd(newCmd2);
	System.out.println("New Container Started "+cont_name);
    }

    public void remoteStartContainer_bind(String ip, String docker_port, String cont_name, String container_image, String devNum, String hostPath, String contPath) {
	String cmd = String.format("/usr/bin/curl -s -X POST -H \"Content-Type: application/json\" http://%s:%s/v1.37/containers/create?name=%s -d \'{\"Image\": \"%s\", \"Env\": [\"PROTECTION_ID=%s\"], \"HostConfig\": {\"AutoRemove\": true, \"CapAdd\": [\"NET_ADMIN\"], \"Binds\": [\"%s:%s\"], \"Privileged\": true, \"Device\": [\"/dev/uhcallkmod\"]}, \"Tty\": true}\'", ip, docker_port, cont_name, container_image, devNum, hostPath, contPath);
	String[] newCmd = {"/bin/sh", "-c", cmd};
	String output=obj.exeCmd(newCmd);
	cmd=String.format("/usr/bin/curl -s -X POST http://%s:%s/v1.37/containers/%s/start", ip, docker_port, cont_name);
	String[] newCmd2={"/bin/sh", "-c", cmd};
	output=obj.exeCmd(newCmd2);
	System.out.println("New Container Started "+cont_name);
    }
    
    public void remoteCreateContainer_bind(String ip, String docker_port, String cont_name, String container_image, String hostPath, String contPath) {
	String cmd = String.format("/usr/bin/curl -s -X POST -H \"Content-Type: application/json\" http://%s:%s/v1.37/containers/create?name=%s -d \'{\"Image\": \"%s\", \"HostConfig\": {\"AutoRemove\": true, \"Binds\": [\"%s:%s\"]}, \"Tty\": true}\'", ip, docker_port, cont_name, container_image, hostPath, contPath);
	String[] newCmd = {"/bin/sh", "-c", cmd};
	String output=obj.exeCmd(newCmd);
	System.out.println("New Container Started "+cont_name);
    }

    public void remoteAttatchArchive(String ip, String docker_port, String cont_name, String archiveFile, String contPath) {
	String cmd = String.format("/usr/bin/curl -s -X PUT -T %s http://%s:%s/v1.37/containers/%s/archive?path=%s", archiveFile, ip, docker_port, cont_name, contPath);
	String[] newCmd = {"/bin/sh", "-c", cmd};
	String output=obj.exeCmd(newCmd);
    }

    public void remoteStartCreatedContainer(String ip, String docker_port, String cont_name) {
	String cmd=String.format("/usr/bin/curl -s -X POST http://%s:%s/v1.37/containers/%s/start", ip, docker_port, cont_name);
	String[] newCmd={"/bin/sh", "-c", cmd};
	String output=obj.exeCmd(newCmd);
	System.out.println("New Container Started "+cont_name);
    }

    public void installOVSBridge(String name){
	String cmd=String.format("/usr/bin/sudo /usr/bin/ovs-vsctl --may-exist add-br %s", name);
	String output=obj.exeCmd(cmd);
	System.out.println("Added Bridge "+name);
    }

    public void remoteInstallOVSBridge(String ip, String ovs_port, String name){
	String cmd=String.format("/usr/bin/sudo /usr/bin/ovs-vsctl --db=tcp:%s:%s --may-exist add-br %s", ip, ovs_port, name);
	String output=obj.exeCmd(cmd);
	System.out.println("Added Bridge "+name);
    }    

    public void addExternalPort(String bridge, String iface){
	String cmd=String.format("/usr/bin/sudo /usr/bin/ovs-vsctl --may-exist add-port %s %s -- set Interface %s ofport_request=1", bridge, iface, iface);
	String output=obj.exeCmd(cmd);
	System.out.println("Added port: to bridge "+bridge+" for interface "+iface);
    }

    public void remoteAddExternalPort(String ip, String ovs_port, String bridge, String iface){
	String cmd=String.format("/usr/bin/sudo /usr/bin/ovs-vsctl --db=tcp:%s:%s --may-exist add-port %s %s -- set Interface %s ofport_request=1", ip, ovs_port, bridge, iface, iface);
	String output=obj.exeCmd(cmd);
	System.out.println("Added port: to bridge "+bridge+" for interface "+iface);
    }    

    public void addContainerPort(String bridge, String name, String iface) {
	String cmd = String.format("/usr/bin/sudo /usr/bin/ovs-docker add-port %s %s %s", bridge, iface, name);
	String output = obj.exeCmd(cmd);
	System.out.println("Added interface "+iface+" to container "+name);
    }

    public void remoteAddContainerPort(String bridge, String name, String iface, String ip, String ovs_port, String docker_port, String cont_ip) {
	String ip_arg=String.format("--ipaddress=%s", cont_ip);
	String cmd = String.format("/usr/bin/sudo /usr/bin/ovs-docker-remote add-port %s %s %s %s %s %s %s", bridge, iface, name, ip, ovs_port, docker_port, ip_arg);
	String output = obj.exeCmd(cmd);
	System.out.println("Added interface "+iface+" to container "+name+" with IP address "+cont_ip);
    }

    public String remoteAddContainerPort(String bridge, String name, String iface, String ip, String ovs_port, String docker_port, String cont_ip, String bridge_remote_port, String OF_version) {
	String ip_arg=String.format("--ipaddress=%s", cont_ip);
	String cmd = String.format("/usr/bin/sudo /usr/bin/ovs-docker-remote add-port %s %s %s %s %s %s %s", bridge, iface, name, ip, ovs_port, docker_port, ip_arg);
	String output = obj.exeCmd(cmd);
	System.out.println("Added interface "+iface+" to container "+name+" with IP address "+cont_ip);
	String OFPort = remoteFindContOfPort(ip, ovs_port, bridge_remote_port, name, iface, OF_version);
	return OFPort;
    }    


    public void addContainerPort(String bridge, String name, String iface, String ip) {
	String cmd = String.format("/usr/bin/sudo /usr/bin/ovs-docker add-port %s %s %s --ipaddress=%s", bridge, iface, name, ip);
	String output = obj.exeCmd(cmd);
	System.out.println("Added interface "+iface+" to container "+name);
    }    

    public void remoteAddContainerPort(String bridge, String name, String iface, String ip, String ovs_port, String docker_port) {
	String cmd = String.format("/usr/bin/sudo /usr/bin/ovs-docker-remote add-port %s %s %s %s %s %s", bridge, iface, name, ip, ovs_port, docker_port);
	String output = obj.exeCmd(cmd);
	System.out.println("Added interface "+iface+" to container "+name);
    }        
    
    public String remoteAddContainerPort(String bridge, String name, String iface, String ip, String ovs_port, String docker_port, String bridge_remote_port, String OF_version) {
	String cmd = String.format("/usr/bin/sudo /usr/bin/ovs-docker-remote add-port %s %s %s %s %s %s", bridge, iface, name, ip, ovs_port, docker_port);
	String output = obj.exeCmd(cmd);
	System.out.println("Added interface "+iface+" to container "+name);
	String OFPort = remoteFindContOfPort(ip, ovs_port, bridge_remote_port, name, iface, OF_version);
	return OFPort;	
    }    
    

    public String findContOfPort(String bridge, String name, String iface, String OF_version) {
	String cmd = String.format("/usr/bin/sudo /usr/bin/ovs-vsctl --data=bare --no-heading --columns=name find interface external_ids:container_id=%s external_ids:container_iface=%s", name, iface);
	String ovsPort=obj.exeCmd(cmd);
	ovsPort=ovsPort.replaceAll("\n","");
	System.out.println("OVS Port: "+ovsPort);

	if(OF_version.equals("13"))
	    cmd=String.format("/usr/bin/sudo /usr/bin/ovs-ofctl -O Openflow13 show %s | grep %s | awk -F '(' '{ print $1 }' | sed 's/ //g'", bridge, ovsPort);
	else
	    cmd=String.format("/usr/bin/sudo /usr/bin/ovs-ofctl show %s | grep %s | awk -F '(' '{ print $1 }' | sed 's/ //g'", bridge, ovsPort);
	String[] pipeCmd={"/bin/sh", "-c", cmd};
	String ofPort=obj.exeCmd(pipeCmd);
	ofPort=ofPort.replaceAll("\n","");
	System.out.println("OF Port: "+ofPort);
	return ofPort;
    }

    public String remoteFindContOfPort(String ip, String ovs_port, String bridge_remote_port, String name, String iface) {
	String cmd = String.format("/usr/bin/sudo /usr/bin/ovs-vsctl --db=tcp:%s:%s --data=bare --no-heading --columns=name find interface external_ids:container_id=%s external_ids:container_iface=%s", ip, ovs_port, name, iface);
	String ovsPort=obj.exeCmd(cmd);
	ovsPort=ovsPort.replaceAll("\n","");
	if (ovsPort.equals("")){
	    return ovsPort;
	}
	//System.out.println("OVS Port: "+ovsPort);

	cmd=String.format("/usr/bin/sudo /usr/bin/ovs-ofctl show tcp:%s:%s | grep %s | awk -F '(' '{ print $1 }' | sed 's/ //g'", ip, bridge_remote_port, ovsPort);
	String[] pipeCmd={"/bin/sh", "-c", cmd};
	String ofPort=obj.exeCmd(pipeCmd);
	ofPort=ofPort.replaceAll("\n","");
	//System.out.println("OF Port: "+ofPort);
	return ofPort;
    }

    public String remoteFindContOfPort(String ip, String ovs_port, String bridge_remote_port, String name, String iface, String OF_version) {
	String cmd = String.format("/usr/bin/sudo /usr/bin/ovs-vsctl --db=tcp:%s:%s --data=bare --no-heading --columns=name find interface external_ids:container_id=%s external_ids:container_iface=%s", ip, ovs_port, name, iface);
	String ovsPort=obj.exeCmd(cmd);
	ovsPort=ovsPort.replaceAll("\n","");
	if (ovsPort.equals("")){
	    return ovsPort;
	}	
	//System.out.println("OVS Port: "+ovsPort);

	if(OF_version.equals("13")){
	    cmd=String.format("/usr/bin/sudo /usr/bin/ovs-ofctl -O Openflow13 show tcp:%s:%s | grep %s | awk -F '(' '{ print $1 }' | sed 's/ //g'", ip, bridge_remote_port, ovsPort);
	} else {
	    cmd=String.format("/usr/bin/sudo /usr/bin/ovs-ofctl show tcp:%s:%s | grep %s | awk -F '(' '{ print $1 }' | sed 's/ //g'", ip, bridge_remote_port, ovsPort);
	}
	String[] pipeCmd={"/bin/sh", "-c", cmd};
	String ofPort=obj.exeCmd(pipeCmd);
	ofPort=ofPort.replaceAll("\n","");
	//System.out.println("OF Port: "+ofPort);
	return ofPort;
    }    
    
    public String findExternalOfPort(String bridge, String iface) {
	String cmd=String.format("/usr/bin/sudo /usr/bin/ovs-ofctl show %s | grep %s | awk -F '(' '{ print $1 }' | sed 's/ //g'", bridge, iface);
	String[] pipeCmd={"/bin/sh", "-c", cmd};
	String ofPort=obj.exeCmd(pipeCmd);
	ofPort=ofPort.replaceAll("\n","");
	System.out.println("OF Port: "+ofPort);
	return ofPort;
    }

    public String remoteFindExternalOfPort(String ip, String bridge_remote_port, String iface) {
	String cmd=String.format("/usr/bin/sudo /usr/bin/ovs-ofctl show tpc:%s:%s | grep %s | awk -F '(' '{ print $1 }' | sed 's/ //g'", ip, bridge_remote_port, iface);
	String[] pipeCmd={"/bin/sh", "-c", cmd};
	String ofPort=obj.exeCmd(pipeCmd);
	ofPort=ofPort.replaceAll("\n","");
	System.out.println("OF Port: "+ofPort);
	return ofPort;
    }    

    public void addFlow(String bridge, String in_port, String out_port) {
	String cmd=String.format("/usr/bin/sudo /usr/bin/ovs-ofctl add-flow %s 'priority=100 in_port=%s actions=output:%s'", bridge, in_port, out_port);
	String output = obj.exeCmd(cmd);
	System.out.println("Added flow "+in_port+" to "+out_port);
    }
    
    public void remoteAddFlow(String ip, String remote_bridge_port, String in_port, String out_port) {
	String cmd=String.format("/usr/bin/sudo /usr/bin/ovs-ofctl add-flow tcp:%s:%s 'priority=100 in_port=%s actions=output:%s'", ip, remote_bridge_port, in_port, out_port);
	String output = obj.exeCmd(cmd);
	System.out.println("Added flow "+in_port+" to "+out_port);
    }
    

    public void addFlow2D(String bridge, String port1, String port2, String OF_version) {
	String cmd = "";
	if(OF_version.equals("13"))
	    cmd=String.format("/usr/bin/sudo /usr/bin/ovs-ofctl -O Openflow13 add-flow %s 'priority=100 in_port=%s actions=output:%s'", bridge, port1, port2);
	else
	    cmd=String.format("/usr/bin/sudo /usr/bin/ovs-ofctl add-flow %s 'priority=100 in_port=%s actions=output:%s'", bridge, port1, port2);
	String[] pipeCmd={"/bin/sh", "-c", cmd};
	String output = obj.exeCmd(pipeCmd);
	if(OF_version.equals("13"))
	    cmd=String.format("/usr/bin/sudo /usr/bin/ovs-ofctl -O Openflow13 add-flow %s 'priority=100 in_port=%s actions=output:%s'", bridge, port2, port1);
	else
	    cmd=String.format("/usr/bin/sudo /usr/bin/ovs-ofctl add-flow %s 'priority=100 in_port=%s actions=output:%s'", bridge, port2, port1);
	String[] pipeCmd2={"/bin/sh", "-c", cmd};	
	output = obj.exeCmd(pipeCmd2);
	System.out.println("Added flow "+port1+" <==> "+port2);
    }

    public void remoteAddFlow2D(String ip, String remote_bridge_port, String port1, String port2) {
	String cmd=String.format("/usr/bin/sudo /usr/bin/ovs-ofctl add-flow tcp:%s:%s 'priority=100 in_port=%s actions=output:%s'", ip, remote_bridge_port, port1, port2);
	String output = obj.exeCmd(cmd);
	cmd=String.format("/usr/bin/sudo /usr/bin/ovs-ofctl add-flow tcp:%s:%s 'priority=100 in_port=%s actions=output:%s'", ip, remote_bridge_port, port2, port1);
	output = obj.exeCmd(cmd);
	System.out.println("Added flow "+port1+" <==> "+port2);
    }    

    public void setupRemoteSwitch(String ip, String ovs_port, String bridge, String remote_bridge_port) {
	String cmd=String.format("/usr/bin/sudo /usr/bin/ovs-vsctl --db=tcp:%s:%s set-controller %s ptcp:%s");
	String output = obj.exeCmd(cmd);
	System.out.println("Setup bridge "+bridge+" for remote operation, listening on port "+remote_bridge_port);
    }

    public void shutdownContainer(String name, String bridge_name) {
	String cmd = String.format("/usr/bin/sudo /usr/bin/docker kill %s", name);
	String output=obj.exeCmd(cmd);
	cmd = String.format("/usr/bin/sudo /usr/bin/docker rm %s", name);
	output=obj.exeCmd(cmd);
	cmd = String.format("/usr/bin/sudo /usr/bin/ovs-docker del-ports %s %s", bridge_name, name);
	output=obj.exeCmd(cmd);
    }


    public void remoteShutdownContainer(String ip, String docker_port, String name, String bridge, String ovs_port) {
	String cmd = String.format("/usr/bin/curl -s -X POST http://%s:%s/v1.37/containers/%s/kill", ip, docker_port, name);
	String[] newCmd = {"/bin/bash", "-c", cmd};
	String output=obj.exeCmd(newCmd);
	cmd = String.format("/usr/bin/sudo /usr/bin/ovs-docker-remote del-ports %s %s %s %s %s", bridge, name, ip, ovs_port, docker_port);
	output=obj.exeCmd(cmd);	
    }

    public void remoteShutdownContainer(String ip, String docker_port, String name, String bridge, String ovs_port, String remote_bridge_port, String OF_version) {
	remoteDeleteContFlows(ip, ovs_port, remote_bridge_port, OF_version, name);	
	String cmd = String.format("/usr/bin/curl -s -X POST http://%s:%s/v1.37/containers/%s/kill", ip, docker_port, name);
	String[] newCmd = {"/bin/bash", "-c", cmd};
	String output=obj.exeCmd(newCmd);
	cmd = String.format("/usr/bin/sudo /usr/bin/ovs-docker-remote del-ports %s %s %s %s %s", bridge, name, ip, ovs_port, docker_port);
	output=obj.exeCmd(cmd);	
    }     

    public void remoteShutdownContainer(String ip, String docker_port, String name) {
	String cmd = String.format("/usr/bin/curl -s -X POST http://%s:%s/v1.37/containers/%s/kill", ip, docker_port, name);
	String[] newCmd = {"/bin/bash", "-c", cmd};
	String output=obj.exeCmd(newCmd);
    }    

    
    public void removeBridges(String bridge_name) {
	String cmd = String.format("/usr/bin/sudo /usr/bin/ovs-ofctl del-flows %s", bridge_name);
	String output=obj.exeCmd(cmd);
	cmd = String.format("/usr/bin/sudo /usr/bin/ovs-vsctl --if-exists del-br %s", bridge_name);
	output=obj.exeCmd(cmd);
    }

    public void remoteRemoveBridges(String ip, String remote_bridge_port, String bridge, String ovs_port) {
	String cmd = String.format("/usr/bin/sudo /usr/bin/ovs-ofctl del-flows tcp:%s:%s", ip, remote_bridge_port);
	String output=obj.exeCmd(cmd);
	cmd = String.format("/usr/bin/sudo /usr/bin/ovs-vsctl --db=tcp:%s:%s --if-exists del-br %s", ip, ovs_port, bridge);
	output=obj.exeCmd(cmd);
    }

    public String findBridge() {
	String cmd = "/usr/bin/sudo /usr/bin/ovs-vsctl show | grep Bridge | awk -F '\"' '{ print $2 }'";
	String[] newCmd = {"/bin/bash", "-c", cmd};
	String output=obj.exeCmd(newCmd);	
	return output;
    }

    public String remoteFindBridge(String ip, String ovs_port) {
	String cmd = String.format("/usr/bin/sudo /usr/bin/ovs-vsctl --db=tcp:%s:%s show | grep Bridge | awk -F '\"' '{ print $2 }'", ip, ovs_port);
	String[] newCmd = {"/bin/bash", "-c", cmd};
	String output=obj.exeCmd(newCmd);	
	return output;
    }

    public void remoteDeleteFlows(String ip, String remote_bridge_port, String OF_version) {
	String cmd;
	if(OF_version.equals("13")){
	    cmd = String.format("/usr/bin/sudo /usr/bin/ovs-ofctl del-flows tcp:%s:%s -OOpenflow13", ip, remote_bridge_port);
	} else {
	    cmd = String.format("/usr/bin/sudo /usr/bin/ovs-ofctl del-flows tcp:%s:%s", ip, remote_bridge_port);
	}
	String output=obj.exeCmd(cmd);
    }

    public void remoteDeleteContFlows(String ip, String ovs_port, String remote_bridge_port, String OF_version, String name) {
	String OFPort;
	String cmd;
	String cmd2;	
	String[] ifaces={"eth1", "eth2"};
	String output;
	for (String iface:ifaces) {
	    OFPort = remoteFindContOfPort(ip, ovs_port, remote_bridge_port, name, iface, OF_version);
	    if (OFPort.equals("")) {
		continue;
	    }
	    if(OF_version.equals("13")){
		cmd = String.format("/usr/bin/sudo /usr/bin/ovs-ofctl del-flows tcp:%s:%s -OOpenflow13 in_port=%s", ip, remote_bridge_port,OFPort);
		cmd2 = String.format("/usr/bin/sudo /usr/bin/ovs-ofctl del-flows tcp:%s:%s -OOpenflow13 out_port=%s", ip, remote_bridge_port,OFPort);		    
	    } else {
		cmd = String.format("/usr/bin/sudo /usr/bin/ovs-ofctl del-flows tcp:%s:%s in_port=%s", ip, remote_bridge_port,OFPort);
		cmd2 = String.format("/usr/bin/sudo /usr/bin/ovs-ofctl del-flows tcp:%s:%s out_port=%s", ip, remote_bridge_port,OFPort);		    
	    }
	    output=obj.exeCmd(cmd);
	}
    }
    
    public void remoteDeleteContFlows(String ip, String ovs_port, String remote_bridge_port, String OF_version, String[] names) {
	String OFPort;
	String cmd;
	String cmd2;	
	String[] ifaces={"eth1", "eth2"};
	String output;
	for(String name:names) {
	    for (String iface:ifaces) {
		OFPort = remoteFindContOfPort(ip, ovs_port, remote_bridge_port, name, iface, OF_version);
		if (OFPort.equals("")) {
		    continue;
		}
		if(OF_version.equals("13")){
		    cmd = String.format("/usr/bin/sudo /usr/bin/ovs-ofctl del-flows tcp:%s:%s -OOpenflow13 in_port=%s", ip, remote_bridge_port,OFPort);
		    cmd2 = String.format("/usr/bin/sudo /usr/bin/ovs-ofctl del-flows tcp:%s:%s -OOpenflow13 out_port=%s", ip, remote_bridge_port,OFPort);		    
		} else {
		    cmd = String.format("/usr/bin/sudo /usr/bin/ovs-ofctl del-flows tcp:%s:%s in_port=%s", ip, remote_bridge_port,OFPort);
		    cmd2 = String.format("/usr/bin/sudo /usr/bin/ovs-ofctl del-flows tcp:%s:%s out_port=%s", ip, remote_bridge_port,OFPort);		    
		}
		output=obj.exeCmd(cmd);
	    }
	}
    }    

    public void remoteAddRoute(String ip, String dockerPort, String contName, String route, String device) {
	String cmd = String.format("/usr/bin/curl -s -X POST -H \"Content-Type: application/json\" http://%s:%s/v1.37/containers/%s/exec -d \'{\"AttachStdout\": true, \"Tty\": true, \"Privileged\": true, \"Cmd\": [\"ip\", \"route\", \"add\", \"%s\", \"dev\", \"%s\"]}\' | jq -r '.Id'", ip, dockerPort, contName, route, device);
	String[] newCmd = {"/bin/bash", "-c", cmd};
	String execID=obj.exeCmd(newCmd);

	String secondCmd = String.format("/usr/bin/curl -s -X POST -H \"Content-Type: application/json\" http://%s:%s/v1.37/exec/%s/start -d \'{\"Detach\": false, \"Tty\": true }\'", ip, dockerPort, execID);
	String[] newCmd2 = {"/bin/bash", "-c", secondCmd};
	String output=obj.exeCmd(newCmd2);

    }

    public void remoteAddRoute(String ip, String dockerPort, String contName, String route, String device, String contIP) {
	String cmd = String.format("/usr/bin/curl -s -X POST -H \"Content-Type: application/json\" http://%s:%s/v1.37/containers/%s/exec -d \'{\"AttachStdout\": true, \"Tty\": true, \"Privileged\": true, \"Cmd\": [\"ip\", \"route\", \"add\", \"%s\", \"dev\", \"%s\", \"src\", \"%s\"]}\' | jq -r '.Id'", ip, dockerPort, contName, route, device, contIP);
	String[] newCmd = {"/bin/bash", "-c", cmd};
	String execID=obj.exeCmd(newCmd);

	String secondCmd = String.format("/usr/bin/curl -s -X POST -H \"Content-Type: application/json\" http://%s:%s/v1.37/exec/%s/start -d \'{\"Detach\": false, \"Tty\": true }\'", ip, dockerPort, execID);
	String[] newCmd2 = {"/bin/bash", "-c", secondCmd};
	String output=obj.exeCmd(newCmd2);
    }    

    public void remoteSetDefault(String ip, String dockerPort, String contName, String device) {
	String cmd = String.format("/usr/bin/curl -s -X POST -H \"Content-Type: application/json\" http://%s:%s/v1.37/containers/%s/exec -d \'{\"AttachStdout\": true, \"Tty\": true, \"Privileged\": true, \"Cmd\": [\"ip\", \"route\", \"del\", \"default\"]}\' | jq -r '.Id'", ip, dockerPort, contName);
	String[] newCmd = {"/bin/bash", "-c", cmd};
	String execID=obj.exeCmd(newCmd);

	String secondCmd = String.format("/usr/bin/curl -s -X POST -H \"Content-Type: application/json\" http://%s:%s/v1.37/exec/%s/start -d \'{\"Detach\": false, \"Tty\": true }\'", ip, dockerPort, execID);
	String[] newCmd2 = {"/bin/bash", "-c", secondCmd};
	String output=obj.exeCmd(newCmd2);

	String thirdCmd = String.format("/usr/bin/curl -s -X POST -H \"Content-Type: application/json\" http://%s:%s/v1.37/containers/%s/exec -d \'{\"AttachStdout\": true, \"Tty\": true, \"Privileged\": true, \"Cmd\": [\"ip\", \"route\", \"add\", \"default\", \"dev\", \"%s\"]}\' | jq -r '.Id'", ip, dockerPort, contName, device);
	String[] newCmd3 = {"/bin/bash", "-c", thirdCmd};
	String execID2=obj.exeCmd(newCmd3);

	String fourthCmd = String.format("/usr/bin/curl -s -X POST -H \"Content-Type: application/json\" http://%s:%s/v1.37/exec/%s/start -d \'{\"Detach\": false, \"Tty\": true }\'", ip, dockerPort, execID2);
	String[] newCmd4 = {"/bin/bash", "-c", fourthCmd};
	String output2=obj.exeCmd(newCmd4);	
    }    

    
    public void remoteDisableGRO(String ip, String dockerPort, String contName, String iface) {
	String cmd = String.format("/usr/bin/curl -s -X POST -H \"Content-Type: application/json\" http://%s:%s/v1.37/containers/%s/exec -d \'{\"AttachStdout\": true, \"Tty\": true, \"Privileged\": true, \"Cmd\": [\"ethtool\", \"--offload\", \"%s\", \"tx\", \"off\", \"rx\", \"off\"]}\' | jq -r '.Id'", ip, dockerPort, contName, iface);
	String[] newCmd = {"/bin/bash", "-c", cmd};
	String execID=obj.exeCmd(newCmd);

	String secondCmd = String.format("/usr/bin/curl -s -X POST -H \"Content-Type: application/json\" http://%s:%s/v1.37/exec/%s/start -d \'{\"Detach\": false, \"Tty\": true }\'", ip, dockerPort, execID);
	String[] newCmd2 = {"/bin/bash", "-c", secondCmd};
	String output=obj.exeCmd(newCmd2);
    }

    public void restartContProcess(String ip, String dockerPort, String contName) {
	String cmd = String.format("/usr/bin/curl -s -X POST -H \"Content-Type: application/json\" http://%s:%s/v1.37/containers/%s/exec -d \'{\"AttachStdout\": true, \"Tty\": true, \"Privileged\": true, \"Cmd\": [\"./restart\"]}\' | jq -r '.Id'", ip, dockerPort, contName);
	String[] newCmd = {"/bin/bash", "-c", cmd};
	String execID=obj.exeCmd(newCmd);

	String secondCmd = String.format("/usr/bin/curl -s -X POST -H \"Content-Type: application/json\" http://%s:%s/v1.37/exec/%s/start -d \'{\"Detach\": false, \"Tty\": true }\'", ip, dockerPort, execID);
	String[] newCmd2 = {"/bin/bash", "-c", secondCmd};
	String output=obj.exeCmd(newCmd2);
    }    
	
}
