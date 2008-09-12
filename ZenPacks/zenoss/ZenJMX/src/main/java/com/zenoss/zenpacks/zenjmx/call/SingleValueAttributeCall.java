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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.zenoss.jmx.JmxClient;
import com.zenoss.jmx.JmxException;
import com.zenoss.zenpacks.zenjmx.ConfigAdapter;


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

  private String _dataPointName;

  /**
   * Creates a SingleAttributeCall
   */
  public SingleValueAttributeCall(String objectName,
                                  String attrName,
                                  String dataPointName,
                                  String attrType) {
    super(objectName);

    _attrName = attrName;

    Map<String, String> typeMap = new HashMap<String, String>();
    typeMap.put(attrName, attrType);
    setTypeMap(typeMap);
    _dataPointName = dataPointName;
    _summary.setCallSummary("single-value attribute: " + attrName);
  }


  /**
   * Returns the name of the attribute this call will retrieve.
   */
  public String getAttributeName() { return _attrName; }

  
  /**
   * @throws JmxException 
 * @see Callable#call
   */
  public Summary call(JmxClient client) throws JmxException {
  // record when we started
      _startTime = System.currentTimeMillis();
      // issue the query
      Map<String, Object> values = client.query(_objectName, _attrName, 
              Collections.singletonList(_dataPointName));
  
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
  public static SingleValueAttributeCall fromValue(ConfigAdapter config) 
    throws ConfigurationException {

    
    List<String> types = config.getDataPointTypes();
    String type = "";
    if (types.size() > 0) {
      type = types.iterator().next();
    }
    String dpName = config.getDataPoints().iterator().next();

    SingleValueAttributeCall call = 
      new SingleValueAttributeCall(config.getOjectName(),
                                   config.getAttributeName(),
                                   dpName,
                                   type);
    call.setDeviceId(config.getDevice());
    call.setDataSourceId(config.getDatasourceId());
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
