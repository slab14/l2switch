/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.l2switch.flow.json;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.FileReader;
import com.google.gson.Gson;

public class GetFile {

    //returns string that does not have quotes, so cannot re-parse as a JSON
    public String readJson(String path) throws FileNotFoundException, IOException {
	BufferedReader bufferedReader = new BufferedReader(new FileReader(path));
	Gson gson = new Gson();
	Object json = gson.fromJson(bufferedReader, Object.class);
	String jsonString = json.toString();
	return jsonString;
    }

    public String readFile(String path) throws FileNotFoundException, IOException {
	BufferedReader bufferedReader = new BufferedReader(new FileReader(path));
	String output = new String();
	for (String line; (line = bufferedReader.readLine()) != null; output += line);
	return output;
    }
}


