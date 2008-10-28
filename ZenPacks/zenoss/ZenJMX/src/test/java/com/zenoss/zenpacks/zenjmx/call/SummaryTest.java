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

import junit.framework.*;
import junit.textui.TestRunner;

import java.util.Map;
import java.util.HashMap;


/**
 * <p> Tests the methods in the Summary class.  Don't feel reassured
 * though - the extensiveness of the test is lacking...  </p>
 *
 * <p>$Author: chris $</p>
 *
 * @author Christopher Blunck
 * @version $Revision: 1.6 $
 */
public class SummaryTest
  extends TestCase {

  // the configuration for the Summary
  private static final String DEVICE_ID = "device";
  private static final String OBJECT_NAME = "objectName";
  private static final String CALL_SUMMARY = "single-value attribute call";
  private static final String DATA_SOURCE_ID = "dataSourceId";
  private static final Map<String, Object> RESULTS;
  private static final Map<String, String> TYPE_MAP;
  private static final long RUNTIME = 12L;
  private static final boolean CANCELLED = false;
  private static final int CALL_ID = 34;

  // the summaries we will test
  private static Summary _summ1;
  private static Summary _summ2;

  static {
    RESULTS = new HashMap<String, Object>();
    RESULTS.put("a", "a");

    TYPE_MAP = new HashMap<String, String>();
    TYPE_MAP.put("a", "java.lang.String");
  }


  /**
   * Constructs a test that invokes a specific test method
   */
  public SummaryTest(String method) {
    super(method);
  }


  /**
   * Creates a Summary based on default values
   */
  private Summary createSummary() {
    Summary s = new Summary();

    s.setDeviceId(DEVICE_ID);
    s.setObjectName(OBJECT_NAME);
    s.setCallSummary(CALL_SUMMARY);
    s.setDataSourceId(DATA_SOURCE_ID);
    s.setResults(RESULTS);
    s.setRuntime(RUNTIME);
    s.setCancelled(CANCELLED);
    s.setCallId(CALL_ID);

    return s;
  }


  /**
   * Called before each method is invoked
   */
  public void setUp() { 
    _summ1 = createSummary();
    _summ2 = createSummary();
  }


  /**
   * Called after each method finishes execution
   */
  public void tearDown() { }


  /**
   * Defines the list of test methods to run.  By default we'll run
   * 'em all.
   */
  public static Test suite() {
    TestSuite suite = new TestSuite(SummaryTest.class);
    return suite;
  }

  
  /**
   * Runs all the tests via the command line.
   */
  public static void main(String[] args) {
    TestRunner.run(SummaryTest.class);
  }


  /*
   * TEST METHODS BEGIN HERE
   */

  /**
   * Tests the equals() method
   */
  public void testEquals() {
    assertTrue(_summ1.equals(_summ2));
  }

}
