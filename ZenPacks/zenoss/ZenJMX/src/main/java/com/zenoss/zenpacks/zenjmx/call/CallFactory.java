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

import com.zenoss.zenpacks.zenjmx.ConfigAdapter;


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
  public static final String DATASOURCE_ID = "datasourceId";

  // logger
  private static final Log _logger = LogFactory.getLog(CallFactory.class);


  /**
   * Private constructor enforces non-instantiability
   */
  private CallFactory() { }

  /**
   * Creates a JmxCall from a configuration read from the server
   * @param config the name-value parameters from the server
   * @throws ConfigurationException if the configuration provided does not
   *         contain sufficient information to create a call.
   */
  public static JmxCall createCall(ConfigAdapter config) 
      throws ConfigurationException {
      
      _logger.debug("config: " + config);

      List<String>dataPoints = config.getDataPoints();
      if (dataPoints.isEmpty()) {
          _logger.warn("no data points defined");
          throw new ConfigurationException("No datapoints defined; " +
                          "will not run collections");
      }

      // the id of the device we will query
      String deviceId = config.getDevice();

      // the id of the data source
      String dataSourceId = config.getDatasourceId();

      // if the attributeName is blank the configuration represents an operation
      String attributeName =  config.getAttributeName();
      JmxCall call = null;
      if (attributeName.trim().length() == 0) {
          _logger.debug("creating an operation call");
          call = OperationCall.fromValue(config);
      }else if (dataPoints.size() > 1) {
          _logger.debug("creating a multi-value attribute call");
          call = MultiValueAttributeCall.fromValue(config);
      } else {
          _logger.debug("creating a single value attribute call");
          call = SingleValueAttributeCall.fromValue(config);
          }
      return call;
  }
}

