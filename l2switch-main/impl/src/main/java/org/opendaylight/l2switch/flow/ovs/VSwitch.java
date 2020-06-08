/*
 * Copyright (c) 2020 Slab and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.l2switch.flow.ovs;

public class VSwitch {
    private String dataplaneIP;
    private String ovsRemote_bridgePort;
    private String OFversion="13";

    public VSwitch(String ip, String port){
	this.dataplaneIP=ip;
	this.ovsRemote_bridgePort=port;
    }

    public VSwitch(String ip, String port, String ver){
	this.dataplaneIP=ip;
	this.ovsRemote_bridgePort=port;
	this.OFversion=ver;
    }

    public String getIP(){
	return dataplaneIP;
    }

    public String getPort(){
	return ovsRemote_bridgePort;
    }

    public String getOFver(){
	return OFversion;
    }

    public void setIP(String ip){
	this.dataplaneIP=ip;
    }

    public void setPort(String port){
	this.ovsRemote_bridgePort=port;
    }

    public void setOFver(String ver){
	this.OFversion=ver;
    }

    public void setOFver(int ver){
	this.OFversion=String.valueOf(ver);
    }


}
