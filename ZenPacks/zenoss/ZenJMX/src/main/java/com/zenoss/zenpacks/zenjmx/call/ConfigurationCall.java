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

import java.util.List;
import java.util.Map;

import com.zenoss.zenpacks.zenjmx.Reactor;

import com.zenoss.xmlrpc.XmlRpcClient;

import org.apache.xmlrpc.XmlRpcException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * <p></p>
 *
 * <p>$Author: chris $</p>
 *
 * @author Christopher Blunck
 * @version $Revision: 1.6 $
 */
public class ConfigurationCall
  implements Runnable {

  // client to zenhub
  private XmlRpcClient _client;

  // logger
  private static final Log _logger = LogFactory.getLog(ConfigurationCall.class);


  /**
   * Creates a ConfigurationCall
   */
  public ConfigurationCall(XmlRpcClient client) { 
    _client = client;
  }


  /**
   * Retrieves the JMX data source configurations and updates the
   * reactor configuration.
   */
  public void run() {
    try {
      _logger.info("retrieving JMX data source configurations...");
      List<Map> configs = _client.getConfigs("JMX");
      _logger.info("retrieved " + configs.size() + " configs.");

      Reactor reactor = Reactor.instance();
      reactor.updateConfigs(configs);
    } catch (XmlRpcException e) {
      _logger.error("error retrieving data source configs.  " +
                    "XML-RPC error code: " + e.code, e);
    }
  }
}
