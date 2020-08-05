/*
 * Copyright (c) 2018 Slab and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.l2switch.flow.middlebox; 

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator; 

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory; 

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class NmapParser extends DefaultHandler {
    
    String nmap_string;
    String toPrint;
    String state;
    List<String> openPorts = new ArrayList<>();
   
    public NmapParser(String nmap_string, String type) {
        this.nmap_string = nmap_string;

	if (type.equals("openports"))
	    parsePortDocument();
	else if (type.equals("CVE"))
	    parseCVEDocument();
    }

    public List getOpenPorts(){
        return openPorts;
    }

    private void parsePortDocument() {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        try {
            SAXParser parser = factory.newSAXParser();
            parser.parse(new InputSource(new StringReader(nmap_string)), this);
        } catch (ParserConfigurationException e) {
            System.out.println("ParserConfig error");
        } catch (SAXException e) {
            System.out.println("SAXException : " + e.getMessage());
            
        } catch (IOException e) {
            System.out.println("IO error");
        }
    }

    private void parseCVEDocument() {
	System.out.println("parse CVE doc");
    }
	    

    /* [startElement], [endElement], [characters] are methods to be overriden */
    
    @Override
    public void startElement(String s, String s1, String elementName, Attributes attributes) throws SAXException {
         if (elementName.equalsIgnoreCase("port")) {
	    openPorts.add(attributes.getValue("portid"));
	 }
	 if (elementName.equalsIgnoreCase("state")) {
	     state = attributes.getValue("state");
	     if (!state.equalsIgnoreCase("open")){
		 if(!openPorts.isEmpty()){
		     int index = openPorts.size() - 1;
		     openPorts.remove(index);  
		 }                 
	     }
	 }
    }

    @Override
    public void endElement(String s, String s1, String element) throws SAXException {}

    @Override
    public void characters(char[] ac, int i, int j) throws SAXException {}

   
}
