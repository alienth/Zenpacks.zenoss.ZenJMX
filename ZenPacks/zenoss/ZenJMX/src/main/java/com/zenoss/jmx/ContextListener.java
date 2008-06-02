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
package com.zenoss.jmx;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import com.zenoss.jmx.beans.Server;

/**
 * <p> ServletContextListener that relies on the deployment callback
 * mechanism to deploy and activate an MBean. </p>
 *
 * <p>$Author: chris $</p>
 *
 * @author Christopher Blunck
 * @version $Revision: 1.6 $
 */
public final class ContextListener
  implements ServletContextListener {

  /**
   * Called when the application context is created.  This corresponds
   * to when an application is deployed in a container.
   */
  public void contextInitialized(ServletContextEvent event) {
    Server mbean = new Server();
  }


  /**
   * Called when the application context is destroyed.
   */
  public void contextDestroyed(ServletContextEvent event) { }


}
