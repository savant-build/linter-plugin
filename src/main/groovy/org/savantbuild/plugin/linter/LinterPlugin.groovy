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

import java.nio.charset.StandardCharsets
import java.nio.file.Files

import org.savantbuild.domain.Project
import org.savantbuild.output.Output
import org.savantbuild.plugin.groovy.BaseGroovyPlugin
import org.savantbuild.runtime.RuntimeConfiguration

import net.sourceforge.pmd.PMDConfiguration
import net.sourceforge.pmd.PmdAnalysis
import net.sourceforge.pmd.lang.LanguageRegistry
import net.sourceforge.pmd.lang.rule.RulePriority
import net.sourceforge.pmd.renderers.HTMLRenderer
import net.sourceforge.pmd.renderers.TextRenderer

/**
 * PMD plugin.
 *
 * @author Daniel DeGroff
 */
class LinterPlugin extends BaseGroovyPlugin {
  public LinterSettings settings = new LinterSettings()

  LinterPlugin(Project project, RuntimeConfiguration runtimeConfiguration, Output output) {
    super(project, runtimeConfiguration, output)
  }

  void pmd(Map attributes = [:]) {
    String reportFormat = attributes.get("reportFormat", "xml")
    String reportFileName = attributes.get("reportFileName", "pmd-report")
    String languageVersion = attributes.get("languageVersion", "17")
    String inputPath = attributes.get("inputPath", "src/main/java")
    String minimumPriority = attributes.get("minimumPriority", "MEDIUM")
    String[] ruleSets = attributes.get("ruleSets", [])

    if (ruleSets.size() == 0) {
      fail("You must specify one or more values for the [ruleSets] argument. It will look something like this: \n\n" +
          "pmd(ruleSets: [\"src/test/resources/pmd/ruleset.xml\"])")
    }

    PMDConfiguration config = new PMDConfiguration()
    config.addInputPath(project.directory.resolve(inputPath))

    config.setDefaultLanguageVersion(LanguageRegistry.PMD.getLanguageById("java").getVersion(languageVersion))
    config.setMinimumPriority(RulePriority.valueOf(minimumPriority))
    config.setReportFormat(reportFormat)
    config.setReportFile(settings.reportDirectory.resolve(reportFileName + "." + reportFormat))

    // Ensure the report directory exists
    Files.createDirectory(settings.reportDirectory)

    TextRenderer textRenderer = new TextRenderer()
    Writer textWriter = new StringWriter()
    textRenderer.setWriter(textWriter)

    Writer htmlWriter = new StringWriter()
    HTMLRenderer htmlRenderer = new HTMLRenderer()
    htmlRenderer.setWriter(htmlWriter)

    def projectPath = project.directory.toAbsolutePath().toString() + "/"
    try (PmdAnalysis pmd = PmdAnalysis.create(config)) {
      for (ruleSet in ruleSets) {
        pmd.addRuleSet(pmd.newRuleSetLoader().loadFromResource(projectPath + ruleSet))
      }

      pmd.addRenderer(htmlRenderer)
      pmd.addRenderer(textRenderer)

      output.infoln("\nPMD analysis configuration ")
      output.infoln("Language: java")
      output.infoln("Language version: %s", languageVersion)
      output.infoln("Minimum priority: %s", minimumPriority)
      output.infoln("Rulesets: %s", String.join(", ", ruleSets))

      var report = pmd.performAnalysisAndCollectReport()

      Files.write(settings.reportDirectory.resolve(reportFileName + ".html"), htmlWriter.toString().getBytes(StandardCharsets.UTF_8))
      Files.write(settings.reportDirectory.resolve(reportFileName + ".txt"), textWriter.toString().getBytes(StandardCharsets.UTF_8))

      output.infoln("\nPMD analysis summary ")
      output.infoln("[%d] Configuration errors", report.configurationErrors.size())
      output.infoln("[%d] ProcessingErrors errors", report.processingErrors.size())
      output.infoln("[%d] Suppressed violations", report.suppressedViolations.size())
      output.infoln("[%d] Violations", report.violations.size())

      if (report.configurationErrors.size() > 0 || report.processingErrors.size() > 0 || report.suppressedViolations.size() > 0 || report.violations.size() > 0) {
        output.infoln("\nPMD analysis report")
        output.infoln textWriter.toString()
      }
    }
  }
}
