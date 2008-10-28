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
  public static final String LISTEN_PORT = "zenjmxjavaport";
  public static final String CONFIG_FILE = "configfile";
  public static final String LOG_SEVERITY = "v";
  public static final String CONCURRENT_JMX_CALLS = "concurrentJMXCalls";
  
  // default values (also set in zenjmx.conf)
  public static final String DEFAULT_LISTENPORT = "9988";

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
   * Creates command line options
   */
  public Options createOptions() {
    Options o = new Options();

    // everything is treated as an optional argument
    o.addOption(createOption(CONFIG_FILE, true,  "configuration file"));
    o.addOption(createOption(LISTEN_PORT, true,  "Port to listen for requests"));
    o.addOption(createOption(LOG_SEVERITY, true,  "Severity for logging"));
    o.addOption(createOption(CONCURRENT_JMX_CALLS, false,  "Enable concurrent calls to a JMX server"));
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
