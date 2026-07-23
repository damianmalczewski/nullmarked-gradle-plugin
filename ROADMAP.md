# Roadmap

Candidate features for `io.github.malczuuu.nullmarked`, roughly ordered by how soon they're worth doing. Nothing
here is committed to a release; it's a backlog to prioritize against, not a promise.

## Near term

- **Multi source-set support.** `generatePackageInfo` only scans the `main` source set today
  (`NullMarkedPlugin.kt` hardcodes `SourceSet.MAIN_SOURCE_SET_NAME`). Extending generation to `test`,
  `integrationTest`, and other custom source sets - each with its own enabled/excludedPackages override - would let
  test code opt into the same convention instead of staying unmarked by default.
- **Explicit `@NullUnmarked` for excluded packages.** `excludedPackages` currently just skips generation, leaving
  those packages with no annotation at all - indistinguishable from "nobody has looked at this yet." Generating an
  explicit `@NullUnmarked` `package-info.java` for excluded packages instead would make the exclusion visible in
  source and in Javadoc, not just in `build.gradle.kts`.
- **Verify-only mode.** A `verifyPackageInfo` task that fails the build listing packages missing a `package-info.java`
  instead of generating one. Useful for maintainers who want the convention enforced in CI but require contributors to
  hand-write the file (e.g. to add real package documentation) rather than relying on generated output.

## Mid term

- **Auto-detect NullAway/Error Prone.** When `net.ltgt.errorprone` and NullAway are on the classpath, automatically
  wire the `NullAway:OnlyNullMarked` and `NullAway:JSpecifyMode` options (what `example/build.gradle.kts` does by
  hand today). Removes the most common piece of boilerplate paired with this plugin, opt-out via the extension for
  anyone who wants different NullAway options. See [`example/`](example) for what's currently manual.
- **Per-source-set `jspecifyVersion`/`excludedPackages` overrides**, once multi-source-set support lands - a
  monorepo module might want stricter exclusions in `test` than in `main`.
- **Generation report.** Write a small summary to `build/reports/nullmarked/` listing which packages were generated,
  which were excluded, and which already had a hand-written file - useful for tracking `@NullMarked` migration
  progress across a large codebase.
- **`module-info.java` awareness.** Detect JPMS modules and decide whether generation should be skipped or scoped
  differently for non-exported packages.

## Long term / exploratory

- **Android Gradle Plugin support** - `com.android.application`/`com.android.library` use their own source set
  model, not the plain `java`/`java-library` one this plugin currently hooks into.
- **Settings-plugin variant** to apply one `nullmarked` configuration as a default across every subproject in a
  multi-module build, instead of repeating the block per module.
- **Mixed Java/Kotlin module handling** - confirm behavior (and document it) for modules where a package contains
  only Kotlin files; `package-info.java` is a Java-only mechanism, so these packages should probably be left alone
  rather than get a Java file injected into an otherwise-Kotlin package.

## Non-goals

This plugin generates `package-info.java` and wires the JSpecify dependency - it deliberately does not become a
nullness checker itself. Actual enforcement (NullAway, Error Prone, or anything else) stays the consuming project's
choice; the plugin's job is only to make `@NullMarked` coverage cheap to keep complete.
