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


/**
 * <p> Tests the methods in the SingleValueAttributeCall class.  Don't
 * feel reassured though - the extensiveness of the test is
 * lacking...  </p>
 *
 * <p>$Author: chris $</p>
 *
 * @author Christopher Blunck
 * @version $Revision: 1.6 $
 */
public class SingleValueAttributeCallTest
  extends TestCase {

  // the configuration for the call
  private static final String URL = 
    "service:jmx:jmxrmi://localhost:12345/mbean";
  private static final boolean AUTHENTICATE = true;
  private static final String USERNAME = "username";
  private static final String PASSWORD = "password";
  private static final String OBJECT_NAME = "mbean";
  private static final String ATTR_NAME = "attribute";
  private static final String ATTR_TYPE = "attribute type";

  // the calls we will test
  private static SingleValueAttributeCall _call1;
  private static SingleValueAttributeCall _call2;


  /**
   * Constructs a test that invokes a specific test method
   */
  public SingleValueAttributeCallTest(String method) {
    super(method);
  }


  /**
   * Called before each method is invoked
   */
  public void setUp() { 
    _call1 = 
      new SingleValueAttributeCall(URL, AUTHENTICATE, 
                                   USERNAME, PASSWORD, 
                                   OBJECT_NAME, ATTR_NAME, ATTR_TYPE);

    _call2 = 
      new SingleValueAttributeCall(URL, AUTHENTICATE, 
                                   USERNAME, PASSWORD, 
                                   OBJECT_NAME, ATTR_NAME, ATTR_TYPE);
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
    TestSuite suite = new TestSuite(SingleValueAttributeCallTest.class);
    return suite;
  }

  
  /**
   * Runs all the tests via the command line.
   */
  public static void main(String[] args) {
    TestRunner.run(SingleValueAttributeCallTest.class);
  }


  /*
   * TEST METHODS BEGIN HERE
   */

  /**
   * Tests the hashCode() method
   */
  public void testHashCode() {
    assertEquals(_call1.hashCode(), _call2.hashCode());
  }


  /**
   * Tests the equals() method
   */
  public void testEquals() {
    assertTrue(_call1.equals(_call2));
  }

}
