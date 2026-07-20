# Agent Instructions - Gradle NullMarked Plugin

Gradle plugin (`io.github.malczuuu.nullmarked`) applying JSpecify's `@NullMarked` convention: generates
`@NullMarked`-annotated `package-info.java` for packages missing one, and auto-adds the `org.jspecify:jspecify` as
`compileOnly` dependency.

## Build & Validate

- Default tasks: `./gradlew` runs `spotlessApply build` (format, compile, unit test).
- `./gradlew test --tests "*.PackagePatternTest"` - single test class/method.
- `./gradlew integrationTest` - TestKit compat tests; `-Pcompat.gradle.version=9.0.0` targets a specific Gradle.
- `./gradlew allTest` - unit + integration.
- Java 17 toolchain builds the plugin; compiled output targets Java 8 bytecode.

## Project Layout

| Path                             | Contents                               |
|----------------------------------|----------------------------------------|
| `src/main/kotlin/.../nullmarked` | Main plugin sources                    |
| `src/test/kotlin`                | Unit + Gradle TestKit functional tests |
| `src/integrationTest/kotlin`     | Tests for Gradle compatibility         |
| `build.gradle.kts`               | Project and build config               |

## Architecture

- `NullMarkedPlugin` wires everything once the `java` plugin is applied: registers `generatePackageInfo`
  (`GeneratePackageInfoTask`) and adds the JSpecify dependency unless already declared in `api`/`compileOnlyApi`/
  `compileOnly`/`implementation`.
- `GeneratePackageInfoTask` scans hand-written `main` source dirs (its own output dir excluded), writes one
  `package-info.java` per package that has `.java` files but none of its own, minus `excludedPackages` matches.
  Disabling (`enabled = false`) only deletes previously generated output - it doesn't skip the task.
  `excludedPackages` uses ArchUnit's package-identifier syntax (`org.acme..`, `..internal..`, `*`), parsed by
  `PackagePattern`.
- Plugin version is baked into a resource (`generatePluginProperties` task in `build.gradle.kts`) and read back by
  `GeneratePackageInfoTask` to stamp the generated file header.

## Coding Rules

- Let `spotlessApply` handle formatting - never format manually.
- Follow existing code patterns and naming conventions.
