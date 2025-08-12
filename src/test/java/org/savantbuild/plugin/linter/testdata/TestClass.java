/*
 * Copyright (c) 2024, Inversoft Inc., All Rights Reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */
package org.savantbuild.plugin.linter.testdata;

// Unused import for Checkstyle to catch
import java.util.List;
// Another unused import
import java.util.Map;

/**
 * Test class with various issues for linter testing.
 *
 * @author Test Author
 */
public class TestClass {
  
  // PMD will catch this unused private field
  private String unusedField = "This field is never used";
  
  // Checkstyle might complain about naming (should be CONSTANT_CASE)
  private static final String badConstant = "should be uppercase";
  
  public String someMethod(String input) {
    // SpotBugs will catch this - comparing String with ==
    if (input == "test") {
      return "matched";
    }
    
    // PMD will catch this empty catch block
    try {
      Integer.parseInt(input);
    } catch (NumberFormatException e) {
      // Empty catch block
    }
    
    // SpotBugs will catch this - unnecessary String constructor
    return new String("result");
  }
  
  // Missing @Override annotation - some linters catch this
  public boolean equals(Object obj) {
    // Poor equals implementation - always returns true
    return true;
  }
  
  // Missing hashCode when equals is overridden
}