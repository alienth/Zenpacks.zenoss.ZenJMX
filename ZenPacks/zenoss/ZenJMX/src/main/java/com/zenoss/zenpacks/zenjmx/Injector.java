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
package com.zenoss.zenpacks.zenjmx;

import com.zenoss.zenpacks.zenjmx.call.JmxCall;
import com.zenoss.zenpacks.zenjmx.call.CallFactory;

import java.util.Map;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.ArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * <p>
 * Injects a JmxCall into the Reactor for servicing.
 * </p>
 *
 * <p>$Author: chris $</p>
 *
 * @author Christopher Blunck
 * @version $Revision: 1.6 $
 */
public class Injector
  implements Runnable {

  // reference to the Reactor
  private Reactor _reactor;

  // set to true to indicate we should NOT shut down the reactor after this run
  private boolean _cycle;

  // the JmxCalls we already know about
  private Set<JmxCall> _calls;
  
  // the hashcodes of the calls we know about
  private Set<Integer> _callIds;

  // logger
  private static final Log _logger = LogFactory.getLog(Injector.class);


  /**
   * Creates an Injector
   */
  public Injector(boolean cycle) {
    _reactor = Reactor.instance();
    _cycle = cycle;
    _calls = new HashSet<JmxCall>();
    _callIds = new HashSet<Integer>();
  }


  /**
   * @see Runnable#run
   */
  public void run() {
    try {
      // get configs from the reactor
      List<Map> configs = _reactor.getConfigs();
      if (configs == null) {
        _logger.warn("data source configurations not loaded yet...");
        return;
      }
    
      // assemble the calls
      for (Map config : configs) {
        JmxCall call = CallFactory.createCall(config);
        _logger.debug("calls size: " + _calls.size());
        if (! _callIds.contains(call.hashCode())) {
          _calls.add(call);
          _callIds.add(call.hashCode());
        }
      }

      // inject the calls
      _reactor.dispatch(_calls);

      // optionally stop the reactor
      if (! _cycle) {
        _reactor.stop();
      }
    } catch (Throwable e) {
      _logger.error("unexpected error occurred during injection", e);
    }
  }
}
