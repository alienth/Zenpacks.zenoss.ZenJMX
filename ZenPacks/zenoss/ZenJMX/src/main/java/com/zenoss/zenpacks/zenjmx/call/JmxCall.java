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
package com.zenoss.zenpacks.zenjmx.call;

import java.util.Random;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Iterator;

import java.util.concurrent.Callable;

import java.io.IOException;

import com.zenoss.jmx.JmxClient;
import com.zenoss.jmx.JmxException;


/**
 * <p> Callable that queries a JMX Agent. </p>
 *
 * <p> Programmer's Note: You must update the hashCode() definition if
 * you add additional fields to the class.  See the javadoc of
 * hashCode() for additional information. </p>
 *
 * <p>$Author: chris $<br>
 * $Date: 2005/03/13 18:45:25 $</p>
 *
* @author Christopher Blunck
 * @version $Revision: 1.6 $
 */
public abstract class JmxCall
  implements Callable {

  // configuration common to all calls
  public static String JMX_PORT = "jmxPort";
  public static String AUTHENTICATE = "authenticate";
  public static String USERNAME = "username";
  public static String PASSWORD = "password";
  public static String OBJECT_NAME = "objectName";
  public static String TYPES = "dptypes";

  
  // configuration...
  boolean _authenticate;
  String _username;
  String _password;
  String _objectName;

  // time when the call started (in ms since the epoch)
  long _startTime;

  // summary bean used for passing results to the Processor
  Summary _summary;
  
  // client to the agent
  JmxClient _client;

  
  /**
   * Creates JmxCall
   * @param url the jmx agent to connect to
   * @param username credential information for authenticated calls
   * @param password credential inforamtion for authenticated calls
   * @param objectName the mbean object name hosting the attribute to query
   * @param attrName the name of the attribute (can be a multi-value attribute)
   * @param attrKey the key of the attribute to return (only valid for
   * multi-value attributes)
   */
  public JmxCall(String url,
                 boolean authenticate,
                 String username,
                 String password,
                 String objectName) {
    _client = new JmxClient(url);
    _summary = new Summary();
    _summary.setObjectName(objectName);
  
    _authenticate = authenticate;
    _username = username;
    _password = password;

    _objectName = objectName;
  }


  /**
   * Sets the device id
   * @param id the id of the device that we're calling
   */
  public void setDeviceId(String id) {
    _summary.setDeviceId(id);
  }


  /**
   * Sets the datasource id
   * @param id the data source that the call is associated with
   */
  public void setDataSourceId(String id) {
    _summary.setDataSourceId(id);
  }


  /**
   * Sets the type map.  The type map is used to map the data point
   * name to the rrd type the point is stored in.
   */
  public void setTypeMap(Map<String, String> map) {
    _summary.setTypeMap(map);
  }


  /**
   * Builds the Map<String, String> that maps data point names to
   * their rrd types
   */
  Map<String, String> buildTypeMap(List<String> keys, List<String> types) {
    Map<String, String> typeMap = new HashMap<String, String>();

    Iterator<String> typeIter = types.iterator();
    for (String dpName : keys) {
      String type = "";

      if (typeIter.hasNext()) {
        type = typeIter.next();
      } 

      typeMap.put(dpName, type);
    }

    return typeMap;
  }


  /**
   * Optionally authenticates, throwing an Exception if authentication
   * fails
   * @throws IOException if authentication is unsuccessful
   */
  void setCredentials() {
    // set credential information 
    if (_authenticate) {
      String[] creds = new String[] { _username, _password };
      _client.setCredentials(creds);
    }
  }



  /**
   * Overrides the hashCode() definition in java.lang.Object.  In the
   * situation where a JMX Call is taking a ridiculously long amount
   * of time to complete and the user has specified a ridiculously
   * long timeout period we run into the situation where the bad JMX
   * Agent prevents the queries to good JMX Agents.  To work around
   * this situation the Reactor maintains a list of "in-flight"
   * requests, and the dispatch() method of Reactor does not re-issue
   * the same request to a bad JMX Agent.  The hashCode() method is
   * overridden so that we can compute the same hash code when the
   * same configuration is provided.
   * @see Object#hashCode
   */
  public int hashCode() {
    int hc = 0;

    hc += hashCode(_username);
    hc += hashCode(_password);
    hc += hashCode(_objectName);
    hc += hashCode(_summary.getDataSourceId());
    hc += hashCode(_summary.getDeviceId());

    return hc;
  }


  /**
   * Returns the hashcode from the variable provided in a safe way
   */
  int hashCode(Object variable) {
    if (variable != null) {
      return variable.hashCode();
    } else {
      return 0;
    }
  }

}
