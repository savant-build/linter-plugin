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

import java.nio.file.Path

import org.savantbuild.domain.Project
import org.savantbuild.output.Output
import org.savantbuild.plugin.groovy.BaseGroovyPlugin
import org.savantbuild.runtime.RuntimeConfiguration
import net.sourceforge.pmd.PMDConfiguration
import net.sourceforge.pmd.PmdAnalysis;
/**
 * PMD plugin.
 *
 * @author Daniel DeGroff
 */
class LinterPlugin extends BaseGroovyPlugin {
  public LinterSettings settings = new LinterSettings()
  Path javaPath

  LinterPlugin(Project project, RuntimeConfiguration runtimeConfiguration, Output output) {
    super(project, runtimeConfiguration, output)
  }

  void pmd() {
    PMDConfiguration config = new PMDConfiguration();
    config.setDefaultLanguageVersion(LanguageRegistry.findLanguageByTerseName("java").getVersion("11"));
    config.addInputPath(Path.of("src/ main/ java"));
    config.prependClasspath("target/ classes");
    config.setMinimumPriority(RulePriority.HIGH);
    config.addRuleSet("rulesets/ java/ quickstart. xml");
    config.setReportFormat("xml");
    config.setReportFile("target/ pmd-report. xml");
    try (PmdAnalysis pmd = PmdAnalysis.create(config)) {
      // note: don't use `config` once a PmdAnalysis has been created.
      // optional: add more rulesets
      pmd.addRuleSet(pmd.newRuleSetLoader().loadFromResource("custom-ruleset. xml"));
      // optional: add more files
      pmd.files().addFile(Paths.get("src", "main", "more-java", "ExtraSource. java"));
      // optional: add more renderers
      pmd.addRenderer(renderer);
      pmd.performAnalysis();
    }
  }
}
