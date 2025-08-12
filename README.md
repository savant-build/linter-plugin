# Savant Linter Plugin

A comprehensive linter plugin for the Savant build system that integrates three powerful Java static analysis tools:
- **PMD** - Source code analyzer for finding programming flaws
- **Checkstyle** - Coding standard and style checker  
- **SpotBugs** - Bytecode analyzer for finding bugs (successor to FindBugs)

## Features

- Unified integration of PMD, Checkstyle, and SpotBugs in your Savant build
- Configurable rule sets and thresholds for each tool
- Multiple report formats (HTML, XML, Text)
- Support for custom PMD rules via dependency resolution
- Fail-on-violation options for CI/CD integration
- Centralized report directory for all linter outputs

## Requirements

- Savant build system
- Java 11+ (for SpotBugs execution)
- Compiled classes for SpotBugs analysis

## Building the Plugin

```bash
# Clone the repository
git clone <repository-url>
cd linter-plugin

# Compile the plugin
sb compile

# Build the JAR
sb jar

# Run tests (comprehensive tests for PMD, Checkstyle, and SpotBugs)
sb test

# Install locally for testing
sb int
```

## Usage

Add the plugin to your project's `build.savant` file:

```groovy
// Load the linter plugin
linter = loadPlugin(id: "org.savantbuild.plugin:linter:2.0.3")

// PMD Example
target(name: "pmd", description: "Run PMD analysis") {
  linter.pmd(
    ruleSets: ["config/pmd/ruleset.xml"],
    inputPath: "src/main/java",
    languageVersion: "17",
    minimumPriority: "MEDIUM",
    failOnViolations: true
  )
}

// Checkstyle Example
target(name: "checkstyle", description: "Run Checkstyle analysis") {
  linter.checkstyle(
    configFile: "config/checkstyle/checkstyle.xml",
    inputPath: "src/main/java",
    failOnViolations: true
  )
}

// SpotBugs Example (requires compilation)
target(name: "spotbugs", description: "Run SpotBugs analysis", dependsOn: ["compile"]) {
  linter.spotbugs(
    classesPath: "build/classes/main",
    effort: "default",
    threshold: "medium",
    failOnBugs: true
  )
}
```

## Configuration Examples

Example configuration files are provided in the `examples/config/` directory:
- `pmd/basic-ruleset.xml` - Basic PMD rules
- `checkstyle/basic-checkstyle.xml` - Basic Checkstyle configuration
- `spotbugs/exclude-filter.xml` - SpotBugs exclusion filter

## Reports

All reports are generated in `build/linter-reports/` by default:
- PMD: `pmd-report.html`, `pmd-report.txt`, `pmd-report.xml`
- Checkstyle: `checkstyle-report.xml`
- SpotBugs: `spotbugs-report.html` (or `.xml`/`.txt` based on format)

## Development Notes

### Known Issues

1. **Dependency Conflicts**: The plugin manages several dependency conflicts between the three tools:
   - Different versions of `slf4j-api` 
   - Conflicting `checker-qual` versions
   - Multiple `jsr305` versions
   
   These are handled via exclusions in `build.savant`.

2. **SpotBugs TextUIBugReporter**: The abstract `TextUIBugReporter` class cannot be instantiated directly. The plugin uses `PrintingBugReporter` for text output instead.

3. **Test Suite**: The test suite includes 12 comprehensive tests across three classes:
   - CheckstylePluginTest: 4 tests (basic usage, custom report names, configuration validation)
   - PMDPluginTest: 2 tests (basic usage, missing ruleset handling) 
   - SpotBugsPluginTest: 6 tests (basic usage, formats, effort levels, filters, error conditions)
   
   Tests use test data in `src/test/java/org/savantbuild/plugin/linter/testdata/` and work without a separate test project.

### Building from Source

The plugin uses Savant's semantic version mapping for Maven dependencies with non-standard versions. If you encounter version-related build errors, add appropriate mappings in `build.savant`:

```groovy
semanticVersions {
  mapping(id: "group:artifact:non-semantic-version", version: "semantic.version.here")
}
```

### Adding New Linters

To add support for additional linters:
1. Add the dependency to `build.savant`
2. Import necessary classes in `LinterPlugin.groovy`
3. Create a new method following the pattern of existing linters
4. Update documentation and examples

## Testing

See `TESTING.md` for comprehensive testing instructions and examples.

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Ensure the plugin builds: `sb compile jar`
5. Add tests if applicable
6. Submit a pull request

## License

Copyright (c) 2024, Inversoft Inc., All Rights Reserved

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
