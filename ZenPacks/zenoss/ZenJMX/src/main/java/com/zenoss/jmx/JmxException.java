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
package com.zenoss.jmx;

/**
 * <p>
 * Tagging interfaces for all JmxExceptions.
 * </p>
 * 
 * <p>
 * $Author: chris $<br>
 * $Date: 2007/07/30 18:45:25 $
 * </p>
 * 
 * @author Christopher Blunck
 * @version $Revision: 1.6 $
 */
public class JmxException extends Exception {

    /**
     * Creates a JmxException based on some lower level exception that occurred.
     */
    public JmxException(Throwable t)
        {
        super(t);
        }

    /**
     * Creates a JmxException with a message
     */
    public JmxException(String message)
        {
        super(message);
        }

    /**
     * Creates a JmxException with a message and a Throwable
     */
    public JmxException(String message, Throwable t)
        {
        super(message, t);
        }

    @Override
    public String getMessage()
        {
        String msg = super.getMessage();
        if ( this.getCause() != null )
            {
            msg += " [Nested Exception: " + this.getCause().toString() + "]";
            }
        return msg;
        }
}
