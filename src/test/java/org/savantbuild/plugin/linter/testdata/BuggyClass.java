/*
 * Copyright (c) 2024, Inversoft Inc., All Rights Reserved
 */
package org.savantbuild.plugin.linter.testdata;

import java.util.Random;

/**
 * Class with more bugs for SpotBugs to find.
 */
public class BuggyClass {
  
  // Static Random without seed - predictable
  private static Random random = new Random();
  
  public void buggyMethod() {
    // Deprecated wrapper constructor
    Integer number = new Integer(42);
    
    // Potential null pointer
    String str = getString();
    if (str.equals("test")) {
      System.out.println("Found test");
    }
  }
  
  private String getString() {
    return Math.random() > 0.5 ? "test" : null;
  }
}