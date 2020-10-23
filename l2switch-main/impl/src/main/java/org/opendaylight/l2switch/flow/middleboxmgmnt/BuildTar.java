/*
 * Copyright (c) 2020 Slab and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.l2switch.flow.middlebox;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.Thread;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.utils.IOUtils;
import org.rauschig.jarchivelib.*;



public class BuildTar{
    public void file2Tar(String tarpath, String infile) {
	File f = new File(tarpath);
	File dest = new File(f.getParent());
	File data = new File(infile);
    	if(f.exists()){
	    Timestamp ts = new Timestamp(System.currentTimeMillis());
	    File old = new File(tarpath+"."+ts.getTime());
	    f.renameTo(old);
	}	
	try{
	    Archiver archiver = ArchiverFactory.createArchiver("tar");
	    archiver.create(f.getName(), dest, data);	    		
	}catch(IOException e){
	    System.out.println("IOException: " + e.getMessage());
	}
    }

    public void ports2Tar(String tarpath, ArrayList<String> ports, String action){
	File f = new File(tarpath); 
    	File dest = new File(f.getParent()); 
    	File rules = new File(f.getParent() + "/setup_iptables.sh");
	String msg="";
    	if(f.exists()){
	    Timestamp ts = new Timestamp(System.currentTimeMillis());
	    File old = new File(tarpath+"."+ts.getTime());
	    f.renameTo(old);
	}
    	try{
	    FileWriter writer = new FileWriter(rules, false);
	    if(action.equals("allow")){
		// set default drop rules
		msg = "iptables -P INPUT DROP\n";
		writer.write(msg);
		msg = "iptables -P FORWARD DROP\n";
		writer.write(msg);
		msg = "iptables -P OUTPUT DROP\n";
		writer.write(msg);		
		for(String port:ports){
		    msg = String.format("iptables -A FORWARD -p tcp --dport %s -j ACCEPT\n", port);
		    writer.write(msg);
		    msg = String.format("iptables -A FORWARD -p tcp --sport %s -j ACCEPT\n", port);
		    writer.write(msg);				    
		    msg = String.format("iptables -A INPUT -p tcp --dport %s -j ACCEPT\n", port);
		    writer.write(msg);				    
		    msg = String.format("iptables -A INPUT -p tcp --sport %s -j ACCEPT\n", port);
		    writer.write(msg);
		}
	    } else if (action.equals("drop")){
		for(String port:ports){
		    msg = String.format("iptables -A FORWARD -p tcp --dport %s -j DROP\n", port);
		    writer.write(msg);
		    msg = String.format("iptables -A FORWARD -p tcp --sport %s -j DROP\n", port);
		    writer.write(msg);
		    msg = String.format("iptables -A INPUT -p tcp --dport %s -j DROP\n", port);
		    writer.write(msg);
		    msg = String.format("iptables -A INPUT -p tcp --sport %s -j DROP\n", port);
		    writer.write(msg);		    
		}
	    }
	    writer.close();
	    Archiver archiver = ArchiverFactory.createArchiver("tar");
	    archiver.create(f.getName(), dest, rules);	    		
	    rules.delete(); 
	}catch(IOException ioe){
	    System.out.println("IOException: " + ioe.getMessage());
	}
    }

}
