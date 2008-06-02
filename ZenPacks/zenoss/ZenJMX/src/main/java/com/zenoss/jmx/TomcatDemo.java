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
package com.zenoss.jmx;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Arrays;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * <p></p>
 *
 * <p>$Author: chris $<br>
 * $Date: 2005/03/13 18:45:25 $</p>
 *
 * @author Christopher Blunck
 * @version $Revision: 1.6 $
 */
public class TomcatDemo {
  // JMX URL to connect to and interrogate
  public static final String URL = 
    "service:jmx:rmi:///jndi/rmi://localhost:12346/jmxrmi";

  // authentication information
  public static final String USERNAME = "admin";
  public static final String PASSWORD = "admin";


  // single-value mbean and attribute
  public static final String SINGLE_VALUE_MBEAN = 
    "Catalina:type=Cache,host=localhost,path=/zenjmx";
  public static final String SINGLE_VALUE_ATTRIBUTE = "accessCount";


  // multi-value mbean name and attribute
  public static final String MULTI_VALUE_MBEAN = 
    "java.lang:type=Memory";
  public static final String MULTI_VALUE_ATTRIBUTE = "HeapMemoryUsage";
  public static final String[] MULTI_VALUE_KEYS = 
    new String[] { "used" };


  // jboss operation mbean, name, parameters, and method signature
  public static final String OPERATION_MBEAN = 
    "Application:Name=Server,Type=Server";
  public static final String OPERATION_NAME = "calcUptime";
  public static final Object[] OPERATION_PARAMS = 
    new Object[] { 
  };
  public static final String[] OPERATION_SIGNATURE = 
    new String[] { };

  public static final String[] OPERATION_KEYS = 
    new String[] { "uptime" };


  // the connection to the JMX Agent
  private JmxClient _client;

  // logger
  private static final Log _logger = LogFactory.getLog(TomcatDemo.class);


  /**
   * Creates a Main instance using the parameters provided
   * @param url the JMX URL to connect to
   */
  public TomcatDemo(String url) {
    _client = new JmxClient(url);
  }


  /**
   * Connects to the server and authenticates using the credentials
   * provided
   * @param creds authentication information
   */
  public void connect(String username, String password) 
    throws JmxException {

    String[] creds = new String[] { username, password };

    _client.setCredentials(creds);
    _client.connect();
  }


  /**
   * Queries the server for the object and attribute provided
   */
  public Map<String, Object> query(String object,
                                   String attribute,
                                   List<String> keys) 
    throws JmxException {

    return _client.query(object, attribute, keys);
  }


  /**
   * Queries the server for the object and attribute provided
   */
  public Object query(String object, String attribute) 
    throws JmxException {

    return _client.query(object, attribute);
  }


  /**
   * Invokes the requested operation on the server
   */
  public Object invoke(String object, 
                       String operation, 
                       Object[] params, 
                       String[] types) 
    throws JmxException {
    
    return _client.invoke(object, operation, params, types);
  }


  /**
   * Creates a Main and runs it
   */
  public static void start()
    throws Exception {

    // construct and connect
    TomcatDemo demo = new TomcatDemo(URL);
    demo.connect(USERNAME, PASSWORD);

    // issue single-value query
    Object o = demo.query(SINGLE_VALUE_MBEAN, SINGLE_VALUE_ATTRIBUTE);
    _logger.info("tomcat single-value output: " + o);

    // issue multi-value query
    List<String> keys = Arrays.asList(MULTI_VALUE_KEYS);
    Map<String, Object> result = 
      demo.query(MULTI_VALUE_MBEAN, MULTI_VALUE_ATTRIBUTE, keys);
    _logger.info("tomcat multi-value output: " + result);

    // issue an operation query
    keys = Arrays.asList(OPERATION_KEYS);
    o = demo.invoke(OPERATION_MBEAN, 
                    OPERATION_NAME, OPERATION_PARAMS, OPERATION_SIGNATURE);
    _logger.info("tomcat operation output: " + o);
  }  
}
