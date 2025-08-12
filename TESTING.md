# Linter Plugin Testing Guide

This guide explains how to test the Savant Linter Plugin with PMD, Checkstyle, and SpotBugs support.

## Prerequisites

1. Build the plugin first:
   ```bash
   savant compile jar
   ```

2. Install the plugin locally:
   ```bash
   savant int
   ```

## Testing PMD

PMD analyzes source code for potential bugs, code style violations, and other issues.

### Basic PMD Configuration

Create a PMD ruleset file (e.g., `config/pmd/ruleset.xml`):

```xml
<?xml version="1.0"?>
<ruleset name="Custom Rules"
         xmlns="http://pmd.sourceforge.net/ruleset/2.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://pmd.sourceforge.net/ruleset/2.0.0 
         https://pmd.sourceforge.io/ruleset_2_0_0.xsd">
    
    <description>Custom PMD rules for the project</description>
    
    <!-- Include basic error prone rules -->
    <rule ref="category/java/errorprone.xml/AvoidBranchingStatementAsLastInLoop"/>
    <rule ref="category/java/errorprone.xml/AvoidDecimalLiteralsInBigDecimalConstructor"/>
    <rule ref="category/java/errorprone.xml/AvoidMultipleUnaryOperators"/>
    <rule ref="category/java/errorprone.xml/AvoidUsingOctalValues"/>
    <rule ref="category/java/errorprone.xml/BrokenNullCheck"/>
    <rule ref="category/java/errorprone.xml/ClassCastExceptionWithToArray"/>
    <rule ref="category/java/errorprone.xml/MisplacedNullCheck"/>
    
    <!-- Include basic best practices -->
    <rule ref="category/java/bestpractices.xml/AvoidReassigningParameters"/>
    <rule ref="category/java/bestpractices.xml/AvoidStringBufferField"/>
    <rule ref="category/java/bestpractices.xml/AvoidUsingHardCodedIP"/>
    <rule ref="category/java/bestpractices.xml/CheckResultSet"/>
    
    <!-- Include unused code rules -->
    <rule ref="category/java/bestpractices.xml/UnusedFormalParameter"/>
    <rule ref="category/java/bestpractices.xml/UnusedLocalVariable"/>
    <rule ref="category/java/bestpractices.xml/UnusedPrivateField"/>
    <rule ref="category/java/bestpractices.xml/UnusedPrivateMethod"/>
</ruleset>
```

### Running PMD in build.savant

```groovy
// Load the linter plugin
linter = loadPlugin(id: "org.savantbuild.plugin:linter:2.0.3")

target(name: "pmd", description: "Run PMD analysis") {
  linter.pmd(
    ruleSets: ["config/pmd/ruleset.xml"],
    inputPath: "src/main/java",
    languageVersion: "17",
    minimumPriority: "MEDIUM",
    failOnViolations: true,
    reportFileName: "pmd-report"
  )
}
```

## Testing Checkstyle

Checkstyle enforces coding standards and style conventions.

### Basic Checkstyle Configuration

Create a Checkstyle configuration file (e.g., `config/checkstyle/checkstyle.xml`):

```xml
<?xml version="1.0"?>
<!DOCTYPE module PUBLIC
          "-//Checkstyle//DTD Checkstyle Configuration 1.3//EN"
          "https://checkstyle.org/dtds/configuration_1_3.dtd">

<module name="Checker">
    <!-- Configure file encoding -->
    <property name="charset" value="UTF-8"/>
    <property name="fileExtensions" value="java, properties, xml"/>
    
    <!-- Checks for Size Violations -->
    <module name="FileLength">
        <property name="max" value="2000"/>
    </module>
    
    <!-- Checks for whitespace -->
    <module name="FileTabCharacter"/>
    
    <module name="TreeWalker">
        <!-- Checks for Naming Conventions -->
        <module name="ConstantName"/>
        <module name="LocalFinalVariableName"/>
        <module name="LocalVariableName"/>
        <module name="MemberName"/>
        <module name="MethodName"/>
        <module name="PackageName"/>
        <module name="ParameterName"/>
        <module name="StaticVariableName"/>
        <module name="TypeName"/>
        
        <!-- Checks for imports -->
        <module name="AvoidStarImport"/>
        <module name="IllegalImport"/>
        <module name="RedundantImport"/>
        <module name="UnusedImports"/>
        
        <!-- Checks for Size Violations -->
        <module name="LineLength">
            <property name="max" value="120"/>
        </module>
        <module name="MethodLength">
            <property name="max" value="150"/>
        </module>
        <module name="ParameterNumber">
            <property name="max" value="7"/>
        </module>
        
        <!-- Checks for whitespace -->
        <module name="EmptyForIteratorPad"/>
        <module name="GenericWhitespace"/>
        <module name="MethodParamPad"/>
        <module name="NoWhitespaceAfter"/>
        <module name="NoWhitespaceBefore"/>
        <module name="OperatorWrap"/>
        <module name="ParenPad"/>
        <module name="TypecastParenPad"/>
        <module name="WhitespaceAfter"/>
        <module name="WhitespaceAround"/>
        
        <!-- Modifier Checks -->
        <module name="ModifierOrder"/>
        <module name="RedundantModifier"/>
        
        <!-- Checks for blocks -->
        <module name="AvoidNestedBlocks"/>
        <module name="EmptyBlock"/>
        <module name="LeftCurly"/>
        <module name="NeedBraces"/>
        <module name="RightCurly"/>
        
        <!-- Checks for common coding problems -->
        <module name="EmptyStatement"/>
        <module name="EqualsHashCode"/>
        <module name="IllegalInstantiation"/>
        <module name="InnerAssignment"/>
        <module name="MissingSwitchDefault"/>
        <module name="MultipleVariableDeclarations"/>
        <module name="SimplifyBooleanExpression"/>
        <module name="SimplifyBooleanReturn"/>
        
        <!-- Checks for class design -->
        <module name="FinalClass"/>
        <module name="HideUtilityClassConstructor"/>
        <module name="InterfaceIsType"/>
        <module name="VisibilityModifier"/>
    </module>
</module>
```

### Running Checkstyle in build.savant

```groovy
target(name: "checkstyle", description: "Run Checkstyle analysis") {
  linter.checkstyle(
    configFile: "config/checkstyle/checkstyle.xml",
    inputPath: "src/main/java",
    failOnViolations: true,
    reportFileName: "checkstyle-report"
  )
}
```

## Testing SpotBugs

SpotBugs analyzes bytecode to find bugs and potential issues.

### Basic SpotBugs Configuration

Create a SpotBugs filter file to exclude certain warnings (optional, e.g., `config/spotbugs/exclude.xml`):

```xml
<?xml version="1.0" encoding="UTF-8"?>
<FindBugsFilter>
    <!-- Exclude specific bug patterns -->
    <Match>
        <Bug pattern="DM_CONVERT_CASE"/>
    </Match>
    
    <!-- Exclude generated code -->
    <Match>
        <Class name="~.*\.Generated.*"/>
    </Match>
    
    <!-- Exclude test classes from certain checks -->
    <Match>
        <Class name="~.*Test"/>
        <Bug pattern="DM_DEFAULT_ENCODING"/>
    </Match>
</FindBugsFilter>
```

### Running SpotBugs in build.savant

**Important**: SpotBugs requires compiled classes, so you must compile your code first.

```groovy
target(name: "spotbugs", description: "Run SpotBugs analysis", dependsOn: ["compile"]) {
  linter.spotbugs(
    classesPath: "build/classes/main",
    auxClassPath: "build/classes/test",
    effort: "default",  // min, default, or max
    threshold: "medium", // low, medium, or high
    reportFormat: "html",
    excludeFilter: "config/spotbugs/exclude.xml", // optional
    failOnBugs: true,
    reportFileName: "spotbugs-report"
  )
}
```

## Complete Example build.savant

Here's a complete example showing all three tools:

```groovy
project(group: "com.example", name: "my-project", version: "1.0.0") {
  // ... project configuration ...
  
  dependencies {
    // ... your dependencies ...
  }
}

// Load plugins
groovy = loadPlugin(id: "org.savantbuild.plugin:groovy:2.0.0")
linter = loadPlugin(id: "org.savantbuild.plugin:linter:2.0.3")

target(name: "compile", description: "Compiles the project") {
  groovy.compile()
}

target(name: "pmd", description: "Run PMD analysis") {
  linter.pmd(
    ruleSets: ["config/pmd/ruleset.xml"],
    inputPath: "src/main/java",
    failOnViolations: true
  )
}

target(name: "checkstyle", description: "Run Checkstyle analysis") {
  linter.checkstyle(
    configFile: "config/checkstyle/checkstyle.xml",
    inputPath: "src/main/java",
    failOnViolations: true
  )
}

target(name: "spotbugs", description: "Run SpotBugs analysis", dependsOn: ["compile"]) {
  linter.spotbugs(
    classesPath: "build/classes/main",
    effort: "default",
    threshold: "medium",
    failOnBugs: true
  )
}

target(name: "lint", description: "Run all linters", dependsOn: ["compile"]) {
  // Run all three linters
  linter.pmd(ruleSets: ["config/pmd/ruleset.xml"])
  linter.checkstyle(configFile: "config/checkstyle/checkstyle.xml")
  linter.spotbugs(classesPath: "build/classes/main")
}
```

## Testing the Plugin

The plugin includes comprehensive tests that verify all three linters work correctly:

1. **Test Structure**: Test data files are located in `src/test/java/` with intentional violations for each tool to detect.

2. **Running Tests**: 
   ```bash
   # Run all tests
   sb test
   
   # Or just compile and build
   sb jar
   ```

3. **Manual Testing**: You can test the linters on any Java project by:
   - Adding the plugin to your `build.savant`
   - Creating configuration files as shown in the examples above
   - Running the linter targets

## Reports

All reports are generated in the `build/test-linter-reports` directory by default:

- PMD: `pmd-report.html` and `pmd-report.txt`
- Checkstyle: `checkstyle-report.xml`  
- SpotBugs: `spotbugs-report.html` (or `.xml`/`.text` based on format)

## Troubleshooting

### PMD Issues
- If custom rules aren't loading, check that the dependency specs are correct
- Ensure the ruleset XML is well-formed and uses the correct schema

### Checkstyle Issues  
- If Checkstyle fails to load configuration, verify the XML is valid
- Check that the DTD reference in the configuration file is correct
- Some checks may require additional configuration properties

### SpotBugs Issues
- If SpotBugs reports no classes analyzed, ensure the code is compiled first
- Check that the classes directory path is correct
- For auxiliary classpath issues, verify that dependency JARs are available
- SpotBugs requires Java 11+ to run but can analyze older Java code

### General Issues
- Check the Savant build output for detailed error messages
- Verify that all required dependencies are resolved
- Ensure configuration files use absolute or project-relative paths correctly
- Reports directory must be writable

## Parameter Reference

### PMD Parameters
- `ruleSets` (required): Array of ruleset file paths
- `inputPath`: Source directory to analyze (default: "src/main/java")
- `languageVersion`: Java version (default: "17")
- `minimumPriority`: HIGHEST, HIGH, MEDIUM, LOW, LOWEST (default: "MEDIUM")
- `reportFormat`: Output format (default: "xml")
- `reportFileName`: Base name for report files (default: "pmd-report")
- `failOnViolations`: Fail build on violations (default: true)
- `customRuleDependencySpecs`: List of dependency specs for custom rules

### Checkstyle Parameters
- `configFile` (required): Path to Checkstyle configuration XML
- `inputPath`: Source directory to analyze (default: "src/main/java")
- `reportFileName`: Base name for report file (default: "checkstyle-report")
- `failOnViolations`: Fail build on violations (default: true)
- `cacheFile`: Path to cache file (default: "build/checkstyle.cache")

### SpotBugs Parameters
- `classesPath`: Compiled classes directory (default: "build/classes/main")
- `auxClassPath`: Additional classpath (default: "build/classes/test")
- `effort`: Analysis effort - "min", "default", or "max" (default: "default")
- `threshold`: Bug threshold - "low", "medium", or "high" (default: "medium")
- `reportFormat`: Output format - "html", "xml", or "text" (default: "html")
- `reportFileName`: Base name for report file (default: "spotbugs-report")
- `includeFilter`: Path to include filter XML (optional)
- `excludeFilter`: Path to exclude filter XML (optional)
- `failOnBugs`: Fail build on bugs found (default: true)