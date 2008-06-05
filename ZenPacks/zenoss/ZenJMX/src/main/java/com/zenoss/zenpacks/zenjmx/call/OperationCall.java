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

import java.io.IOException;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Iterator;
import java.util.Arrays;

import static com.zenoss.zenpacks.zenjmx.call.JmxCall.*;
import static com.zenoss.zenpacks.zenjmx.call.CallFactory.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * <p> Call for an operation's output.  Operations are difficult from
 * an output-parsing standpoint because the JMX Remote API only
 * returns an Object from the invoke() call.  As a result we have to
 * do a lot of test-casting to see if the result is a Map, List,
 * Object[], or a simple Object.  See the JavaDocs for marshal for
 * additional information. </p>
 *
 * <p>$Author: chris $</p>
 *
 * @author Christopher Blunck
 * @version $Revision: 1.6 $
 */
public class OperationCall
  extends JmxCall {

  // configuration parameters
  public static final String OPERATION_NAME = "operationName";
  public static final String OPERATION_PARAM_VALUES = "operationParamValues";
  public static final String OPERATION_PARAM_TYPES = "operationParamTypes";

  // demarcates parameter types
  public static final String DELIMITER = ",";


  // the name of the operation to invoke
  private String _operationName;

  // parameter values that are passed to the operation
  private Object[] _values;

  // parameter types that are associated with the values
  private String[] _types;

  // the keys we are interested in extracting from the result
  private List<String> _keys;

  // logger
  private static final Log _logger = LogFactory.getLog(OperationCall.class);


  /**
   * Creates a OperationCall
   */
  public OperationCall(String url,
                       boolean authenticate,
                       String username,
                       String password,
                       String objectName,
                       String operationName,
                       Object[] paramValues,
                       String[] paramTypes,
                       List<String> keys,
                       List<String> types) {
    super(url, authenticate, username, password, objectName);

    _operationName = operationName;
    _values = paramValues;
    _types = paramTypes;
    _keys = keys;

    setTypeMap(buildTypeMap(keys, types));

    _summary.setCallSummary("operation invocation: " + 
                            operationName + "(" + paramTypes + ")");
  }


  /**
   * <p>Marshals the results into a Map<String, Object> by iterating
   * over the results and the _keys.  If there are more _keys than
   * results the trailing _keys are NOT set in the Map.</p>
   * @param results the Object[] to marshal
   * @return a Map<String, Object> that contains the results after
   * they have been paired with the _keys
   */
  private Map<String, Object> marshalObjectArray(Object[] results) {
    return marshalList(Arrays.asList(results));
  }


  /**
   * <p>Marshals the results into a Map<String, Object> by iterating
   * over the results and the _keys.  If there are more _keys than
   * results the trailing _keys are NOT set in the Map.</p>
   * @param results the List to marshal
   * @return a Map<String, Object> that contains the results after
   * they have been paired with the _keys
   */
  private Map<String, Object> marshalList(List<Object> results) {
    Map<String, Object> toReturn = new HashMap<String, Object>();

    if (results.size() < _keys.size()) {
      _logger.warn("insufficient result size");
    }
    
    Iterator<String> keyIter = _keys.iterator();
    for (Object result : results) {
      toReturn.put(keyIter.next(), result);
    }

    return toReturn;
  }


  /**
   * <p>Marshals the results into a Map<String, Object> by iterating
   * over the _keys and extracting the values from the Map provided.
   * </p>
   * @param results the Map to marshal
   * @return a Map<String, Object> that contains the results after
   * they have been paired with the _keys.  Keys that do not appear in
   * the results are returned with a null object.
   */
  private Map<String, Object> marshalMap(Map results) {
    Map<String, Object> toReturn = new HashMap<String, Object>();
    
    for (String key : _keys) {
      toReturn.put(key, results.get(key));
    }

    return toReturn;
  }


  /**
   * <p> This method encapsulates all of the ugliness associated with
   * extracting data point values from operation invocations.  The API
   * for invocation simply states that the return of invoke() is an
   * Object.  </p>
   *
   * <p> When the Object returned is a Map we simply reduce that Map
   * down using the keys we are interested in using. </p>
   *
   * <p> When the Object returned is an Object[] we iterate over the
   * _keys and use them in the order in which they were defined when
   * populating the results.  For example, if the _keys are:
   * ['gender', 'age', 'location'] we treat Object[0] as 'gender',
   * Object[1] as 'age', and Object[2] as 'location'.  If there are
   * additional Objects in the array we drop them.  If there are more
   * _keys than Objects[] we place null objects in the Map as the
   * value for the keys. </p>
   *
   * <p> When the Object returned is an Object and there is only one
   * value in _keys we simply put the two into the results Map</p>
   * 
   * <p> It's not pretty, but the JMX Remote API doesn't offer us
   * anything more than an Object to deal with so this is the best we
   * can do...  </p>
   *
   * @param response the result from a call to invoke()
   * @return a Map<String, Object> that corresponds to the parsed
   * results
   */
  private Map<String, Object> marshal(Object result) {
    Map<String, Object> toReturn = new HashMap<String, Object>();

    if (result instanceof Object[]) {
      return marshalObjectArray((Object[]) result);
    }

    if (result instanceof List) {
      return marshalList((List) result);
    }

    if (result instanceof Map) {
      return marshalMap((Map) result);
    }

    return marshalObjectArray(new Object[] { result });
  }

  
  /**
   * @see Callable#call
   */
  public Summary call()
    throws Exception {

    // record when we started
    _startTime = System.currentTimeMillis();

    setCredentials();
    
    // connect to the agent
    _client.connect();
    
    // issue the query
    Object result = _client.invoke(_objectName, _operationName, 
                                   _values, _types);
    
    // disconnect from the agent.
    _client.close();
    
    _summary.setResults(marshal(result));

    // record the runtime of the call
    _summary.setRuntime(System.currentTimeMillis() - _startTime);

    // set our id so the processor can remove it from the reactor
    _summary.setCallId(hashCode());
    
    return _summary;
  }


  /**
   * Creates a OperationCall from the configuration provided
   */
  public static OperationCall fromValue(Map config) {
    String url = Utility.getUrl(config);
    boolean auth = false;
    if (config.containsKey(AUTHENTICATE)) {
       auth = ((Boolean)config.get(AUTHENTICATE)).booleanValue();
    }
    
    String types = (String) config.get(OPERATION_PARAM_TYPES);
    String[] paramTypes = types.split(DELIMITER);
    if ("".equals(types)) {
      paramTypes = new String[] { };
    }

    // FIXME: marshal String to Object[] representations
    Object[] paramValues = new Object[] { };

    // ugly form of downcasting...  but XML-RPC doesn't give us a List<String>
    List<String> keys = Utility.downcast((Object[]) config.get(DATA_POINT));
    _logger.debug("keys: " + keys);

    List<String> rrdTypes = Utility.downcast((Object[]) config.get(TYPES));

    OperationCall call = 
      new OperationCall(url,
                        auth,
                        Utility.getUsername(config),
                        Utility.getPassword(config),
                        (String) config.get(OBJECT_NAME),
                        (String) config.get(OPERATION_NAME),
                        paramValues,
                        paramTypes,
                        keys,
                        rrdTypes);

    return call;
  }


  /**
   * Returns the name of the operation to invoke
   */
  public String getOperationName() { return _operationName; }


  /**
   * Returns the values of the parameters passed to the operation
   */
  public Object[] getValues() { return _values; }


  /**
   * Returns the types of the parameters passed to the operation
   */
  public String[] getTypes() { return _types; }


  /**
   * Returns the keys we are interested in extracting from the result
   */
  public List<String> getKeys() { return _keys; }


  /**
   * @see JmxCall#hashCode
   */
  public int hashCode() {
    int hc = 0;

    hc += super.hashCode();
    hc += hashCode(_operationName);

    // arrays with the same contents do not return the same hashCode value
    for (String type : _types) {
      hc += hashCode(type);
    }

    // arrays with the same contents do not return the same hashCode value
    for (Object value : _values) {
      hc += hashCode(value);
    }

    // collections with the same contents do return the same hashCode
    hc += hashCode(_keys);

    return hc;
  }


  /**
   * @see Object#equals
   */
  public boolean equals(Object other) {
    if (! (other instanceof OperationCall)) {
      return false;
    }

    boolean toReturn = super.equals(other);

    OperationCall call = (OperationCall) other;

    toReturn &= Utility.equals(call.getOperationName(), getOperationName());
    toReturn &= Utility.equals(call.getValues(), getValues());
    toReturn &= Utility.equals(call.getTypes(), getTypes());
    toReturn &= Utility.equals(call.getKeys(), getKeys());
    
    return toReturn;
  }
}
