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


/**
 * <p> Represents a problem that occurred as a result of
 * configuration.  </p>
 *
 * <p>$Author: chris $</p>
 *
 * @author Christopher Blunck
 * @version $Revision: 1.6 $
 */
public class ConfigurationException 
  extends Exception {

  /**
   * Creates a ConfigurationException based on a message and an
   * exception that caused the ConfigurationException.
   */
  public ConfigurationException(String message, Throwable t) {
    super(message, t);
  }


  /**
   * Creates a ConfigurationException given a message
   */
  public ConfigurationException(String message) {
    super(message);
  }
}
