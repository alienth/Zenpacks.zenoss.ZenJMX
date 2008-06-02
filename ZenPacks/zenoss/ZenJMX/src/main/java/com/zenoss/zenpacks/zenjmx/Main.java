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

import com.zenoss.xmlrpc.XmlRpcClient;

import com.zenoss.zenpacks.zenjmx.call.JmxCall;
import com.zenoss.zenpacks.zenjmx.call.CallFactory;

import java.util.List;
import java.util.Map;
import java.util.Iterator;

import java.io.FileInputStream;
import java.io.IOException;

import java.net.MalformedURLException;

import org.apache.commons.cli.Options;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.cli.ParseException;

import org.apache.xmlrpc.XmlRpcException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import static com.zenoss.zenpacks.zenjmx.OptionsFactory.*;


/**
 * <p>
 * Entry point into the application
 * </p>
 *
 * <p>$Author: chris $</p>
 *
 * @author Christopher Blunck
 * @version $Revision: 1.6 $
 */
public class Main {
  // raw uncut and unparsed command line arguments
  private String[] _args;

  // unusual command line arguments (ones without values)
  private boolean _cycle;

  // the reactor that manages all the events
  private Reactor _reactor;

  // command line options
  private Options _options;

  // configuration directives
  private Configuration _config;

  // logger
  private static final Log _logger = LogFactory.getLog(Main.class);

  
  /**
   * Creates a Main
   * @param args the command line arguments
   */
  public Main(String[] args) {
    _args = args;

    OptionsFactory factory = OptionsFactory.instance();
    _options = factory.createOptions();

    _config = Configuration.instance();
  }


  /**
   * Checks the CommandLine for the option with the name provided.  If
   * present it sets the name and value pair in the _config field
   * using "true" as the value of the option.
   */
  private void overrideOption(CommandLine cmd, String option) {
    if (cmd.hasOption(option)) {
      _config.setProperty(option, "true");
    }
  }


  /**
   * Checks the CommandLine for the property with the name provided.
   * If present it sets the name and value pair in the _config
   * field.
   */
  private void overrideProperty(CommandLine cmd, String name) {
    if (cmd.hasOption(name)) {
      String value = cmd.getOptionValue(name);
      _config.setProperty(name, value);
    }
  }


  /**
   * Parses the command line arguments
   */
  private void parseArguments() 
    throws ParseException, NumberFormatException {

    // parse the command line
    CommandLineParser parser = new PosixParser();
    CommandLine cmd = parser.parse(_options, _args);

    // get the config file argument and load it into the properties
    if (cmd.hasOption(CONFIG_FILE)) {
      String filename = cmd.getOptionValue(CONFIG_FILE);
      try {
        _config.load(new FileInputStream(filename));
      } catch (IOException e) {
        _logger.error("failed to load configuration file", e);
      }
    } else {
      _logger.warn("no config file option (--" + CONFIG_FILE + ") specified");
      _logger.warn("only setting options based on command line arguments");
    }

    for (String arg : _args) {
      _logger.debug("arg: " + arg);
    }

    // interrogate the options and get the argument values
    overrideProperty(cmd, HUB_URL);
    overrideProperty(cmd, HUB_USERNAME);
    overrideProperty(cmd, HUB_PASSWORD);
    overrideProperty(cmd, CONF_NAME);
    overrideProperty(cmd, COMPONENT_NAME);
    overrideProperty(cmd, JMX_POOL_SIZE);
    overrideProperty(cmd, JMX_TIMEOUT);
    overrideProperty(cmd, CYCLE_TIME);
    overrideOption(cmd, CYCLE);

    // cycle is optional
    _cycle = _config.propertyExists(CYCLE);

    // tell the user about the arguments
    _logger.info("zenjmx configuration:");
    _logger.info(_config.toString());
  }


  /**
   * Displays the usage information
   */
  private void usage() {
    _logger.info("Usage: zenjmx [args] [options]");

    OptionsPrinter printer = new OptionsPrinter(_options);
    _logger.info(printer.toString());
  }


  /**
   * Starts up the reactor.  This needs to be called before any
   * configurations are injected.
   * @throws XmlRpcException if the performance collector
   * configuration cannot be loaded.
   */
  private void startReactor() 
    throws XmlRpcException, MalformedURLException {

    String hubUrl = _config.getProperty(HUB_URL, DEFAULT_HUB_URL);
    String confName = _config.getProperty(CONF_NAME, DEFAULT_CONF_NAME);
    String username = _config.getProperty(HUB_USERNAME, DEFAULT_HUB_USERNAME);
    String password = _config.getProperty(HUB_PASSWORD, DEFAULT_HUB_PASSWORD);
    String valueStr = _config.getProperty(CYCLE_TIME, DEFAULT_CYCLE_TIME);
    int cycleTime = Integer.valueOf(valueStr);

    valueStr = _config.getProperty(JMX_POOL_SIZE, DEFAULT_JMX_POOL_SIZE);
    int poolSize = Integer.valueOf(valueStr);
    
    valueStr = _config.getProperty(JMX_TIMEOUT, DEFAULT_JMX_TIMEOUT);
    int timeout = Integer.valueOf(valueStr);

    XmlRpcClient client = new XmlRpcClient(hubUrl, confName);
    client.setCredentials(username, password);

    _reactor = Reactor.instance(poolSize, timeout, cycleTime);
    _logger.debug("cycle: " + _cycle);
    _reactor.start(client, _cycle);
  }


  /**
   * Stops the reactor.  Note that the reactor may not stop
   * immediately if there are operations that are pending
   */
  private void stopReactor() {
    _reactor.stop();
  }


  /**
   * Returns true if we shoudl cycle
   */
  public boolean cycleEnabled() { return _cycle; }


  /**
   * Runs the application
   */
  public static void main(String[] args) {

    try {
      // construct
      Main main = new Main(args);

      // display usage when no arguments are provided
      if (args.length == 0) {
        main.usage();
        return;
      }

      // parse the arguments
      main.parseArguments();

      // start up the reactor
      main.startReactor();
    } catch (Throwable e) {
      _logger.error("unexpected error occurred", e);
    }
  }
}
