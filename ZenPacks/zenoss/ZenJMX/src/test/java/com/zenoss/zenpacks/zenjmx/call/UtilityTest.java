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

import junit.framework.*;
import junit.textui.TestRunner;

import java.util.Map;
import java.util.HashMap;
import java.util.List;


/**
 * <p> Tests the methods in the Utility class.  </p>
 *
 * <p>$Author: chris $</p>
 *
 * @author Christopher Blunck
 * @version $Revision: 1.6 $
 */
public class UtilityTest
  extends TestCase {

  /**
   * Constructs a test that invokes a specific test method
   */
  public UtilityTest(String method) {
    super(method);
  }


  /**
   * Called before each method is invoked
   */
  public void setUp() { }


  /**
   * Called after each method finishes execution
   */
  public void tearDown() { }


  /**
   * Defines the list of test methods to run.  By default we'll run
   * 'em all.
   */
  public static Test suite() {
    TestSuite suite = new TestSuite(UtilityTest.class);
    return suite;
  }

  
  /**
   * Runs all the tests via the command line.
   */
  public static void main(String[] args) {
    TestRunner.run(UtilityTest.class);
  }


  /*
   * TEST METHODS BEGIN HERE
   */

  /**
   * Tests the get() method
   */
  public void testGet() {
    Map<String, String> config = new HashMap<String, String>();
    config.put("a", "b");

    // make sure unknowns work
    assertTrue("".equals(Utility.get(config, "z", "z")));

    // make sure jproperties work
    assertTrue("b".equals(Utility.get(config, "a", "z")));

    // make sure zproperties work
    assertTrue("b".equals(Utility.get(config, "z", "a")));
  }


  /**
   * Tests the getUrl() method
   */
  public void testGetUrl() {
    Map<String, String> config = new HashMap<String, String>();
    String url = null;

    try {
      url = Utility.getUrl(config);
      fail("created url without any properties");
    } catch (Exception e) { }

    config.put("zJmxManagementPort", "12345");
    try {
      url = Utility.getUrl(config);
      fail("created url without a host address");
    } catch (Exception e) { }


    config.put("manageIp", "localhost");
    try {
      assertNotNull("url not created", Utility.getUrl(config));
    } catch (Exception e) { 
      fail("failed to create url with valid host/port info");
    }

    config.remove("manageIp");
    config.put("device", "localhost");
    try {
      assertNotNull("url not created", Utility.getUrl(config));
    } catch (Exception e) { 
      fail("failed to create url with valid host/port info");
    }
  }


  /**
   * Tests the getUsername() method
   */
  public void testGetUsername() {
    Map<String, String> config = new HashMap<String, String>();
    assertTrue("blank not handled", "".equals(Utility.getUsername(config)));
    
    String val = "username1";
    config.put("zJmxManagementUsername", val);
    assertTrue("zproperty not handled", 
               val.equals(Utility.getUsername(config)));

    val = "username2";
    config.put("username", val);
    assertTrue("jmx property not handled", 
               val.equals(Utility.getUsername(config)));
  }


  /**
   * Tests the getPassword() method
   */
  public void testGetPassword() {
    Map<String, String> config = new HashMap<String, String>();
    assertTrue("blank not handled", "".equals(Utility.getPassword(config)));
    
    String val = "password1";
    config.put("zJmxManagementPassword", val);
    assertTrue("zproperty not handled", 
               val.equals(Utility.getPassword(config)));

    val = "password2";
    config.put("password", val);
    assertTrue("jmx property not handled", 
               val.equals(Utility.getPassword(config)));
  }


  /**
   * Tests the downcast() method
   */
  public void testDowncast() {
    List<String> result = null;
    assertNotNull("didn't handle null", Utility.downcast(null));

    Object[] values = new Object[] { };
    result = Utility.downcast(values);
    assertTrue("didn't handle empty array", result.size() == 0);

    values = new Object[] { "a", "b", "c" };
    result = Utility.downcast(values);
    assertTrue("size is incorrect", result.size() == values.length);
    
    int i = 0;
    for (String testValue : result) {
      assertTrue("value is incorrect", testValue.equals(values[i++]));
    }
  }


  /**
   * Tests the equals(Object, Object) method
   */
  public void testEqualsTwoObjects() {
    assertTrue("didn't handle nulls", Utility.equals(null, null));
    assertFalse("didn't handle null and good value", Utility.equals(null, "a"));
    assertFalse("didn't handle good value and null", Utility.equals("a", null));
    assertTrue("didn't handle the same values", Utility.equals("a", "a"));
    assertFalse("didn't handle different values", Utility.equals("a", "b"));
  }


  /**
   * Tests the equals(Object[], Object[]) method
   */
  public void testEqualsTwoArrays() {
    assertTrue("didn't handle nulls", Utility.equals(null, null));

    Object[] vals1 = new Object[] { };
    Object[] vals2 = new Object[] { };
    assertTrue("didn't handle blank arrays", Utility.equals(vals1, vals2));

    assertFalse("didn't handle blank array and null", 
                Utility.equals(vals1, null));

    assertFalse("didn't handle null and blank array", 
                Utility.equals(null, vals2));

    vals1 = new Object[] { "a" };
    assertFalse("didn't handle values and null", Utility.equals(vals1, null));
    assertFalse("didn't handle null and values", Utility.equals(null, vals1));
    
    vals2 = new Object[] { "a" };
    assertTrue("didn't handle same values", Utility.equals(vals1, vals2));
    
    vals2 = new Object[] { "a", "b" };
    assertFalse("didn't handle different values", Utility.equals(vals1, vals2));
  }

}
