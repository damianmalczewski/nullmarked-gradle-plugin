# Agent Instructions - NullMarked Gradle Plugin

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
  (`GeneratePackageInfoTask`) and `verifyPackageInfo` (`VerifyPackageInfoTask`), and adds the JSpecify dependency unless
  already declared in `api`/`compileOnlyApi`/`compileOnly`/`implementation`.
- `GeneratePackageInfoTask` scans hand-written `main` source dirs (its own output dir excluded), writes one
  `package-info.java` per package that has `.java` files but none of its own, minus what the package rules skip.
  Disabling (`enabled = false`) only deletes previously generated output - it doesn't skip the task.
- Which packages are processed comes from `nullmarked { packages { exclude(...) include(...) } }`
  (`NullMarkedPackagesSpec`), also available per source set. Rules keep declaration order, source-set ones after the
  top-level ones, and the last rule matching a package wins (`PackageFilter`); unmatched packages are processed.
  Identifiers use ArchUnit's syntax (`org.acme..`, `..internal..`, `*`), parsed by `PackagePattern`. `PackageRule` is
  internal, so tasks take the rules encoded as `"-org.acme.."` / `"+org.acme.api"` strings in their `packageSelectionRules`
  input - build scripts only ever see the DSL.
- `VerifyPackageInfoTask` runs between generation and compilation (`compileJava` dependsOn it), scans the whole source
  set (generated output included) and throws `VerificationException` listing every package without a
  `package-info.java`. It writes a marker file under `build/tmp/nullmarked/<task>/` only so Gradle has an output to
  base up-to-date checks on. What it verifies never depends on the mode; its `verifyOnly` input only shapes the failure
  message. The flag otherwise just feeds task inputs: `generationEnabled = enabled && !verifyOnly`,
  `verificationEnabled = enabled`.
- Plugin version is baked into a resource (`generatePluginProperties` task in `build.gradle.kts`) and read back by
  `GeneratePackageInfoTask` to stamp the generated file header.

## Coding Rules

- Let `spotlessApply` handle formatting - never format manually.
- Follow existing code patterns and naming conventions.
