///////////////////////////////////////////////////////////////////////////
//
// This program is part of Zenoss Core, an open source monitoring platform.
// Copyright (C) 2008, Zenoss Inc.
//
// This program is free software; you can redistribute it and/or modify it
// under the terms of the GNU General Public License version 2 as published by
// the Free Software Foundation.
//
// For complete information please visit: http://www.zenoss.com/oss/
//
///////////////////////////////////////////////////////////////////////////
package com.zenoss.zenpacks.zenjmx;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;


/**
 * <p></p>
 *
 * <p>$Author: chris $</p>
 *
 * @author Christopher Blunck
 * @version $Revision: 1.6 $
 */
public class Configuration {

  // singleton instance
  private static Configuration _instance;

  // the actual configuration items reside in this properties object
  private Properties _properties;


  /**
   * Private constructor to enforce non-instantiability
   */
  private Configuration() {
    _properties = new Properties();
  }


  /**
   * Sets a property
   */
  public void setProperty(String name, String value) {
    _properties.setProperty(name, value);
  }


  /**
   * Returns the value of the property requested
   */
  public String getProperty(String name) {
    return _properties.getProperty(name);
  }


  /**
   * Returns the value of the property requested, or the defaultValue
   * of the property with the name provided does not exist.
   */
  public String getProperty(String name, String defaultValue) {
    return _properties.getProperty(name, defaultValue);
  }


  /**
   * Loads properties from the InputStream provided
   */
  public void load(InputStream stream) 
    throws IOException {
    _properties.load(stream);
  }


  /**
   * Returns true if the property with the name provided is defined
   */
  public boolean propertyExists(String name) {
    return _properties.containsKey(name);
  }


  /**
   * Singleton accessor method
   */
  public static synchronized Configuration instance() {
    if (_instance == null) {
      _instance = new Configuration();
    }

    return _instance;
  }


  /**
   * Returns a string representation
   */
  public String toString() {
    return _properties.toString();
  }
}
