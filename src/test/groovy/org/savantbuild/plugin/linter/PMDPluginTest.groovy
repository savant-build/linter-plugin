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

import static org.testng.AssertJUnit.assertEquals
import static org.testng.AssertJUnit.assertFalse
import static org.testng.AssertJUnit.assertTrue
import static org.testng.AssertJUnit.fail

/**
 * Tests the PMD functionality of the Linter plugin.
 *
 * @author Daniel DeGroff
 */
class PMDPluginTest extends BaseLinterTest {
  
  @Test
  void pmd() throws Exception {
    // Execute PMD on test data
    try {
      plugin.pmd(
          inputPath: "src/test/java",  // Analyze test Java files
          minimumPriority: "MEDIUM",
          ruleSets: ["src/test/resources/pmd/ruleset.xml"]
      )
      fail("Expected PMD analysis to fail.")
    } catch (BuildFailureException e) {
      assertEquals("PMD analysis failed.", e.getMessage())
    }

    assertTrue(Files.exists(plugin.settings.reportDirectory.resolve("pmd-report.html")))
    assertTrue(Files.exists(plugin.settings.reportDirectory.resolve("pmd-report.txt")))
    assertTrue(Files.exists(plugin.settings.reportDirectory.resolve("pmd-report.xml")))

    String textReport = new String(Files.readAllBytes(plugin.settings.reportDirectory.resolve("pmd-report.txt")))
    // Should contain violations from our test data files
    assertTrue("Expected report to contain UnusedPrivateField violations", 
               textReport.contains("UnusedPrivateField") || textReport.contains("unusedField"))

    // Perform the same request, but do not fail on violations.
    try {
      plugin.pmd(
          inputPath: "src/test/java",
          failOnViolations: false,
          minimumPriority: "MEDIUM",
          ruleSets: ["src/test/resources/pmd/ruleset.xml"]
      )
    } catch (BuildFailureException e) {
      fail("Did not expected PMD analysis to fail.\n" + e.getMessage())
    }

    assertTrue(Files.exists(plugin.settings.reportDirectory.resolve("pmd-report.html")))
    assertTrue(Files.exists(plugin.settings.reportDirectory.resolve("pmd-report.txt")))
    assertTrue(Files.exists(plugin.settings.reportDirectory.resolve("pmd-report.xml")))

    textReport = new String(Files.readAllBytes(plugin.settings.reportDirectory.resolve("pmd-report.txt")))
    assertTrue("Expected report to contain violations", textReport.length() > 0)
  }

  @Test
  void pmd_missing_rulesets() throws Exception {
    // Execute PMD
    try {
      plugin.pmd()
      fail("Expected PMD analysis to fail.")
    } catch (BuildFailureException e) {
      assertEquals("""You must specify one or more values for the [ruleSets] argument. It will look something like this:

pmd(ruleSets: ["src/test/resources/pmd/ruleset.xml"])""",
          e.getMessage())
    }

    // We failed, so these should not be created
    assertFalse(Files.exists(plugin.settings.reportDirectory.resolve("pmd-report.html")))
    assertFalse(Files.exists(plugin.settings.reportDirectory.resolve("pmd-report.txt")))
    assertFalse(Files.exists(plugin.settings.reportDirectory.resolve("pmd-report.xml")))
  }
}