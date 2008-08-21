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

import java.io.FileInputStream;
import java.io.IOException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xmlrpc.webserver.XmlRpcServlet;
import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.bio.SocketConnector;
import org.mortbay.jetty.servlet.ServletHandler;
import org.mortbay.jetty.servlet.ServletHolder;

public class ZenJmxMain {

    // logger
    private static final Log _logger = LogFactory.getLog(ZenJmxMain.class);

    /**
     * @param args
     */
    public static void main(String[] args) throws Exception {

        Configuration config = Configuration.instance();
        parseArguments(config, args);
        String port = config.getProperty(OptionsFactory.LISTEN_PORT,
                OptionsFactory.DEFAULT_LISTENPORT);

        Server server = new Server();
        Connector connector = new SocketConnector();
        connector.setPort(Integer.parseInt(port));
        server.setConnectors(new Connector[] { connector });

        ServletHandler handler = new ServletHandler();

        ServletHolder holder = new ServletHolder(new XmlRpcServlet());
        handler.addServletWithMapping(holder, "/");
        // handler.start();
        handler.initialize();

        server.setHandler(handler);
        server.start();
        server.join();
    }

    /**
     * Parses the command line arguments
     */
    private static void parseArguments(Configuration config, String[] args)
            throws ParseException, NumberFormatException {

        OptionsFactory factory = OptionsFactory.instance();
        Options options = factory.createOptions();
        // parse the command line
        CommandLineParser parser = new PosixParser();
        CommandLine cmd = parser.parse(options, args);

        // get the config file argument and load it into the properties
        if (cmd.hasOption(OptionsFactory.CONFIG_FILE)) {
            String filename = cmd.getOptionValue(OptionsFactory.CONFIG_FILE);
            try {
                config.load(new FileInputStream(filename));
            } catch (IOException e) {
                _logger.error("failed to load configuration file", e);
            }
        } else {
            _logger.warn("no config file option (--"
                    + OptionsFactory.CONFIG_FILE + ") specified");
            _logger.warn("only setting options based on command "
                    + "line arguments");
        }

        for (String arg : args) {
            _logger.debug("arg: " + arg);
        }

        // interrogate the options and get the argument values
        overrideProperty(config, cmd, OptionsFactory.LISTEN_PORT);

        // tell the user about the arguments
        _logger.info("zenjmxjava configuration:");
        _logger.info(config.toString());
    }

    /**
     * Checks the CommandLine for the property with the name provided. If
     * present it sets the name and value pair in the _config field.
     */
    private static void overrideProperty(Configuration config, CommandLine cmd,
            String name) {
        if (cmd.hasOption(name)) {
            String value = cmd.getOptionValue(name);
            config.setProperty(name, value);
        }
    }

}
