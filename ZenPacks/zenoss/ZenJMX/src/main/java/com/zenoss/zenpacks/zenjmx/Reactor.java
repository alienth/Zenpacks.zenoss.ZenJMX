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

import java.util.Random;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledFuture;

import static java.util.concurrent.TimeUnit.*;

import com.zenoss.zenpacks.zenjmx.call.Summary;
import com.zenoss.zenpacks.zenjmx.call.JmxCall;
import com.zenoss.zenpacks.zenjmx.call.ConfigurationCall;
import com.zenoss.zenpacks.zenjmx.call.CountDownLatchObserver;
import com.zenoss.zenpacks.zenjmx.call.HeartbeatCall;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.apache.xmlrpc.XmlRpcException;


/**
 * <p> Manages configuration requests, jmx agent requests, and
 * response processing. </p>
 *
 * <p> The hubDispatcher periodically calls ZenHub over XML-RPC and
 * retrieves data source configurations associated with a particular
 * performance collector. </p>
 *
 * <p> The injector class is a Runnable that is executed periodically
 * by the _injector ExecutorService.  The Injector class uses the
 * getConfigs() accessor method in the Reactor to retrieve the JMX
 * data sources that were loaded using the hubDispatcher.  It then
 * creates JmxCall instances and dispatches them in the reactor. </p>
 *
 * <p> The _jmxDispatcher ExecutorService schedules JmxCalls for
 * immediate execution.  The returned Future<Summary> is passed to a
 * Processor, which is dispatched via the _jmxProcessor
 * ExecutorService.  This allows us to asynchronousely submit requests
 * and process their results. </p>
 *
 * <p> Note: The JMX request pool size cannot be adjusted during
 * runtime.  </p>
 *
 * <p>$Author: chris $<br>
 * $Date: 2005/03/13 18:45:25 $</p>
 *
 * @author Christopher Blunck
 * @version $Revision: 1.6 $
 */
public class Reactor {

  // number of thrads to create in the various dispatchers
  public static final int JMX_THREAD_POOL_SIZE = 10;
  public static final int HUB_THREAD_POOL_SIZE = 1;
  public static final int INJECTOR_THREAD_POOL_SIZE = 1;

  // amount of time (in seconds) to wait for a JMX query to time out
  public static final int JMX_TIMEOUT = 1000;

  // default amount of time (in seconds) between JMX queries
  public static final int JMX_CYCLE_TIME = 60 * 5;

  // number of times each request has been sent
  private Map<Integer, Integer> _requestCount;

  // number of times a response has been received for a request
  private Map<Integer, Integer> _responseCount;

  // the scheduler and processor issue and process JMX queries and responses
  private ScheduledExecutorService _jmxDispatcher;
  private ExecutorService _jmxProcessor;

  // the scheduler used for observing the completion of processors
  private ExecutorService _latchProcessor;
  private CountDownLatchObserver _latchObserver;

  // sends heartbeats back to the central server
  private ExecutorService _heartbeatDispatcher;
  private HeartbeatCall _heartbeatCall;

  // the number of seconds to wait on a jmx query before timing out
  private int _jmxTimeOut;

  // the scheduler that retrieves data source configurations
  private ScheduledExecutorService _hubDispatcher;

  // the scheduler that injects query requests into the reactor
  private ScheduledExecutorService _injectorDispatcher;
  private Injector _injector;


  // the data source configurations
  private List<Map> _configs;

  // Set of JmxCall hashcodes that are in flight
  private Set<Integer> _calls;

  // set the true if the reactor should cycle
  private boolean _cycle;

  // overrides the cycle time read from the performance configuration
  private int _cycleTime = JMX_CYCLE_TIME;

  // singleton instance
  private static Reactor _instance;

  // logger
  private static final Log _logger = LogFactory.getLog(Reactor.class);


  /**
   * Creates a new Reactor
   */
  private Reactor(int jmxPoolSize, int jmxTimeOut, int cycleTime) { 
    // use the default if the provided pool size is negative
    if (jmxPoolSize < 0) {
      jmxPoolSize = JMX_THREAD_POOL_SIZE;
    } 

    // use the default if the provided timeout is negative
    if (jmxTimeOut < 0) {
      _jmxTimeOut = JMX_TIMEOUT;
    } else {
      _jmxTimeOut = jmxTimeOut;
    }

    // set the cycleTime property
    _cycleTime = cycleTime;

    // sends JMX queries and processes responses
    _logger.info("JMX dispatcher pool size: " + jmxPoolSize);
    _jmxDispatcher = Executors.newScheduledThreadPool(jmxPoolSize);
    _jmxProcessor = Executors.newFixedThreadPool(jmxPoolSize * 2);

    // we only need 1 thread to observe the processors progress
    _latchProcessor = Executors.newSingleThreadScheduledExecutor();
    _latchObserver = new CountDownLatchObserver();

    // we only need 1 thread to send heartbeats
    _heartbeatDispatcher = Executors.newSingleThreadScheduledExecutor();
    _heartbeatCall = new HeartbeatCall();

    // sends data source configuration requests to zenhub
    _hubDispatcher = Executors.newScheduledThreadPool(HUB_THREAD_POOL_SIZE);

    // injects JMX queries into the reactor
    _injectorDispatcher = 
      Executors.newScheduledThreadPool(INJECTOR_THREAD_POOL_SIZE);

    // initialize set of flights to be empty
    _calls = new HashSet<Integer>();

    // initialize the counters
    _requestCount = new ConcurrentHashMap<Integer, Integer>();
    _responseCount = new ConcurrentHashMap<Integer, Integer>();
  }


  /**
   * Dispatches a new CountDownLatchObserver to watch the latch
   * provided.
   */
  private void dispatchLatchListener(CountDownLatch latch) {
    _latchObserver.setLatchToObserve(latch);
    _latchProcessor.submit(_latchObserver);
  }


  /**
   * Dispatches calls to JMX agents
   */
  public void dispatch(Set<JmxCall> calls) {
    CountDownLatch latch = new CountDownLatch(calls.size());
    dispatchLatchListener(latch);
    
    synchronized(_requestCount) {
      for (JmxCall call : calls) {
        int hc = call.hashCode();
        _logger.debug("(" + hc + ") dispatching call...");

        int count = 0;
        if (_requestCount.containsKey(hc)) {
          count = _requestCount.get(hc);
        }
        count++;
        _requestCount.put(hc, count);

        if (! isPending(hc)) {
          Future<Summary> handle = _jmxDispatcher.schedule(call, 0, SECONDS);

          addPending(hc);
          process(handle, latch, hc);
        } else {
          latch.countDown();
          _logger.warn("previously issued JMX call (" + hc + ") has not " +
                       "completed yet.");
          _logger.warn("blocking subsequent calls until the call completes.");
        }
      }
    }
  }


  /**
   * Returns true if the call representing by the hashCode provides is
   * currently pending (executing).
   */
  protected boolean isPending(int call) {
    return _calls.contains(call);
  }


  /**
   * Adds the call to the pending/active set
   */
  protected void addPending(int call) {
    _calls.add(call);
    
    if ( _logger.isDebugEnabled()) {
    	_logger.debug(summarizeCounts());
    }
  }


  /**
   * Removes the call from the pending/active set.
   */
  protected void removePending(int call) {
    synchronized(_responseCount) {
      Integer count = 0;
      if (_responseCount.containsKey(call)) {
        count = _responseCount.get(call);
      }
      count++;
      _responseCount.put(call, count);
    }

    synchronized(_calls) {
      _calls.remove(call);
    }

    if ( _logger.isDebugEnabled()) {
      _logger.debug(summarizeCounts());
    }
  }


  /**
   * Blocks on the handle until a response has been read
   */
  public void process(Future<Summary> handle, 
                      CountDownLatch latch, 
                      int callId) {
    Processor p = new Processor(handle, latch, _jmxTimeOut, callId);
    _jmxProcessor.submit(p);
  }


  /**
   * Starts up the reactor.
   * @throws XmlRpcException if an error occurs obtaining the
   * performance configuration data
   */
  public void start(XmlRpcClient client, boolean cycle) 
    throws XmlRpcException {

    // set the cycle flag
    _cycle = cycle;

    // set up the injector
    _injector = new Injector(_cycle);

    // load the performance config
    _logger.info("retrieving performance collector configuration...");
    Map conf = client.getPerformanceConfig();

    // use the user-provided override if it is defined
    int cyclePeriod = _cycleTime;
    if (cyclePeriod == JMX_CYCLE_TIME) {
      cyclePeriod = (Integer) conf.get("perfsnmpCycleInterval");
    }

    // config cycle time is expressed in minutes
    int configPeriod = (Integer) conf.get("configCycleInterval");
    _logger.info("configuration retrieved; cyclePeriod=" + 
                  cyclePeriod + "; configPeriod=" + configPeriod + " mins.");
    configPeriod *= 60;

    // schedule a periodic call to refresh the data source configurations
    ConfigurationCall call = new ConfigurationCall(client);
    _hubDispatcher.scheduleWithFixedDelay(call, 0, configPeriod, SECONDS);

    // if we are instructed to cycle we need periodic injections
    if (_cycle) {
      _injectorDispatcher.scheduleWithFixedDelay(_injector, 
                                                 cyclePeriod, 
                                                 cyclePeriod, 
                                                 SECONDS);
    }

  }


  /**
   * Shuts down the dispatcher and processor
   */
  public void stop() {
    _logger.info("sending shutdown to jmx schedulers...");
    _jmxDispatcher.shutdown();
    _jmxProcessor.shutdown();

    _logger.info("sending shutdown to hub schedulers...");
    _hubDispatcher.shutdown();

    _logger.info("sending shutdown to injector...");
    _injectorDispatcher.shutdown();

    _logger.info("returning from stop()");
  }


  /**
   * Sets the list of configurations to use on the next polling cycle
   * @param configs the data source configurations
   */
  public void updateConfigs(List<Map> configs) { 
    if (configs.equals(_configs)) {
      _logger.debug("data source configuration has not changed.");
      return;
    }
    
    _logger.debug("updating data source configuration.");
    _configs = configs;

    _logger.info("scheduling a collection cycle...");
    _injectorDispatcher.schedule(_injector, 0, SECONDS);
  }


  /**
   * Returns the data source configurations
   */
  public List<Map> getConfigs() { return _configs; }


  /**
   * Singleton style accessor
   * @param jmxPoolSize the number out simultaneous JMX requests to support
   * @param jmxTimeOut the number of seconds to wait on an individual
   * JMX request before timing it out
   * @param cycleTime the time to sleep (in seconds) between jmx cycles
   */
  public synchronized static Reactor instance(int jmxPoolSize, 
                                              int jmxTimeOut,
                                              int cycleTime) {
    if (_instance == null) {
      _instance = new Reactor(jmxPoolSize, jmxTimeOut, cycleTime);
    }

    return _instance;
  }


  /**
   * Singleton style accessor
   */
  public synchronized static Reactor instance() {
    return instance(JMX_THREAD_POOL_SIZE, JMX_TIMEOUT, JMX_CYCLE_TIME);
  }


  /**
   * Sends a heartbeat
   */
  public void sendHeartbeat() {
    try {
      _heartbeatDispatcher.submit(_heartbeatCall);
    } catch (Exception e) {
      _logger.error("failed to send heartbeat", e);
    }
  }


  /**
   * Returns a pretty representation of the request/response count.
   * Warning: this method is computationally expensive!
   * 
   */
  private String summarizeCounts() {
    String summary = "Request/Response Count:\n";

    if (! _logger.isDebugEnabled()) {
      return summary;
    }

    synchronized(_requestCount) {
      for (Integer hc : _requestCount.keySet()) {
        Integer requestCount = _requestCount.get(hc);
        Integer responseCount = 0;
        if (_responseCount.containsKey(hc)) {
          responseCount = _responseCount.get(hc);
        }

        double completePercent = 100.0;
        if (requestCount > 0) {
          completePercent = ((double) responseCount / (double) requestCount) * 100;
        }

        summary += "  " + hc + ": " + requestCount + " req, " + 
          responseCount + " resp (" + completePercent + "%)\n";
      }
    }

    synchronized(_calls) {
      if (_calls.size() > 0) {
        summary += "\nIn-Flight Requests:\n";
        for (Integer hc : _calls) {
          summary += "  " + hc;
        }
      }
    }

    return summary;
  }

}
