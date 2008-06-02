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

import java.util.Random;

import java.util.concurrent.Callable;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * <p> Callable used for testing.  Sleeps a pre-determined amount of
 * time during the call() invocation and returns a random number.
 * </p>
 *
 * <p>$Author: chris $<br>
 * $Date: 2005/03/13 18:45:25 $</p>
 *
 * @author Christopher Blunck
 * @version $Revision: 1.6 $
 */
public class DelayedCall
  implements Callable {

  // how long we should wait in run() before returning
  private int _delay;

  // random number generator used to produce return values from call()
  private Random _random;

  // configuration...
  private String _hostname;

  // logger
  private static final Log _logger = LogFactory.getLog(DelayedCall.class);


  /**
   * Creates a DelayedCallalbe with the delay provided
   * @param delay the amount of time (in ms) this instance should wait
   * before returning
   */
  public DelayedCall(int delay, String hostname) {
    _delay = delay;
    _random = new Random();
    _hostname = hostname;
  }


  /**
   * @see Callable#call
   */
  public String call() 
    throws IOException {

    try {
      Thread.sleep(_delay);
    } catch (InterruptedException e) {
      _logger.info("(" + Thread.currentThread().getName() + "): " +
                         "Thread interrupted...");
    }

    return _hostname + ": " + _random.nextInt();
  }
}
