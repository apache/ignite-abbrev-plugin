# Apache Ignite abbreviation plugin

<a href="https://ignite.apache.org/"><img src="src/main/resources/META-INF/pluginIcon.svg" hspace="20"/></a>

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0)
![Java](https://img.shields.io/badge/Java-17-orange?logo=openjdk)
![Gradle](https://img.shields.io/badge/Gradle-8.5+-blue?logo=gradle)
![IntelliJ IDEA](https://img.shields.io/badge/IntelliJ_IDEA-2022.3+-orange?logo=intellij-idea)

IntelliJ Idea plugin that simplifies development with Apache Ignite [coding guidelines](https://cwiki.apache.org/confluence/display/IGNITE/Coding+Guidelines).

## Features

- **Code Style Validation**:
  - Abbreviation rules. Full list of abbreviations to enforce can be found in `src/main/resources/abbreviation.properties`
  - Naming conventions (classes, methods, variables, getters/setters)
  - Annotation placement
  - Brace placement
  - Empty line management
  - Comparison using equals()
  - Modifiers in interfaces

- **Instant Feedback**:
    - Real-time highlighting of violations
    - Quick-fix suggestions

- **Automated Corrections**:
    - One-click fixes for common issues

## Installation

1) Download the plugin (`.zip` file) from the [Releases page](https://github.com/dspavlov/ignite-abbrev-plugin/releases)
2) Install manually in IntelliJ IDEA following the [instructions](https://www.jetbrains.com/help/idea/managing-plugins.html#install_plugin_from_disk)

## Reporting issues
If you experience bugs or weird behavior please create an issue on the [bug tracker](https://issues.apache.org/jira)
or send it to [dev@ignite.apache.org](mailto:dev@ignite.apache.org).

## Plugin Version Compatibility
The latest release can be found on the [Releases page](https://github.com/dspavlov/ignite-abbrev-plugin/releases).
See [tags page](https://github.com/dspavlov/ignite-abbrev-plugin/tags) for full list of all available versions.

| Plugin Version | Minimal IntelliJ IDEA Version |
|----------------|-------------------------------|
| 2.6.0 - 3.0.1  | 2013.1                        |
| 4.0.0          | 2022.3                        |
