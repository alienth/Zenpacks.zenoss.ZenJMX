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

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.zenoss.zenpacks.zenjmx.call.CallFactory;
import com.zenoss.zenpacks.zenjmx.call.JmxCall;


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

  // logger
  private static final Log _logger = LogFactory.getLog(Injector.class);


  /**
   * Creates an Injector
   */
  public Injector(boolean cycle) {
    _reactor = Reactor.instance();
    _cycle = cycle;
  }


  /**
   * @see Runnable#run
   */
  public void run() {
    Set<JmxCall> calls = new HashSet<JmxCall>();
    Set<Integer> callIds = new HashSet<Integer>();
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
        _logger.debug("calls size: " + calls.size());
        if (! callIds.contains(call.hashCode())) {
          calls.add(call);
          callIds.add(call.hashCode());
        }
      }

      // inject the calls
      _reactor.dispatch(calls);

      // optionally stop the reactor
      if (! _cycle) {
        _reactor.stop();
      }
    } catch (Throwable e) {
      _logger.error("unexpected error occurred during injection", e);
    }
  }
}
