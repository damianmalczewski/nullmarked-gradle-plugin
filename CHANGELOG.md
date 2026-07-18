# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog][keepachangelog], and this project adheres to [Semantic Versioning][semver].

## [Unreleased]

### Added

- Add `io.github.malczuuu.nullmarked` plugin generating a `@NullMarked`-annotated `package-info.java` for every
  non-empty package of the `main` source set that does not declare its own.
- Add `org.jspecify:jspecify` as a `compileOnly` dependency unless the build script already declares it in
  `compileOnly`, `implementation`, `api` or `compileOnlyApi`.
- Support Java 17 or newer and Gradle 9.0 or newer.

[keepachangelog]: https://keepachangelog.com/en/1.1.0/

[semver]: https://semver.org/spec/v2.0.0.html
