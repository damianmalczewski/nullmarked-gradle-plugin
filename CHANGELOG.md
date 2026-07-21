# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog][keepachangelog], and this project adheres to [Semantic Versioning][semver].

## [Unreleased]

### Fixed

- Restore old format of `package-info.java` (an empty line between header and code).

## [0.2.0] - 2026-07-21

### Added

- Add option to pass a full dependency to `jspecifyVersion` (to support forks if necessary).

### Changed

- Generate `package-info.java` files incrementally; only stale files are pruned and only changed/missing ones are
  (re)written, instead of wiping and regenerating the whole output directory each run.

## [0.1.0] - 2026-07-18

### Added

- Add `io.github.malczuuu.nullmarked` plugin generating a `@NullMarked`-annotated `package-info.java` for every
  non-empty package of the `main` source set that does not declare its own.
- Add `org.jspecify:jspecify` as a `compileOnly` dependency unless the build script already declares it in
  `compileOnly`, `implementation`, `api` or `compileOnlyApi`.

[keepachangelog]: https://keepachangelog.com/en/1.1.0/

[semver]: https://semver.org/spec/v2.0.0.html
