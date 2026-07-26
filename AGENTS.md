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

| Path                                      | Contents                               |
|-------------------------------------------|----------------------------------------|
| `src/main/kotlin/.../nullmarked`          | `NullMarkedPlugin` entry point         |
| `src/main/kotlin/.../nullmarked/tasks`    | Gradle task types                      |
| `src/main/kotlin/.../nullmarked/dsl`      | `nullmarked { }` extension and specs   |
| `src/main/kotlin/.../nullmarked/internal` | internal plugin logic                  |
| `src/test/kotlin`                         | Unit + Gradle TestKit functional tests |
| `src/integrationTest/kotlin`              | Tests for Gradle compatibility         |
| `build.gradle.kts`                        | Project and build config               |

## Architecture

- `NullMarkedPlugin` wires everything once the `java` plugin is applied: registers `GeneratePackageInfo` and
  `VerifyPackageInfo` per source set, and adds the JSpecify dependency unless the source set is disabled or the build
  script declares one itself.
- `GeneratePackageInfo` writes one `package-info.java` per package that has `.java` files but none of its own.
- `VerifyPackageInfo` runs before compilation and fails listing every package still missing one.
- `NullMarkedExtension` is the `nullmarked { }` DSL, with per-source-set overrides (`NullMarkedSourceSetSpec`) and
  package rules (`NullMarkedPackagesSpec`). Rules keep declaration order and the last one matching a package wins
  (`PackageFilter`); unmatched packages are processed. Identifiers use ArchUnit's syntax, parsed by `PackagePattern`.
- `PackageRule` is internal, so tasks take rules as encoded `"-org.acme.."` / `"+org.acme.api"` strings - build scripts
  only ever see the DSL.
- Plugin version is baked into a resource (`generatePluginProperties` in `build.gradle.kts`, written under the 
  `internal` subpackage) and read back to stamp the generated file header.

## Coding Rules

- Let `spotlessApply` handle formatting - never format manually.
- Follow existing code patterns and naming conventions.
