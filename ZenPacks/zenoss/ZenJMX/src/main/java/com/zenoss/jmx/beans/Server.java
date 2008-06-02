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

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

/**
 * <p>
 * Implements the ServerMBean interface.
 * </p>
 *
 * <p>$Author: chris $<br>
 *
 * @author Christopher Blunck
 * @version $Revision: 1.6 $
 */
public class Server
  implements ServerMBean {

  // set to seconds since the epoch when the bean is deployed
  private long _startTime = 0L;

  /**
   * Constructs a Server.  Must support bean style constructors (no
   * parameters)
   */
  public Server() {
    MBeanServer server = getServer();
    
    ObjectName name = null;
    try {
      name = new ObjectName("Application:Name=Server,Type=Server");
      server.registerMBean(this, name);
    } catch (Exception e) {
      e.printStackTrace();
    }

    _startTime = System.currentTimeMillis();
  }


  /**
   * Returns the MBeanServer.  If one doesn't already exists it
   * creates one.  If one already exists it returns the one that
   * already exists.
   * @return MBeanServer a server in which MBeans can be registered
   */
  private MBeanServer getServer() {
    MBeanServer server = null;

    // attempt to locate MBean servers
    List servers = MBeanServerFactory.findMBeanServer(null);
    if (servers.size() > 0) {
      server = (MBeanServer) servers.get(0);
    }

    if (server != null) {
      // use the server that was found
      System.out.print("Found our MBean server.");
    } else {
      // create a new mbean server
      System.out.print("No MBean server found.  Creating a new one.");
      server = MBeanServerFactory.createMBeanServer();
    }

    return server;
  }


  /**
   * @see ServerMBean#calcUptime
   */
  public long calcUptime() { 
    System.out.println("calcUptime() called.  sleeping for awhile...");
    try {
      Thread.sleep(0);
    } catch (InterruptedException e) { }

    System.out.println("sleep complete.  returning uptime.");

    return System.currentTimeMillis() - _startTime; 
  }


  /**
   * @see ServerMBean#calcPoolSizes
   */
  public Map calcPoolSizes() {
    Map toReturn = new HashMap();

    toReturn.put("pool1", 34.55);
    toReturn.put("pool2", 53.22);
    
    return toReturn;
  }


  /**
   * @see ServerMBean#calcCpuStates
   */
  public List calcCpuStates() {
    List toReturn = new ArrayList();

    toReturn.add(0.22);
    toReturn.add(0.34);
    toReturn.add(0.44);

    return toReturn;
  }


  /**
   * @see ServerMBean#calcMemoryStates
   */
  public Object[] calcMemoryStates() {
    Object[] toReturn = new Object[] {
      128000.00, 256000.00
    };

    return toReturn;
  }

}
