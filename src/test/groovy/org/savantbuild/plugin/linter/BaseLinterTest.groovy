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
import java.nio.file.Path
import java.nio.file.Paths

import org.savantbuild.dep.domain.License
import org.savantbuild.domain.Project
import org.savantbuild.domain.Version
import org.savantbuild.io.FileTools
import org.savantbuild.output.Output
import org.savantbuild.output.SystemOutOutput
import org.savantbuild.runtime.RuntimeConfiguration
import org.testng.annotations.BeforeMethod
import org.testng.annotations.BeforeSuite

/**
 * Base test class for linter plugin tests.
 *
 * @author Daniel DeGroff
 */
abstract class BaseLinterTest {
  public static Path projectDir

  Output output

  Project project

  LinterPlugin plugin

  @BeforeSuite
  static void beforeSuite() {
    try {
      projectDir = Paths.get("").toAbsolutePath()
      if (!Files.isRegularFile(projectDir.resolve("LICENSE"))) {
        projectDir = Paths.get("../linter-plugin").toAbsolutePath()
      }
    } catch (Exception e) {
      // Fallback to current directory
      projectDir = Paths.get("").toAbsolutePath()
    }
  }

  @BeforeMethod
  void beforeMethod() {
    try {
      output = new SystemOutOutput(true)
      output.enableDebug()

      // Use the main project directory instead of a separate test-project
      project = new Project(projectDir, output)
      project.group = "org.savantbuild.plugin"
      project.name = "linter"
      project.version = new Version("2.0.3")
      project.licenses.add(License.parse("Apache-2.0", null))

      plugin = new LinterPlugin(project, new RuntimeConfiguration(), output as Output)
      plugin.settings.reportDirectory = Paths.get("build/test-linter-reports")

      // Ensure the directory exists before clearing it
      Files.createDirectories(plugin.settings.reportDirectory)
      
      // Clear the output directory only if it exists and is not empty
      if (Files.exists(plugin.settings.reportDirectory)) {
        FileTools.prune(plugin.settings.reportDirectory)
      }
    } catch (Exception e) {
      output.errorln("Test setup failed: " + e.getMessage())
      e.printStackTrace()
      throw e
    }
  }
}