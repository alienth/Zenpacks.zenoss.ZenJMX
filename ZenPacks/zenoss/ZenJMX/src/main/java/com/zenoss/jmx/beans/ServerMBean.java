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
package com.zenoss.jmx.beans;

import java.util.Map;
import java.util.List;


/**
 * <p>
 * MBean for managing my server.
 * </p>
 *
 * <p>$Author: chris $<br>
 *
 * @author Christopher Blunck
 * @version $Revision: 1.6 $
 */
public interface ServerMBean {

  /**
   * Returns the amount of time (in seconds) that the server has been
   * running.
   */
  public long calcUptime();


  /**
   * Returns a Map<String, Object> of pool sizes the application has
   * established.
   */
  public Map calcPoolSizes();


  /**
   * Returns the user, system, and kernel cpu states as Floats
   */
  public List calcCpuStates();


  /**
   * Returns user memory and cache memory in an Object[] of Floats
   */
  public Object[] calcMemoryStates();

}
