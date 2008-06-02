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

import java.util.Map;
import java.util.List;
import java.util.Arrays;

import static com.zenoss.zenpacks.zenjmx.call.SingleValueAttributeCall.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * <p> Factory to produce JmxCalls based on the JMX QueryType value
 * and the rest of the configuration. </p>
 *
 * <p>$Author: chris $</p>
 *
 * @author Christopher Blunck
 * @version $Revision: 1.6 $
 */
public class CallFactory {

  // configuration items (keys) in the config returned from the server
  public static final String DATA_POINT = "dps";
  public static final String DEVICE_ID = "device";
  public static final String DATASOURCE_ID = "id";

  // logger
  private static final Log _logger = LogFactory.getLog(CallFactory.class);


  /**
   * Private constructor enforces non-instantiability
   */
  private CallFactory() { }


  /**
   * Creates a JmxCall from a configuration read from the server
   * @param config the name-value parameters from the server
   */
  public static JmxCall createCall(Map config) {
    _logger.debug("config: " + config);

    Object[] datapoints = (Object[]) config.get(DATA_POINT);
    if (datapoints == null) {
      _logger.warn("no data points defined");
      return null;
    }

    // the id of the device we will query
    String deviceId = (String) config.get(DEVICE_ID);

    // the id of the data source
    String dataSourceId = (String) config.get(DATASOURCE_ID);

    // if the attributeName is blank the configuration represents an operation
    String attributeName = (String) config.get(ATTRIBUTE_NAME);
    if ("".equals(attributeName)) {
      _logger.debug("creating an operation call");
      JmxCall call = OperationCall.fromValue(config);
      call.setDeviceId(deviceId);
      call.setDataSourceId(dataSourceId);
      return call;
    }

    if (datapoints.length > 1) {
      _logger.debug("creating a multi-value attribute call");
      JmxCall call = MultiValueAttributeCall.fromValue(config);
      call.setDeviceId(deviceId);
      call.setDataSourceId(dataSourceId);
      return call;
    } else {
      _logger.debug("creating a single value attribute call");
      JmxCall call = SingleValueAttributeCall.fromValue(config);
      call.setDeviceId(deviceId);
      call.setDataSourceId(dataSourceId);
      return call;
    }
  }
}

