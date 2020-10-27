/*                                                                                                                                                    * Copyright (c) 2020 Slab and others.  All rights reserved.                                                                                          *                                                                                                                                                    * This program and the accompanying materials are made available under the                                                                           * terms of the Eclipse Public License v1.0 which accompanies this distribution,                                                                      * and is available at http://www.eclipse.org/legal/epl-v10.html                                                                                      */

package org.opendaylight.l2switch.flow.middlebox;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class SnortRuleDB{
    public boolean initialized = false;
    private HashMap<String, String> RuleDB = new HashMap<String, String>();

    public void SnortRuleDB(){
	RuleDB.put("2017-0143", "alert tcp any any -> $HOME_NET 445 (msg:\"OS-WINDOWS Microsoft Windows SMBv1 identical MID and FID type confusion attempt\"; flow:to_server,established; content:\"|FF|SMB|2F 00 00 00 00|\"; depth:9; offset:4; fast_pattern; byte_test:1,!&,0x80,0,relative; content:\"|00 00 00 00 00 00 00 00 00 00|\"; within:10; distance:5; byte_extract:2,6,mid,relative,little; content:\"|FF 00|\"; within:2; distance:1; byte_test:2,=,mid,2,relative,little; content:\"|04 00|\"; within:2; distance:12; byte_test:2,>,65000,0,relative,little; byte_test:2,>,500,4,relative,little; metadata:policy balanced-ips drop, policy max-detect-ips drop, policy security-ips drop, service netbios-ssn; reference:cve,2017-0143; reference:url,technet.microsoft.com/en-us/security/bulletin/MS17-010; classtype:attempted-admin; sid:41984; rev:5;)");
	initialized=true;
    }

    public String getRule(String CVE){
	if (RuleDB.containsKey(CVE)){
	    return RuleDB.get(CVE);
	}
	return "";
    }
}
