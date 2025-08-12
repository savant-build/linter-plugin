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
package org.savantbuild.plugin.linter

import java.nio.file.Files

import org.savantbuild.runtime.BuildFailureException
import org.testng.annotations.Test

import static org.testng.AssertJUnit.assertFalse
import static org.testng.AssertJUnit.assertTrue
import static org.testng.AssertJUnit.fail

/**
 * Tests the Checkstyle functionality of the Linter plugin.
 *
 * @author Daniel DeGroff
 */
class CheckstylePluginTest extends BaseLinterTest {
  
  @Test
  void checkstyle() throws Exception {
    // Execute Checkstyle - should find violations
    try {
      plugin.checkstyle(
          configFile: "src/test/resources/checkstyle/test-checkstyle.xml",
          inputPath: "src/test/java"  // Analyze test Java files
      )
      fail("Expected Checkstyle analysis to fail.")
    } catch (BuildFailureException e) {
      assertTrue("Expected failure message to contain violation count", 
                 e.getMessage().contains("Checkstyle analysis failed with"))
      assertTrue("Expected failure message to contain 'violations'", 
                 e.getMessage().contains("violations."))
    }

    // Verify report was created
    assertTrue(Files.exists(plugin.settings.reportDirectory.resolve("checkstyle-report.xml")))
    
    // Verify report contains violations
    String xmlReport = new String(Files.readAllBytes(plugin.settings.reportDirectory.resolve("checkstyle-report.xml")))
    assertTrue("Expected XML report to contain errors", xmlReport.contains("<error"))
    assertTrue("Expected XML report to contain unused imports violation", 
               xmlReport.contains("UnusedImports") || xmlReport.contains("unused"))

    // Perform the same request, but do not fail on violations
    try {
      plugin.checkstyle(
          configFile: "src/test/resources/checkstyle/test-checkstyle.xml",
          inputPath: "src/test/java",
          failOnViolations: false
      )
    } catch (BuildFailureException e) {
      fail("Did not expect Checkstyle analysis to fail when failOnViolations=false.\n" + e.getMessage())
    }

    // Report should still exist and contain violations
    assertTrue(Files.exists(plugin.settings.reportDirectory.resolve("checkstyle-report.xml")))
    
    xmlReport = new String(Files.readAllBytes(plugin.settings.reportDirectory.resolve("checkstyle-report.xml")))
    assertTrue("Expected XML report to contain errors", xmlReport.contains("<error"))
  }

  @Test
  void checkstyle_missing_config() throws Exception {
    // Execute Checkstyle without config file
    try {
      plugin.checkstyle(
          inputPath: "src/test/java"
      )
      fail("Expected Checkstyle analysis to fail.")
    } catch (BuildFailureException e) {
      assertTrue("Expected error message about missing config file", 
                 e.getMessage().contains("You must specify a [configFile] argument"))
    }

    // We failed early, so report should not be created
    assertFalse(Files.exists(plugin.settings.reportDirectory.resolve("checkstyle-report.xml")))
  }

  @Test
  void checkstyle_nonexistent_config() throws Exception {
    // Execute Checkstyle with non-existent config file
    try {
      plugin.checkstyle(
          configFile: "src/test/resources/checkstyle/nonexistent.xml",
          inputPath: "src/test/java"
      )
      fail("Expected Checkstyle analysis to fail.")
    } catch (BuildFailureException e) {
      assertTrue("Expected error message about config file not found", 
                 e.getMessage().contains("Checkstyle configuration file not found"))
    }

    // We failed early, so report should not be created
    assertFalse(Files.exists(plugin.settings.reportDirectory.resolve("checkstyle-report.xml")))
  }

  @Test
  void checkstyle_custom_report_name() throws Exception {
    // Execute Checkstyle with custom report name, don't fail on violations
    try {
      plugin.checkstyle(
          configFile: "src/test/resources/checkstyle/test-checkstyle.xml",
          inputPath: "src/test/java",
          reportFileName: "custom-checkstyle",
          failOnViolations: false
      )
    } catch (BuildFailureException e) {
      fail("Did not expect Checkstyle analysis to fail when failOnViolations=false.\n" + e.getMessage())
    }

    // Verify custom report name was used
    assertTrue(Files.exists(plugin.settings.reportDirectory.resolve("custom-checkstyle.xml")))
    assertFalse(Files.exists(plugin.settings.reportDirectory.resolve("checkstyle-report.xml")))
  }
}