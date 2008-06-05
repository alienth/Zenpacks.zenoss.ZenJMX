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

import java.io.IOException;

import java.util.Map;
import java.util.List;
import java.util.HashMap;

import static com.zenoss.zenpacks.zenjmx.call.JmxCall.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * <p>
 * Call for a single-value attribute
 * </p>
 *
 * <p>$Author: chris $</p>
 *
 * @author Christopher Blunck
 * @version $Revision: 1.6 $
 */
public class SingleValueAttributeCall
  extends JmxCall {

  // configuration parameters
  public static final String ATTRIBUTE_NAME = "attributeName";


  // the name of the attribute to query
  private String _attrName;

  // logger
  private static final Log _logger = 
    LogFactory.getLog(SingleValueAttributeCall.class);


  /**
   * Creates a SingleAttributeCall
   */
  public SingleValueAttributeCall(String url,
                                  boolean authenticate,
                                  String username,
                                  String password,
                                  String objectName,
                                  String attrName,
                                  String attrType) {
    super(url, authenticate, username, password, objectName);

    _attrName = attrName;

    Map<String, String> typeMap = new HashMap<String, String>();
    typeMap.put(attrName, attrType);
    setTypeMap(typeMap);

    _summary.setCallSummary("single-value attribute: " + attrName);
  }


  /**
   * Returns the name of the attribute this call will retrieve.
   */
  public String getAttributeName() { return _attrName; }

  
  /**
   * @see Callable#call
   */
  public Summary call() 
    throws Exception {

    // record when we started
    _startTime = System.currentTimeMillis();

    setCredentials();
    
    // connect to the agent
    _client.connect();
    
    // issue the query
    Object result = _client.query(_objectName, _attrName);
    
    // disconnect from the agent.
    _client.close();
    
    // marshal the results
    Map<String, Object> values = new HashMap<String, Object>();
    values.put(_attrName, result);
    _summary.setResults(values);
    
    // record the runtime of the call
    _summary.setRuntime(System.currentTimeMillis() - _startTime);

    // set our id so the processor can remove it from the reactor
    _summary.setCallId(hashCode());

    // return result
    return _summary;
  }


  /**
   * Creates a SingleValueAttributeCall from the configuration provided
   */
  public static SingleValueAttributeCall fromValue(Map config) {
    String url = Utility.getUrl(config);
    boolean auth = false;
    if (config.containsKey(AUTHENTICATE)) {
       auth = ((Boolean)config.get(AUTHENTICATE)).booleanValue();
    }
    
    List<String> types = Utility.downcast((Object[]) config.get(TYPES));
    String type = "";
    if (types.size() > 0) {
      type = types.iterator().next();
    }

    SingleValueAttributeCall call = 
      new SingleValueAttributeCall(url,
                                   auth,
                                   Utility.getUsername(config),
                                   Utility.getPassword(config),
                                   (String) config.get(OBJECT_NAME),
                                   (String) config.get(ATTRIBUTE_NAME),
                                   type);
    return call;
  }


  /**
   * @see Object#equals
   */
  public boolean equals(Object other) {
    if (! (other instanceof SingleValueAttributeCall)) {
      return false;
    }

    boolean toReturn = super.equals(other);

    SingleValueAttributeCall call = (SingleValueAttributeCall) other;

    toReturn &= Utility.equals(call.getAttributeName(), getAttributeName());
    
    return toReturn;
  }
  

  /**
   * @see JmxCall#hashCode
   */
  public int hashCode() {
    int hc = 0;

    hc += super.hashCode();
    hc += hashCode(_attrName);

    return hc;
  }

}
