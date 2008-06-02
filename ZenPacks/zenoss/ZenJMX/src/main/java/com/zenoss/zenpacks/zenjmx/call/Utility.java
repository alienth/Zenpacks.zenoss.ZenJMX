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

import java.util.Map;
import java.util.List;
import java.util.ArrayList;


/**
 * <p></p>
 *
 * <p>$Author: chris $</p>
 *
 * @author Christopher Blunck
 * @version $Revision: 1.6 $
 */
public class Utility {

  /**
   * Looks in the config Map provided and extracts the JMX property
   * requested.  If it does not exist the method looks for the
   * zProperty in the Map.  If it can't find that it returns ""
   */
  public static String get(Map config, String jProperty, String zProperty) {
    String toReturn = "";
    
    if (config.containsKey(jProperty)) {
      toReturn = (String) config.get(jProperty);
    } 

    if ("".equals(toReturn.trim()) && config.containsKey(zProperty)) {
      toReturn = config.get(zProperty).toString();
    }

    return toReturn;
  }


  /**
   * Creates a URL based on the configuration provided.
   */
  public static String getUrl(Map config) {
    String port = get(config, "jmxPort", "zJmxManagementPort");
    String hostAddr = null;
    
    if (config.containsKey("manageIp"))
       hostAddr = (String)config.get("manageIp");
    // Support pre-2.1.3+ hub xml-rpc responses. If manageIp is not present
    // in configuration, fallback to device, which will have a better chance
    // of working than null.
    else hostAddr = (String)config.get("device"); 
    
    String url = 
      "service:jmx:rmi:///jndi/rmi://" + hostAddr + ":" + port + "/jmxrmi";
    
    return url;
  }


  /**
   * Returns the JMX username from the configuration provided
   */
  public static String getUsername(Map config) {
    return get(config, "username", "zJmxManagementUsername");
  }


  /**
   * Returns the JMX password from the configuration provided
   */
  public static String getPassword(Map config) {
    return get(config, "password", "zJmxManagementPassword");
  }


  /**
   * Downcasts the Object[] to a List<String>
   * @param source the Objects to downcast to their String representation
   * @return a List<String> that is the result of calling toString()
   * on each Object in the source
   */
  public static List<String> downcast(Object[] source) {
    List<String> dest = new ArrayList<String>();
    if (source != null) {
      for (Object obj : source) {
        dest.add(obj.toString());
      }
    }

    return dest;
  }
}
