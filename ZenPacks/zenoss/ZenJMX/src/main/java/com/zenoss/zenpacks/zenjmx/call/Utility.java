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
package com.zenoss.zenpacks.zenjmx.call;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.zenoss.zenpacks.zenjmx.ConfigAdapter;


/**
 * <p> Commonly used methods.  </p>
 *
 * <p>$Author: chris $</p>
 *
 * @author Christopher Blunck
 * @version $Revision: 1.6 $
 */
public class Utility {

    
  private static final Log _logger = LogFactory.getLog(Utility.class);



  /**
   * Creates a URL based on the configuration provided.
   * @returns a jmx connection url
   * @throws ConfigurationException if the url is invalid (if the hostname 
   *         or port cannot be found in the configuration)
   */
  public static String getUrl(ConfigAdapter config) 
    throws ConfigurationException {

    String jmxRawService = config.getJmxRawService();
    String url = null;

    // allow users to specify a raw JMX url
    if (jmxRawService != null && ! jmxRawService.equals("")) {
        _logger.debug("using JMX Raw URL option");
        url = jmxRawService;
    }
    // old fashioned JMX url building
    else {
        String port = config.getJmxPort();
        String rmiContext = config.getRmiContext();

        if(rmiContext == null || "".equals(rmiContext)){
            rmiContext = "jmxrmi";  //default context for mbean server
        }
    
        String protocol = config.getJmxProtocol();
        if ("".equals(port)) {
            String message = " jmxPort or zJmxManagementPort not specified";
            throw new ConfigurationException("Datasource " + 
                config.getDatasourceId() + message);
        }

        String hostAddr = config.getManageIp();
     
        if ((hostAddr == null) || ("".equals(hostAddr.trim()))) {
            String message = " manageIp or device properties not specified";
            throw new ConfigurationException("Datasource " + 
                config.getDatasourceId() + message);
        }
    
        url = "service:jmx:";
        if (protocol.equals("JMXMP"))
            url += "jmxmp://" + hostAddr + ":" + port;
        else
            url += "rmi:///jndi/rmi://" + hostAddr + ":" + port + "/"+ rmiContext;
    }

    _logger.debug("JMX URL is: "+url);
    return url;
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


  /**
   * Returns true if the two objects are both null or are equal to
   * each other
   */
  public static boolean equals(Object obj1, Object obj2) {
    if ((obj1 == null) && (obj2 == null)) {
      return true;
    }

    if ((obj1 == null) && (obj2 != null)) {
      return false;
    }

    if ((obj1 != null) && (obj2 == null)) {
      return false;
    }

    return obj1.equals(obj2);
  }


  /**
   * Returns true the object arrays are both null or are equal to each
   * other
   */
  public static boolean equals(Object[] obj1, Object[] obj2) {
    if ((obj1 == null) && (obj2 == null)) {
      return true;
    }

    if ((obj1 == null) && (obj2 != null)) {
      return false;
    }

    if ((obj1 != null) && (obj2 == null)) {
      return false;
    }

    if (obj1.length != obj2.length) {
      return false;
    }
    
    boolean toReturn = true;
    for (int i = 0; i < obj1.length; i++) {
      toReturn &= equals(obj1[i], obj2[i]);
    }

    return toReturn;
  }

}
