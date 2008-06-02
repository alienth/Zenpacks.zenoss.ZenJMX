///////////////////////////////////////////////////////////////////////////
//
// This program is part of Zenoss Core, an open source monitoring platform.
// Copyright (C) 2007, Zenoss Inc.
//
// This program is free software; you can redistribute it and/or modify it
// under the terms of the GNU General Public License version 2 as published by
// the Free Software Foundation.
//
// For complete information please visit: http://www.zenoss.com/oss/
//
///////////////////////////////////////////////////////////////////////////
package com.zenoss.xmlrpc;

import java.net.MalformedURLException;

import java.util.Map;
import java.util.List;
import java.util.Iterator;

import org.apache.xmlrpc.XmlRpcException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * <p> Demonstrates how to use the XmlRpcClient. </p>
 *
 * <p> This demo retrieves all of the data source of a given type
 * associated with a particular performance collector.  It
 * demonstrates how any Java based performance collector can use
 * XML-RPC to retrieve performance templates (data sources). </p>
 *
 * <p>$Author: chris $<br>
 * $Date: 2005/03/13 18:45:25 $</p>
 *
 * @author Christopher Blunck
 * @version $Revision: 1.6 $
 */
public class XmlRpcDemo {
  // URL of the XmlRpc server to talk to
  public static final String URL = "http://localhost:8081/";

  // authentication information
  public static final String USERNAME = "admin";
  public static final String PASSWORD = "zenoss";

  // unique name of this performance collector
  public static final String PERFORMANCE_COLLECTOR_NAME = "jmx1";

  // the type of data sources to retrieve from the server
  public static final String DATASOURCE_TYPE = "JMX";


  // client used for all communication
  private XmlRpcClient _client;

  // logger
  private static final Log _logger = LogFactory.getLog(XmlRpcDemo.class);


  /**
   * Creates an XmlRpcDemo
   * @param url the URL of the server to connect to
   * @param name the name of the performance monitor
   */
  public XmlRpcDemo(String url, String name) 
    throws IllegalArgumentException {

    try {
      _client = new XmlRpcClient(url, name);
    } catch (MalformedURLException e) {
      throw new IllegalArgumentException(e);
    }
  }


  /**
   * Sets the authentication inforamtion
   * @param username the basic username to authenticate with
   * @param password the basic password to authenticate with
   */
  public void setCredentials(String username, String password) {
    _client.setCredentials(username, password);
  }

  
  /**
   * Retrieves the configurations on the server for given data source type
   * @param dsType the type of datasource for which configurations
   * should be retrieved
   * @throws XmlRpcException if an error occurs while retrieving the
   * configuration
   */
  public void getConfigs(String dsType) 
    throws XmlRpcException {

    // get the configuration
    List<Map> configs = _client.getConfigs(dsType);
    _logger.info("number of data sources: " + configs.size());

    // iterate over the configurations and print them out
    Iterator<Map> iter = configs.iterator();
    while (iter.hasNext()) {
      Map config = iter.next();
      _logger.debug("data source: " + config);
    }
  }


  /**
   * Runs the demo using the public static fields in the class
   */
  public static void start()
    throws Exception {
    
    // construct and connect
    XmlRpcDemo demo = new XmlRpcDemo(URL, PERFORMANCE_COLLECTOR_NAME);
    demo.setCredentials(USERNAME, PASSWORD);
    demo.getConfigs(DATASOURCE_TYPE);
  }
}
