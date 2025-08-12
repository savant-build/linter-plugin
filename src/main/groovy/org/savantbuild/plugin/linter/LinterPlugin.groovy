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

import java.io.FileWriter
import java.io.PrintWriter
import java.nio.charset.StandardCharsets
import java.nio.file.Files

import com.puppycrawl.tools.checkstyle.Checker
import com.puppycrawl.tools.checkstyle.ConfigurationLoader
import com.puppycrawl.tools.checkstyle.PropertiesExpander
import com.puppycrawl.tools.checkstyle.XMLLogger
import com.puppycrawl.tools.checkstyle.api.AuditEvent
import com.puppycrawl.tools.checkstyle.api.AuditListener
import com.puppycrawl.tools.checkstyle.api.AutomaticBean
import com.puppycrawl.tools.checkstyle.api.Configuration

import edu.umd.cs.findbugs.BugCollection
import edu.umd.cs.findbugs.BugReporter
import edu.umd.cs.findbugs.DetectorFactoryCollection
import edu.umd.cs.findbugs.FindBugs2
import edu.umd.cs.findbugs.HTMLBugReporter
import edu.umd.cs.findbugs.PrintingBugReporter
import edu.umd.cs.findbugs.TextUIBugReporter
import edu.umd.cs.findbugs.XMLBugReporter
import edu.umd.cs.findbugs.config.UserPreferences

import net.sourceforge.pmd.PMDConfiguration
import net.sourceforge.pmd.PMDVersion
import net.sourceforge.pmd.PmdAnalysis
import net.sourceforge.pmd.lang.LanguageRegistry
import net.sourceforge.pmd.lang.rule.RulePriority
import net.sourceforge.pmd.lang.rule.RuleSetLoader
import net.sourceforge.pmd.renderers.HTMLRenderer
import net.sourceforge.pmd.renderers.TextRenderer

import org.savantbuild.dep.DependencyService
import org.savantbuild.dep.domain.Artifact
import org.savantbuild.dep.domain.Dependencies
import org.savantbuild.dep.domain.DependencyGroup
import org.savantbuild.dep.domain.License
import org.savantbuild.dep.domain.ReifiedArtifact
import org.savantbuild.domain.Project
import org.savantbuild.io.FileTools
import org.savantbuild.output.Output
import org.savantbuild.plugin.groovy.BaseGroovyPlugin
import org.savantbuild.runtime.RuntimeConfiguration

/**
 * Linter plugin supporting PMD, Checkstyle, and SpotBugs static analysis tools.
 *
 * @author Daniel DeGroff
 */
class LinterPlugin extends BaseGroovyPlugin {
  // Default configuration constants
  private static final String DEFAULT_INPUT_PATH = "src/main/java"
  private static final String DEFAULT_SOURCE_CLASSES_PATH = "build/classes/main"
  private static final String DEFAULT_TEST_CLASSES_PATH = "build/classes/test"
  private static final String DEFAULT_PMD_CACHE_PATH = "build/pmd.cache"
  private static final String DEFAULT_CHECKSTYLE_CACHE_FILE = "build/checkstyle.cache"
  
  // Default report settings
  private static final String DEFAULT_PMD_REPORT_NAME = "pmd-report"
  private static final String DEFAULT_CHECKSTYLE_REPORT_NAME = "checkstyle-report"
  private static final String DEFAULT_SPOTBUGS_REPORT_NAME = "spotbugs-report"
  
  // File extensions
  private static final String HTML_EXTENSION = ".html"
  private static final String XML_EXTENSION = ".xml"
  private static final String TXT_EXTENSION = ".txt"
  private static final String JAVA_EXTENSION = ".java"
  
  // Default analysis settings
  private static final String DEFAULT_LANGUAGE_VERSION = "17"
  private static final String DEFAULT_PMD_PRIORITY = "MEDIUM"
  private static final String DEFAULT_SPOTBUGS_EFFORT = "default"
  private static final String DEFAULT_SPOTBUGS_THRESHOLD = "medium"
  private static final String DEFAULT_REPORT_FORMAT = "xml"

  public LinterSettings settings = new LinterSettings()

  LinterPlugin(Project project, RuntimeConfiguration runtimeConfiguration, Output output) {
    super(project, runtimeConfiguration, output)
  }

  /**
   * Ensures the report directory exists and is ready for writing reports.
   */
  private void setupReportDirectory() {
    FileTools.prune(settings.reportDirectory)
    if (!Files.exists(settings.reportDirectory.parent)) {
      Files.createDirectories(settings.reportDirectory.parent)
    }
    Files.createDirectory(settings.reportDirectory)
  }

  /**
   * Writes a report to the specified file in the report directory.
   *
   * @param fileName the base name of the file (without extension)
   * @param extension the file extension to append
   * @param writer the StringWriter containing the report content
   */
  private void writeReport(String fileName, String extension, StringWriter writer) {
    String fileNameWithSuffix = fileName + extension
    Files.write(settings.reportDirectory.resolve(fileNameWithSuffix), writer.toString().getBytes(StandardCharsets.UTF_8))
  }

  /**
   * Logs a standardized configuration header for analysis tools.
   *
   * @param toolName the name of the analysis tool
   */
  private void logConfigurationHeader(String toolName) {
    output.infoln("\n${toolName} analysis configuration")
    output.infoln("===============================================")
  }

  /**
   * Logs a standardized analysis summary for analysis tools.
   *
   * @param toolName the name of the analysis tool
   */
  private void logAnalysisSummary(String toolName) {
    output.infoln("\n${toolName} analysis summary")
    output.infoln("===============================================")
  }

  /**
   * Runs PMD static analysis on the project's Java source code.
   * 
   * @param attributes Configuration map with the following supported options:
   *   - reportFormat: Output format (xml, html, text) - default: "xml"
   *   - reportFileName: Base name for report files - default: "pmd-report"
   *   - reportSuppressedViolations: Include suppressed violations - default: false
   *   - languageVersion: Java language version - default: "17"
   *   - inputPath: Source directory to analyze - default: "src/main/java"
   *   - sourceClassesPath: Compiled classes directory - default: "build/classes/main"
   *   - testClassesPath: Test classes directory - default: "build/classes/test"
   *   - cachePath: PMD cache file location - default: "build/pmd.cache"
   *   - minimumPriority: Minimum rule priority (LOW, MEDIUM, HIGH) - default: "MEDIUM"
   *   - ruleSets: Array of PMD ruleset files (required)
   *   - failOnViolations: Fail build on violations - default: true
   *   - customRuleDependencySpecs: List of custom rule dependencies - default: []
   */
  void pmd(Map attributes = [:]) {
    String reportFormat = attributes.getOrDefault("reportFormat", DEFAULT_REPORT_FORMAT)
    String reportFileName = attributes.getOrDefault("reportFileName", DEFAULT_PMD_REPORT_NAME)
    boolean reportSuppressedViolations = attributes.getOrDefault("reportSuppressedViolations", false)
    String languageVersion = attributes.getOrDefault("languageVersion", DEFAULT_LANGUAGE_VERSION)
    String inputPath = attributes.getOrDefault("inputPath", DEFAULT_INPUT_PATH)
    String sourceClassesPath = attributes.getOrDefault("sourceClassesPath", DEFAULT_SOURCE_CLASSES_PATH)
    String testClassesPath = attributes.getOrDefault("testClassesPath", DEFAULT_TEST_CLASSES_PATH)
    String cachePath = attributes.getOrDefault("cachePath", DEFAULT_PMD_CACHE_PATH)
    String minimumPriority = attributes.getOrDefault("minimumPriority", DEFAULT_PMD_PRIORITY)
    String[] ruleSets = attributes.getOrDefault("ruleSets", [])
    boolean failOnViolations = attributes.getOrDefault("failOnViolations", true)
    List<String> customRuleDependencySpecs = attributes.getOrDefault("customRuleDependencySpecs", [])
    
    // Load custom rules if specified
    def ruleClassLoader = RuleSetLoader.class.getClassLoader()
    if (customRuleDependencySpecs) {
      ruleClassLoader = getLoaderWithCustomRules(customRuleDependencySpecs, ruleClassLoader)
    }

    if (ruleSets.length == 0) {
      fail("You must specify one or more values for the [ruleSets] argument. It will look something like this:\n\npmd(ruleSets: [\"src/test/resources/pmd/ruleset.xml\"])")
    }

    setupReportDirectory()

    def config = getPMDConfiguration(cachePath, inputPath, sourceClassesPath, testClassesPath,
        languageVersion, minimumPriority, reportSuppressedViolations, reportFormat, reportFileName)

    def writers = [
        "html": new StringWriter(),
        "text": new StringWriter()
    ]

    def html = new HTMLRenderer()
    html.setWriter(writers.html)
    html.setShowSuppressedViolations(reportSuppressedViolations)

    def text = new TextRenderer()
    text.setWriter(writers.text)
    text.setShowSuppressedViolations(reportSuppressedViolations)

    def projectPath = project.directory.toAbsolutePath().toString() + "/"
    try (PmdAnalysis pmd = PmdAnalysis.create(config)) {
      for (ruleSet in ruleSets) {
        String ruleSetFileName = projectPath + ruleSet
        pmd.addRuleSet(pmd.newRuleSetLoader()
            .loadResourcesWith(ruleClassLoader)
            .loadFromResource(ruleSetFileName))
      }

      pmd.addRenderer(html)
      pmd.addRenderer(text)

      logConfigurationHeader("PMD")
      output.infoln("Language: Java %s", languageVersion)
      output.infoln("Minimum priority: %s", minimumPriority)
      output.infoln("Rulesets: %s", String.join(", ", ruleSets))

      var report = pmd.performAnalysisAndCollectReport()

      writeReport(reportFileName, HTML_EXTENSION, writers.html)
      writeReport(reportFileName, TXT_EXTENSION, writers.text)

      logAnalysisSummary("PMD")
      output.infoln("[%d] Configuration errors", report.configurationErrors.size())
      output.infoln("[%d] ProcessingErrors errors", report.processingErrors.size())
      output.infoln("[%d] Suppressed violations", report.suppressedViolations.size())
      output.infoln("[%d] Violations", report.violations.size())

      if (report.configurationErrors.size() > 0 || report.processingErrors.size() > 0 || (reportSuppressedViolations && report.suppressedViolations.size() > 0) || report.violations.size() > 0) {
        output.infoln("\nPMD analysis report")
        output.infoln("===============================================")
        output.infoln writers.text.toString()
      }

      if (report.configurationErrors.size() > 0 || report.processingErrors.size() > 0 || (failOnViolations && report.violations.size() > 0)) {
        fail("PMD analysis failed.")
      }
    }
  }

  private getPMDConfiguration(String cachePath, String inputPath, String sourceClassesPath, String testClassesPath,
                              String languageVersion, String minimumPriority, boolean reportSuppressedViolations,
                              String reportFormat, String reportFileName) {
    new PMDConfiguration().with {
      output.infoln("Using PMD version [%s]", PMDVersion.fullVersionName)
      setAnalysisCacheLocation(project.directory.resolve(cachePath).toAbsolutePath().toString());
      addInputPath(project.directory.resolve(inputPath))
      def auxClassPath = [
          sourceClassesPath,
          testClassesPath
      ].collect { f -> project.directory.resolve(f).toAbsolutePath().toString() }
          .join(File.pathSeparator)
      output.infoln("Adding aux classpath [%s]", auxClassPath)
      prependAuxClasspath(auxClassPath)

      setDefaultLanguageVersion(LanguageRegistry.PMD.getLanguageById("java").getVersion(languageVersion))
      setMinimumPriority(RulePriority.valueOf(minimumPriority))
      setShowSuppressedViolations(reportSuppressedViolations)
      setReportFormat(reportFormat)
      setReportFile(settings.reportDirectory.resolve((String) (reportFileName + "." + reportFormat)))
      it
    }
  }

  /**
   * Runs Checkstyle static analysis on the project's Java source code.
   * 
   * @param attributes Configuration map with the following supported options:
   *   - reportFileName: Base name for XML report file - default: "checkstyle-report"
   *   - configFile: Path to Checkstyle configuration file (required)
   *   - inputPath: Source directory to analyze - default: "src/main/java"
   *   - failOnViolations: Fail build on violations - default: true
   *   - cacheFile: Checkstyle cache file location - default: "build/checkstyle.cache"
   */
  void checkstyle(Map attributes = [:]) {
    String reportFileName = attributes.getOrDefault("reportFileName", DEFAULT_CHECKSTYLE_REPORT_NAME)
    String configFile = attributes.getOrDefault("configFile", null)
    String inputPath = attributes.getOrDefault("inputPath", DEFAULT_INPUT_PATH)
    boolean failOnViolations = attributes.getOrDefault("failOnViolations", true)
    String cacheFile = attributes.getOrDefault("cacheFile", DEFAULT_CHECKSTYLE_CACHE_FILE)
    
    if (!configFile) {
      fail("You must specify a [configFile] argument for Checkstyle. It should point to your checkstyle.xml configuration file.")
    }
    
    setupReportDirectory()
    
    def configPath = project.directory.resolve(configFile)
    if (!Files.exists(configPath)) {
      fail("Checkstyle configuration file not found: ${configPath}")
    }
    
    logConfigurationHeader("Checkstyle")
    output.infoln("Configuration file: %s", configFile)
    output.infoln("Input path: %s", inputPath)
    output.infoln("Cache file: %s", cacheFile)
    
    try {
      // Load configuration
      def properties = new Properties()
      def config = ConfigurationLoader.loadConfiguration(
          configPath.toAbsolutePath().toString(),
          new PropertiesExpander(properties))
      
      // Create checker
      def checker = new Checker()
      checker.setModuleClassLoader(Thread.currentThread().getContextClassLoader())
      checker.configure(config)
      
      // Set up cache
      if (cacheFile) {
        checker.setCacheFile(project.directory.resolve(cacheFile).toAbsolutePath().toString())
      }
      
      // Create XML report with proper XMLLogger
      def xmlReportPath = settings.reportDirectory.resolve(reportFileName + XML_EXTENSION)
      def xmlOutputStream = Files.newOutputStream(xmlReportPath)
      def xmlLogger = new XMLLogger(xmlOutputStream, AutomaticBean.OutputStreamOptions.CLOSE)
      checker.addListener(xmlLogger)
      
      // Add violation counter
      def violationCount = [count: 0] // Using a map so the closure can modify it
      def listener = new AuditListener() {
        void auditStarted(AuditEvent event) {}
        void auditFinished(AuditEvent event) {}
        void fileStarted(AuditEvent event) {}
        void fileFinished(AuditEvent event) {}
        void addError(AuditEvent event) { 
          violationCount.count++
          output.debugln("Checkstyle violation: ${event.getMessage()}")
        }
        void addException(AuditEvent event, Throwable throwable) {}
      }
      checker.addListener(listener)
      
      // Process files
      def files = collectJavaFiles(inputPath)
      checker.process(files)
      
      logAnalysisSummary("Checkstyle")
      output.infoln("[%d] Files processed", files.size())
      output.infoln("[%d] Violations found", violationCount.count)
      
      if (violationCount.count > 0) {
        output.infoln("\nCheckstyle violations detected.")
        if (failOnViolations) {
          fail("Checkstyle analysis failed with ${violationCount.count} violations.")
        }
      }
      
      checker.destroy()
      
    } catch (Exception e) {
      fail("Checkstyle analysis failed: ${e.message}")
    }
  }
  
  /**
   * Runs SpotBugs (successor to FindBugs) static analysis on the project's compiled bytecode.
   * 
   * @param attributes Configuration map with the following supported options:
   *   - reportFileName: Base name for report file - default: "spotbugs-report"
   *   - reportFormat: Output format (html, xml, text) - default: "html"
   *   - classesPath: Compiled classes directory - default: "build/classes/main"
   *   - auxClassPath: Auxiliary classpath directory - default: "build/classes/test"
   *   - effort: Analysis effort level (min, default, max) - default: "default"
   *   - threshold: Bug priority threshold (low, medium, high) - default: "medium"
   *   - failOnBugs: Fail build on bugs found - default: true
   *   - includeFilter: Path to include filter file - default: null
   *   - excludeFilter: Path to exclude filter file - default: null
   */
  void spotbugs(Map attributes = [:]) {
    String reportFileName = attributes.getOrDefault("reportFileName", DEFAULT_SPOTBUGS_REPORT_NAME)
    String reportFormat = attributes.getOrDefault("reportFormat", "html")
    String classesPath = attributes.getOrDefault("classesPath", DEFAULT_SOURCE_CLASSES_PATH)
    String auxClassPath = attributes.getOrDefault("auxClassPath", DEFAULT_TEST_CLASSES_PATH)
    String effort = attributes.getOrDefault("effort", DEFAULT_SPOTBUGS_EFFORT) // min, default, or max
    String threshold = attributes.getOrDefault("threshold", DEFAULT_SPOTBUGS_THRESHOLD) // low, medium, high
    boolean failOnBugs = attributes.getOrDefault("failOnBugs", true)
    String includeFilter = attributes.getOrDefault("includeFilter", null)
    String excludeFilter = attributes.getOrDefault("excludeFilter", null)
    
    def classesDir = project.directory.resolve(classesPath)
    if (!Files.exists(classesDir)) {
      output.warningln("Classes directory does not exist: %s. Skipping SpotBugs analysis.", classesDir)
      return
    }
    
    setupReportDirectory()
    
    logConfigurationHeader("SpotBugs")
    output.infoln("Classes path: %s", classesPath)
    output.infoln("Aux classpath: %s", auxClassPath)
    output.infoln("Effort level: %s", effort)
    output.infoln("Threshold: %s", threshold)
    output.infoln("Report format: %s", reportFormat)
    
    try {
      // Create SpotBugs project
      def spotbugsProject = new edu.umd.cs.findbugs.Project()
      spotbugsProject.setProjectName(project.name)
      
      // Add classes to analyze
      spotbugsProject.addFile(classesDir.toAbsolutePath().toString())
      
      // Add auxiliary classpath
      if (auxClassPath) {
        def auxPath = project.directory.resolve(auxClassPath)
        if (Files.exists(auxPath)) {
          spotbugsProject.addAuxClasspathEntry(auxPath.toAbsolutePath().toString())
        }
      }
      
      // Configure FindBugs2 engine
      def findBugs = new FindBugs2()
      findBugs.setProject(spotbugsProject)
      
      // Set up detectors
      findBugs.setDetectorFactoryCollection(DetectorFactoryCollection.instance())
      
      // Configure user preferences
      def userPrefs = UserPreferences.createDefaultUserPreferences()
      userPrefs.setEffort(effort)
      findBugs.setUserPreferences(userPrefs)
      
      // Set up bug reporter
      def reportPath = settings.reportDirectory.resolve(reportFileName + "." + reportFormat)
      BugReporter reporter = createSpotBugsReporter(reportFormat, reportPath, spotbugsProject)
      
      findBugs.setBugReporter(reporter)
      
      // Set filters if provided
      if (includeFilter) {
        def filterPath = project.directory.resolve(includeFilter)
        if (Files.exists(filterPath)) {
          findBugs.addFilter(filterPath.toAbsolutePath().toString(), true)
        }
      }
      
      if (excludeFilter) {
        def filterPath = project.directory.resolve(excludeFilter)
        if (Files.exists(filterPath)) {
          findBugs.addFilter(filterPath.toAbsolutePath().toString(), false)
        }
      }
      
      // Set threshold
      def thresholdMap = ["low": 3, "medium": 2, "high": 1]
      reporter.setPriorityThreshold(thresholdMap.get(threshold, 2))
      
      // Execute analysis
      findBugs.execute()
      
      // Get results from the reporter
      def bugCollection = reporter.getBugCollection()
      def bugCount = bugCollection ? bugCollection.getCollection().size() : 0
      
      logAnalysisSummary("SpotBugs")
      output.infoln("[%d] Classes analyzed", bugCollection ? bugCollection.getProjectStats().getNumClasses() : 0)
      output.infoln("[%d] Bugs found", bugCount)
      
      if (bugCount > 0) {
        output.infoln("\nSpotBugs found issues. See report: %s", reportPath)
        if (failOnBugs) {
          fail("SpotBugs analysis failed with ${bugCount} bugs found.")
        }
      }
      
      reporter.finish()
      
    } catch (Exception e) {
      fail("SpotBugs analysis failed: ${e.message}")
    }
  }
  
  /**
   * Collects all Java files from the specified input path.
   *
   * @param inputPath the directory path to search for Java files
   * @return a list of File objects representing Java files
   */
  private List<File> collectJavaFiles(String inputPath) {
    def sourcePath = project.directory.resolve(inputPath)
    def files = []
    if (Files.exists(sourcePath)) {
      Files.walk(sourcePath)
          .filter { path -> path.toString().endsWith(JAVA_EXTENSION) }
          .forEach { path -> files.add(path.toFile()) }
    }
    return files
  }

  /**
   * Creates a SpotBugs reporter based on the specified format.
   *
   * @param reportFormat the format for the report (text, xml, html)
   * @param reportPath the path where the report should be written
   * @param spotbugsProject the SpotBugs project instance
   * @return the configured BugReporter
   */
  private BugReporter createSpotBugsReporter(String reportFormat, def reportPath, def spotbugsProject) {
    BugReporter reporter
    
    if (reportFormat == "text") {
      reporter = new PrintingBugReporter()
      reporter.setOutputStream(new PrintStream(Files.newOutputStream(reportPath)))
    } else {
      // XML format (used for both XML and HTML since HTMLBugReporter has complex constructors)
      def xmlReporter = new XMLBugReporter(spotbugsProject)
      xmlReporter.setOutputStream(new PrintStream(Files.newOutputStream(reportPath)))
      reporter = xmlReporter
    }
    
    return reporter
  }

  private getLoaderWithCustomRules(List<String> customRuleDependencySpecs,
                                   ClassLoader ruleClassLoader) {
    def ourOwnRuleGroup = "rules"
    // without the traversal rules, when we resolve below, Savant won't know which groups to resolve
    def traversalRules = new DependencyService.TraversalRules().with(ourOwnRuleGroup,
        new DependencyService.TraversalRules.GroupTraversalRule(false, false))
    // a "fake" or artificial root, just to resolve the custom rule JARs

    def root = new ReifiedArtifact("org.savantbuild.plugin:linter-custom-rule:0.0.0",
        License.Licenses.get("ApacheV2_0"))

    // When an app uses the linter plugin, they've supplied a list of dependency specs, each spec
    // pointing to custom rules
    Artifact[] dependencies = customRuleDependencySpecs.collect { spec ->
      new Artifact(spec)
    }.toArray(new Artifact[0])

    // we are just using the project to get this dependency. This does NOT mean we are inter-tangled
    // with the project's dependency graph
    def dependencyService = project.dependencyService
    def dependencyGraph = dependencyService.buildGraph(root,
        new Dependencies(new DependencyGroup(ourOwnRuleGroup, false, dependencies)),
        project.workflow)
    def artifactGraph = dependencyService.reduce(dependencyGraph)
    def resolvedGraph = dependencyService.resolve(artifactGraph,
        project.workflow,
        traversalRules)

    URL[] ruleJARURLs = resolvedGraph.toClasspath()
        .paths
        .collect { p -> p.toUri().toURL() }.toArray(new URL[0])

    output.infoln("Including the following custom rules in the PMD classpath: [%s]",
        ruleJARURLs)

    new URLClassLoader(ruleJARURLs, ruleClassLoader)
  }
}
