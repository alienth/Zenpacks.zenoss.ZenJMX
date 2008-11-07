///////////////////////////////////////////////////////////////////////////
//
//Copyright 2008 Zenoss Inc 
//Licensed under the Apache License, Version 2.0 (the "License"); 
//you may not use this file except in compliance with the License. 
//You may obtain a copy of the License at 
//    http://www.apache.org/licenses/LICENSE-2.0 
//Unless required by applicable law or agreed to in writing, software 
//distributed under the License is distributed on an "AS IS" BASIS, 
//WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
//See the License for the specific language governing permissions and 
//limitations under the License.
//
///////////////////////////////////////////////////////////////////////////
package com.zenoss.zenpacks.zenjmx.call;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.zenoss.zenpacks.zenjmx.ConfigAdapter;


/**
 * <p> Factory to produce JmxCalls based on the JMX QueryType value
 * and the rest of the configuration. </p>
 *
 * <p>$Author: chris $</p>
 *
 * @author Christopher Blunck
 * @version $Revision: 1.6 $
 */
public class CallFactory {

  // configuration items (keys) in the config returned from the server
  public static final String DATA_POINT = "dps";
  public static final String DEVICE_ID = "device";
  public static final String DATASOURCE_ID = "datasourceId";

  // logger
  private static final Log _logger = LogFactory.getLog(CallFactory.class);


  /**
   * Private constructor enforces non-instantiability
   */
  private CallFactory() { }

  /**
   * Creates a JmxCall from a configuration read from the server
   * @param config the name-value parameters from the server
   * @throws ConfigurationException if the configuration provided does not
   *         contain sufficient information to create a call.
   */
  public static JmxCall createCall(ConfigAdapter config) 
      throws ConfigurationException {
      
      _logger.debug("config: " + config);

      List<String>dataPoints = config.getDataPoints();
      if (dataPoints.isEmpty()) {
          _logger.warn("no data points defined");
          throw new ConfigurationException("Datasource " + 
                          config.getDatasourceId() + "No datapoints defined;" +
                          " will not run collections");
      }


      // if the attributeName is blank the configuration represents an operation
      String attributeName =  config.getAttributeName();
      JmxCall call = null;
      if (attributeName.trim().length() == 0) {
          _logger.debug("creating an operation call");
          call = OperationCall.fromValue(config);
       }else{
          _logger.debug("creating an attribute call");
          call = AttributeCall.fromValue(config);
       }
      return call;
  }
}

