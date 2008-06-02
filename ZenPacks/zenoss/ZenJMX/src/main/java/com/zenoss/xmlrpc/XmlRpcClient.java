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

import java.net.URL;
import java.net.MalformedURLException;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;

import org.apache.xmlrpc.XmlRpcException;

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
public class XmlRpcClient {

  // configuration for the XML-RPC session
  private XmlRpcClientConfigImpl _config;

  // client that is used to invoke operations on the server
  private org.apache.xmlrpc.client.XmlRpcClient _client;
  
  // the name of this performance collector
  private String _name;

  // logger
  private static final Log _logger = LogFactory.getLog(XmlRpcClient.class);


  /**
   * Creates an XmlRpcClient that is connected to the server at the
   * URL provided.
   * @param urlStr URL of the XMLRPC server to connect to
   * @throws MalformedURLException if the URL is malformed
   */
  public XmlRpcClient(String url, String monitorName)
    throws MalformedURLException {

    _config = new XmlRpcClientConfigImpl();
    _config.setServerURL(new URL(url));

    _client = new org.apache.xmlrpc.client.XmlRpcClient();
    _client.setConfig(_config);

    _name = monitorName;
  }


  /**
   * Closes down all the connections and references in the class
   */
  public void close() {
    _config = null;
    _client = null;
    _name = null;
  }


  /**
   * Sets the username and password to be used when authenticating
   * with the XMLRPC server.
   * @param username the username to authenticate with
   * @param password the password to authenticate with
   */
  public void setCredentials(String username, String password) {
    _config.setBasicUserName(username);
    _config.setBasicPassword(password);

    // this may not be necessary
    _client.setConfig(_config);
  }


  /**
   * Loads the configurations for the data source type provided
   * @param dsType the type of the data source
   * @return a List<Map<String, Object>> where each item in the List
   * is a configuration.
   * @throws XmlRpcException if any error occurs while retrieving the
   * configurations
   */
  public List<Map> getConfigs(String dsType)
    throws XmlRpcException {

    // assemble the parameters to the operation
    Object[] params = new Object[] { _name, dsType };

    // invoke the operation and cast result to an Object[]
    Object result = _client.execute("getConfigs", params);
    Object[] results = (Object[]) result;

    // convert the Object[] to a List<Map<Object, Object>>
    List<Map> configs = new ArrayList<Map>();
    for (int i = 0; i < results.length; i++) {
      Map config = (Map) results[i];
      configs.add(config);
    }

    return configs;
  }


  /**
   * Retrieves the performance configuration for the collector with
   * the name provided.
   * @return a Map<String, Object> that represents the configuration
   * of the collector.  If no collector can be found with the name
   * provided null is returned.
   * @throws XmlRpcException if an error occurs obtaining the configuration
   */
  public Map getPerformanceConfig() 
    throws XmlRpcException {

    // assemble the parameters to the operation
    Object[] params = new Object[] { _name };

    // invoke the operation and cast result to an Object[]
    Object result = _client.execute("getPerformanceConfig", params);
    Map results = (Map) result;

    return results;
  }


  /**
   * Writes the value for the data point provided to the device
   */
  public void writeRRD(String device, String dataPoint, Object value) 
    throws XmlRpcException {

    // assemble the parameters to the operation
    Object[] params = new Object[] { device, "", "", dataPoint, value };

    // invoke the operation and cast result to an Object[]
    Object result = _client.execute("writeRRD", params);
  }


  /**
   * Sends a heartbeat event for the device and component provided.
   */
  public void sendHeartbeat(String device, String component, int timeout)
    throws XmlRpcException {

    // heartbeats are sent as events with only 2 fields present
    Map<String, Object> values = new HashMap<String, Object>();
    values.put("device", device);
    values.put("component", component);
    values.put("summary", component + " heartbeat");
    values.put("severity", new Integer(0));
    values.put("eventClass", "/Heartbeat");
    values.put("timeout", timeout);
    
    Object[] params = new Object[] { values };
    Object result = _client.execute("sendEvent", params);
  }

}
