/*
 * Copyright (c) 2020 Slab and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.l2switch.flow.middlebox;

import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.rauschig.jarchivelib.*;
import java.io.File;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator; 
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.lang.Thread;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BuildTar{
    public void file2Tar(String tarpath, String infile) {
	File f = new File(tarpath);
	File dest = new File(f.getParent());
	File data = new File(infile);
	try{
	    Archiver archiver = ArchiverFactory.createArchiver("tar");
	    archiver.create(f.getName(), dest, data);	    		
	}catch(IOException e){
	    System.out.println("IOException: " + e.getMessage());
	}
    }

}
