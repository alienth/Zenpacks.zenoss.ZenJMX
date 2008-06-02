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

import org.apache.commons.cli.Options;
import org.apache.commons.cli.Option;


/**
 * <p> Factory for creating Options.  The command line option
 * information used to be included in the Main class but it was
 * getting too big and bulky so I moved it into a factory.  </p>
 *
 * <p>$Author: chris $</p>
 *
 * @author Christopher Blunck
 * @version $Revision: 1.6 $
 */
public class OptionsFactory {
  // configuration options
  public static final String HUB_URL = "hubUrl";
  public static final String HUB_USERNAME = "hubUser";
  public static final String HUB_PASSWORD = "hubPass";
  public static final String CONF_NAME = "confName";
  public static final String COMPONENT_NAME = "componentName";
  public static final String CONFIG_FILE = "configfile";
  public static final String CYCLE = "cycle";
  public static final String CYCLE_TIME = "cycleTime";
  public static final String JMX_POOL_SIZE = "jmxPoolSize";
  public static final String JMX_TIMEOUT = "jmxTimeOut";
  public static final String HELP = "help";

  // default values (also set in zenjmx.conf)
  public static final String DEFAULT_HUB_URL = "http://localhost:8081/";
  public static final String DEFAULT_HUB_USERNAME = "admin";
  public static final String DEFAULT_HUB_PASSWORD = "zenoss";
  public static final String DEFAULT_CONF_NAME = "localhost";
  public static final String DEFAULT_COMPONENT_NAME = "zenjmx";
  public static final String DEFAULT_CYCLE = "False";
  public static final String DEFAULT_CYCLE_TIME = "300";
  public static final String DEFAULT_JMX_POOL_SIZE = "10";
  public static final String DEFAULT_JMX_TIMEOUT = "30";


  // singleton instance
  private static OptionsFactory _instance;

  
  /**
   * Private constructor to enforce singleton pattern
   */
  private OptionsFactory() { }


  /**
   * Creates an Option (which is by definition ... not required)
   * @param name the short name of the argument
   * @param hasValue set to true to indicate the option has a value
   * associated with it.  set to value if the option does not have a
   * value (e.g. --cycle or --help)
   * @param desc a description of the option
   */
  private Option createOption(String name, boolean hasValue, String desc) {
    Option option = new Option(name, hasValue, desc);
    return option;
  }


  /**
   * Creates an argument.  An argument is required (not optional).
   * @param name the short name of the argument
   * @param hasValue set to true to indicate the option has a value
   * associated with it.  set to value if the option does not have a
   * value (e.g. --cycle or --help)
   * @param desc a description of the option
   */
  private Option createArgument(String name, boolean hasValue, String desc) {
    Option option = createOption(name, hasValue, desc);
    option.setRequired(true);

    return option;
  }


  /**
   * Creates command line options
   */
  public Options createOptions() {
    Options o = new Options();

    // everything is treated as an optional argument
    o.addOption(createOption(CONFIG_FILE, true,  "configuration file"));
    o.addOption(createOption(CYCLE, false, "set to true to loop"));
    o.addOption(createOption(CYCLE_TIME, true, 
                             "time (in secs) to wait between cycles"));
    o.addOption(createOption(JMX_POOL_SIZE, true, 
                             "number of concurrent requests to make to " +
                             "JMX agents"));
    o.addOption(createOption(JMX_TIMEOUT, true, 
                             "number of seconds to wait for an individual " +
                             "JMX response before timing out"));
    o.addOption(createOption(HUB_URL, true, "url to zenhub"));
    o.addOption(createOption(HUB_USERNAME, true, "username for the hub"));
    o.addOption(createOption(HUB_PASSWORD, true, "password for the hub"));
    o.addOption(createOption(CONF_NAME, true, 
                             "name of this performance collector"));
    o.addOption(createOption(COMPONENT_NAME, true, 
                             "name of the component to heartbeat against"));
    
    return o;
  }


  /**
   * Singleton accessor method
   */
  public static OptionsFactory instance() {
    if (_instance == null) {
      _instance = new OptionsFactory();
    }

    return _instance;
  }
}
