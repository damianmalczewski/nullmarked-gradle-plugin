# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog][keepachangelog], and this project adheres to [Semantic Versioning][semver].

## [Unreleased]

## [0.5.0] - 2026-07-27

### Added

- Validate usage of Gradle `8.3` or later explicitly, failing with an explicit message below it.

### Changed

- When `enabled = false`, plugin no longer adds the JSpecify dependency, making the plugin fully inert.
- Reject invalid package identifiers in `exclude(...)` and `include(...)`, pointing at the build script line rather than
  failing later at `:generatePackageInfo`.

## [0.4.1] - 2026-07-26

### Changed

- Defer configuration-time work in plugin application: the `compileOnly` configuration and the generation output
  directory are no longer resolved while the plugin is applied.

## [0.4.0] - 2026-07-25

### Added

- Add a `verifyPackageInfo` task per configured source set (`verifyTestPackageInfo`, etc.), run before compilation,
  failing the build with the list of packages that end up without a `package-info.java` - hand-written or generated.
- Add `verifyOnly` (top-level and per source set) to opt out of generated code: nothing is generated and only
  hand-written `package-info.java` files satisfy verification. Setting `enabled = false` turns both generation and
  verification off.

### Changed

- Replace `excludedPackages = listOf(...)` with a `packages { }` block offering `exclude(...)` and `include(...)`,
  available at the top level and per source set. Rules keep declaration order and the last one matching a package wins,
  so an `include` can carve an exception out of an earlier `exclude`; packages no rule matches are still processed. A
  source set's rules are evaluated after the top-level ones regardless of the order the blocks appear in.

  ```kotlin
  // before
  nullmarked {
      excludedPackages = listOf("..internal..")
  }

  // after
  nullmarked {
      packages {
          exclude("..internal..")
          include("com.acme.internal.api")
      }
  }
  ```

## [0.3.0] - 2026-07-25

### Added

- Add `nullmarked { sourceSet("...") }` to opt other source sets (e.g. `test`) into `package-info.java` generation and
  the auto-added `jspecify` dependency. The `main` source set is still processed by default.

## [0.2.1] - 2026-07-21

### Fixed

- Restore old format of `package-info.java` (an empty line between header and code).
- Exclude generated `package-info.java` files from `Javadoc` tasks; hand-written ones are still included.

## [0.2.0] - 2026-07-21

### Added

- Add option to pass a full dependency to `jspecifyVersion` (to support forks if necessary).

### Changed

- Generate `package-info.java` files incrementally; only stale files are pruned and only changed/missing ones are (re)
  written, instead of wiping and regenerating the whole output directory each run.

## [0.1.0] - 2026-07-18

### Added

- Add `io.github.malczuuu.nullmarked` plugin generating a `@NullMarked`-annotated `package-info.java` for every
  non-empty package of the `main` source set that does not declare its own.
- Add `org.jspecify:jspecify` as a `compileOnly` dependency unless the build script already declares it in
  `compileOnly`, `implementation`, `api` or `compileOnlyApi`.

[keepachangelog]: https://keepachangelog.com/en/1.1.0/

[semver]: https://semver.org/spec/v2.0.0.html
