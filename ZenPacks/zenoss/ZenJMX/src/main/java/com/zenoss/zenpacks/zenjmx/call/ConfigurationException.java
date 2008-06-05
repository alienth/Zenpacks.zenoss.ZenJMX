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


/**
 * <p> Represents a problem that occurred as a result of
 * configuration.  </p>
 *
 * <p>$Author: chris $</p>
 *
 * @author Christopher Blunck
 * @version $Revision: 1.6 $
 */
public class ConfigurationException 
  extends Exception {

  /**
   * Creates a ConfigurationException based on a message and an
   * exception that caused the ConfigurationException.
   */
  public ConfigurationException(String message, Throwable t) {
    super(message, t);
  }


  /**
   * Creates a ConfigurationException given a message
   */
  public ConfigurationException(String message) {
    super(message);
  }
}
