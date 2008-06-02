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

import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.CountDownLatch;
import static java.util.concurrent.TimeUnit.*;

import java.util.Map;

import com.zenoss.zenpacks.zenjmx.call.Summary;

import com.zenoss.xmlrpc.XmlRpcClient;

import org.apache.xmlrpc.XmlRpcException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import static com.zenoss.zenpacks.zenjmx.OptionsFactory.*;


/**
 * <p></p>
 *
 * <p>$Author: chris $<br>
 * $Date: 2005/03/13 18:45:25 $</p>
 *
 * @author Christopher Blunck
 * @version $Revision: 1.6 $
 */
public class Processor 
  implements Runnable {

  // reference to the JMX agent call that fires when the call completes
  private Future<Summary> _handle;

  // number of seconds to wait before timing out a call to Future.get()
  private int _timeout;

  // unique configuration id of the call
  private int _callId;

  // the configuration
  private Configuration _config;

  // used by the observer to determine when all processors are
  // finished processing call results in a cycle
  private CountDownLatch _latch;

  // logger
  private static final Log _logger = LogFactory.getLog(Processor.class);


  /**
   * Creates a Processor
   * @param handle the Future that fires when a request to a JMX agent
   * completes
   * @param timeout the number of seconds to wait on the Future to
   * complete
   */
  public Processor(Future<Summary> handle, 
                   CountDownLatch latch, 
                   int timeout, 
                   int callId) {
    _handle = handle;
    _timeout = timeout;
    _callId = callId;
    _config = Configuration.instance();
    _latch = latch;
  }


  /**
   * @see Runnable#run
   */
  public void run() {
    try {
      Reactor reactor = Reactor.instance();
      Summary summary = null;
      try {
        // get the results from the handle (blocks until they are ready)
        _logger.debug("(" + _callId + "): waiting on result...");
        summary = _handle.get(_timeout, SECONDS);
        _logger.info("(" + _callId + "): result: " + summary);

        // notify the reactor that the results were received
        reactor.removePending(_callId);

        // post the performance information up to the server
        postResults(summary);

      } catch (TimeoutException e) {
        _logger.warn("(" + _callId + "): timeout occurred during request");
        reactor.removePending(_callId);
      } catch (Exception e) {
        _logger.error("(" + _callId + "): error occurred while getting result", e);
        reactor.removePending(_callId);
      }

      // set the summary to null so it can be gc'ed
      summary = null;

      // notch one off the count down latch
      _latch.countDown();
    } catch (Throwable e) {
      _logger.error("unexpected error processing jmx results", e);
    }
  }


  /**
   * Posts the results back to the server.
   */
  private void postResults(Summary summary)
    throws XmlRpcException {
    
    Map<String, Object> results = summary.getResults();
    Map<String, String> typeMap = summary.getTypeMap();

    for (String key : results.keySet()) {
      Object value = results.get(key);
      if (value == null) {
        _logger.warn("(" + _callId + "): null value for data point: " + key);
        continue;
      }

      try {
        String dataPoint = summary.getDataSourceId() + "_" + key;

        String hubUrl = _config.getProperty(HUB_URL);
        String confName = _config.getProperty(CONF_NAME);
        String username = _config.getProperty(HUB_USERNAME);
        String password = _config.getProperty(HUB_PASSWORD);
        

        XmlRpcClient client = new XmlRpcClient(hubUrl, confName);
        client.setCredentials(username, password);

        String rrdType = typeMap.get(key);
        if (("".equals(rrdType)) || ("COUNTER".equals(rrdType))) {
          String numeric = value.toString();
          client.writeRRD(summary.getDeviceId(), dataPoint, numeric);
        } else {
          double numeric = Double.valueOf(value.toString());
          client.writeRRD(summary.getDeviceId(), dataPoint, numeric);
        }

        client.close();
        client = null;
      } catch (NumberFormatException e) {
        _logger.warn("(" + _callId + "): data value '" + value + 
                     "' cannot be coerced to an integer.  device: " + 
                     summary.getDeviceId() + "; key: " + key);
      } catch (Exception e) {
        _logger.error("(" + _callId + "): failed to post results to server", e);
      }

    }
  }
}
