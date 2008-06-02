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

import java.util.concurrent.CountDownLatch;

import java.net.InetAddress;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.zenoss.zenpacks.zenjmx.Configuration;
import com.zenoss.zenpacks.zenjmx.Reactor;

import static com.zenoss.zenpacks.zenjmx.OptionsFactory.*;


/**
 * <p></p>
 *
 * <p>$Author: chris $</p>
 *
 * @author Christopher Blunck
 * @version $Revision: 1.6 $
 */
public class CountDownLatchObserver
  implements Runnable {

  // the latch to observe (invoke .await() on)
  private CountDownLatch _latch;

  // logger
  private static final Log _logger = 
    LogFactory.getLog(CountDownLatchObserver.class);


  /**
   * Creates an observer that focuses on the latch provided
   */
  public CountDownLatchObserver() { }


  /**
   * Sets the latch to observe
   */
  public void setLatchToObserve(CountDownLatch latch) { 
    _latch = latch;
  }


  /**
   * @see Runnable#run Invokes await() on the latch and waits for it
   * to be unlocked.  When it is unlocked it does something.
   */
  public void run() {
    try {
      // block until the latch is opened
      _latch.await();

      // send a heartbeat via the reactor
      Reactor reactor = Reactor.instance();
      reactor.sendHeartbeat();
    } catch (Throwable e) {
      _logger.error("unexpected error while observing the latch", e);
    }
  }
}
