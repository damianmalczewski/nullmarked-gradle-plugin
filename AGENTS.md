# Agent Instructions - NullMarked Gradle Plugin

Gradle plugin (`io.github.malczuuu.nullmarked`) applying JSpecify's `@NullMarked` convention: generates
`@NullMarked`-annotated `package-info.java` for packages missing one, and auto-adds the `org.jspecify:jspecify` as
`compileOnly` dependency.

## Build & Validate

- Default tasks: `./gradlew` runs `spotlessApply build` (format, compile, unit test).
- `./gradlew test --tests "*.PackagePatternTest"` - single test class/method.
- `./gradlew integrationTest` - TestKit compat tests; `-Pcompat.gradle.version=9.0.0` targets a specific Gradle. Must
  include a test project with every feature turned on at once, kept mirrored test-for-test between
  `KotlinDslCompatibilityTest` and `GroovyDslCompatibilityTest`.
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
- `VerifyPackageInfo` runs before compilation and fails listing every package still missing one. Also can inspect the
  used annotations, depending on verification mode.
- `NullMarkedExtension` is the `nullmarked { }` DSL, with:
  - `NullMarkedSourceSetSpec` - per-source-set overrides,
  - `NullMarkedPackagesSpec` - package selection rules,
  - `NullMarkedVerifySpec` - `package-info.java` verification mode.

## Coding Rules

- Let `spotlessApply` handle formatting - never format manually.
- Follow existing code patterns and naming conventions.
