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
import java.util.ArrayList;
import java.util.List;

import static com.zenoss.zenpacks.zenjmx.call.JmxCall.*;
import static com.zenoss.zenpacks.zenjmx.call.SingleValueAttributeCall.*;
import static com.zenoss.zenpacks.zenjmx.call.CallFactory.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * <p>
 * Call for a multi-value attribute
 * </p>
 *
 * <p>$Author: chris $</p>
 *
 * @author Christopher Blunck
 * @version $Revision: 1.6 $
 */
public class MultiValueAttributeCall
  extends JmxCall {

  // configuration parameters
  public static final String ATTRIBUTE_KEY = "attributeKey";


  // the name of the attribute to query
  private String _attrName;

  // the keys of the attributes we should query
  private List<String> _keys;

  // logger
  private static final Log _logger = 
    LogFactory.getLog(MultiValueAttributeCall.class);


  /**
   * Creates a MultiValueAttributeCall
   */
  public MultiValueAttributeCall(String url,
                                 boolean authenticate,
                                 String username,
                                 String password,
                                 String objectName,
                                 String attrName,
                                 List<String> keys,
                                 List<String> types) {
    super(url, authenticate, username, password, objectName);

    _attrName = attrName;
    _keys = keys;
    
    setTypeMap(buildTypeMap(keys, types));

    _summary.setCallSummary("multi-value attribute: " + attrName + 
                            " (" + keys + ")");
  }


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
    Map<String, Object> values = _client.query(_objectName, _attrName, _keys);
    
    // disconnect from the agent.
    _client.close();
    
    _summary.setResults(values);
    
    // record the runtime of the call
    _summary.setRuntime(System.currentTimeMillis() - _startTime);

    // set our id so the processor can remove it from the reactor
    _summary.setCallId(hashCode());

    // return result
    return _summary;
  }


  /**
   * Creates a MultiValueAttributeCall from the configuration provided
   */
  public static MultiValueAttributeCall fromValue(Map config) {
    String url = Utility.getUrl(config);
    boolean auth = false;
    if (config.containsKey(AUTHENTICATE)) {
       auth = ((Boolean)config.get(AUTHENTICATE)).booleanValue();
    }

    // ugly form of downcasting...  but XML-RPC doesn't give us a List<String>
    List<String> keys = Utility.downcast((Object[]) config.get(DATA_POINT));
    List<String> types = Utility.downcast((Object[]) config.get(TYPES));

    MultiValueAttributeCall call = 
      new MultiValueAttributeCall(url,
                                  auth,
                                  Utility.getUsername(config),
                                  Utility.getPassword(config),
                                  (String) config.get(OBJECT_NAME),
                                  (String) config.get(ATTRIBUTE_NAME),
                                  keys,
                                  types);

    return call;
  }


  /**
   * @see JmxCall#hashCode
   */
  public int hashCode() {
    int hc = 0;

    hc += super.hashCode();
    hc += hashCode(_attrName);
    hc += hashCode(_keys);

    return hc;
  }
  
}
