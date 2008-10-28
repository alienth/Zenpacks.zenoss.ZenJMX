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

import java.util.Collection;
import java.util.List;
import java.util.HashSet;


/**
 * <p> The apache commons CLI library does not provide a convenience
 * class for printing the Options in a user-friendly way.  The
 * toString() methods all are intended for debugging.  This class
 * allows you to print out the required and optional arguments in an
 * Options instance.</p>
 *
 * <p>$Author: chris $</p>
 *
 * @author Christopher Blunck
 * @version $Revision: 1.6 $
 */
public class OptionsPrinter {

  // the options to print
  private Options _options;


  /**
   * Creates an OptionPrinter that applies to the Options provided
   */
  public OptionsPrinter(Options options) {
    _options = options;
  }


  /**
   * Returns a user-readable representation of an Option
   */
  private String print(Option option) {
    return "  " + option.getOpt() + ": " + option.getDescription();
  }


  /**
   * Returns a friendly string representation of the options
   */
  public String toString() {
    StringBuffer buffer = new StringBuffer();

    // get the required and optional arguments
    List required = _options.getRequiredOptions();
    Collection optional = new HashSet(_options.getOptions());

    buffer.append("Required Arguments:\n");
    for (Object nameObj : required) {
      String name = nameObj.toString();
      Option option = _options.getOption(name);

      buffer.append("  " + print(option) + "\n");
    }

    buffer.append("Options:\n");
    for (Object optionObj : optional) {
      Option option = (Option) optionObj;

      if (! required.contains(option.getOpt())) {
        buffer.append("  " + print(option) + "\n");
      }
    }
    
    return buffer.toString();
  }
}
