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


/**
 * <p> Tagging interfaces for all JmxExceptions. </p>
 *
 * <p>$Author: chris $<br>
 * $Date: 2007/07/30 18:45:25 $</p>
 *
 * @author Christopher Blunck
 * @version $Revision: 1.6 $
 */
public class JmxException
  extends Exception {

  /**
   * Creates a JmxException based on some lower level exception that
   * occurred.
   */
  public JmxException(Throwable t) { super(t); }


  /**
   * Creates a JmxException with a message
   */
  public JmxException(String message) { super(message); }


  /**
   * Creates a JmxException with a message and a Throwable
   */
  public JmxException(String message, Throwable t) {
    super(message, t);
  }
}
