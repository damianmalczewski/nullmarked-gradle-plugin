# NullMarked & Kotlin interop example

[![CI | Examples](https://github.com/damianmalczewski/nullmarked-gradle-plugin/actions/workflows/ci-examples.yml/badge.svg)](https://github.com/damianmalczewski/nullmarked-gradle-plugin/actions/workflows/ci-examples.yml)

Standalone Gradle project showing `io.github.malczuuu.nullmarked` used to make Java APIs interop cleanly with
[Kotlin's null safety](https://kotlinlang.org/docs/java-interop.html#nullability-annotations).

## Table of Contents

- [What it demonstrates](#what-it-demonstrates)
    - [Breaking it on purpose](#breaking-it-on-purpose)
- [Building](#building)
- [Relevant `build.gradle.kts` wiring](#relevant-buildgradlekts-wiring)

## What it demonstrates

Since Kotlin 2.1, the compiler reads [JSpecify](https://jspecify.dev/) nullness annotations on Java code and enforces
them like it would for Kotlin's own `String`/`String?` types - but only inside `@NullMarked` scope. Outside it, a Java
member is seen as a *platform type* (`String!`): Kotlin has no idea whether `null` is allowed, so it doesn't check
either way and trusts the caller.

- `src/main/java/com/example/greeting` (`Greeter`) - has no `package-info.java` in source control. The plugin generates
  one annotated with `@NullMarked`, so Kotlin sees `Greeter.greet`'s parameter as `String?` and its return type as
  `String`, and `lookupNickname`'s return type as `String?`, exactly matching the `@Nullable`
  annotations in the Java source.
- `src/main/java/com/example/legacy` (`LegacyGreeter`) - keeps a hand-written `package-info.java` without
  `@NullMarked` (not yet migrated). Kotlin sees `LegacyGreeter.greet`'s `String` parameter as the platform type
  `String!` instead of a real `String`, even though the Java signature carries no `@Nullable` at all.

Run `com.example.app.MainKt` (`./gradlew run`) to see both in action.

### Breaking it on purpose

**Null-marked side - caught at compile time.** `Greeter.lookupNickname` returns `@Nullable String`, so Kotlin infers
`String?`. Change the `nickname` variable in `Main.kt` to a non-null type:

```kotlin
val nickname: String = greeter.lookupNickname("Ada")
```

`./gradlew compileKotlin` now fails before it ever runs:

```
e: .../Main.kt:15:26 Initializer type mismatch: expected 'String', actual 'String?'.
```

**Legacy side - not caught until runtime.** `LegacyGreeter.greet`'s `name` parameter is a platform type, so Kotlin
doesn't stop you from passing `null`:

```kotlin
println(legacy.greet(null))
```

This compiles without a warning. Running it throws instead:

```
Exception in thread "main" java.lang.NullPointerException: Cannot invoke "String.trim()" because "name" is null
	at com.example.legacy.LegacyGreeter.greet(LegacyGreeter.java:6)
	at com.example.app.MainKt.main(Main.kt:21)
```

That's the gap `@NullMarked` closes: the exact same kind of bug, but moved from a runtime crash to a compiler error,
just by annotating the package.

## Building

```sh
./gradlew build   # compiles Java + Kotlin sources, runs generatePackageInfo first
./gradlew run      # runs com.example.app.MainKt
```

Inspect the generated file:

```sh
cat build/generated/sources/nullmarked/java/main/com/example/greeting/package-info.java
```

## Relevant `build.gradle.kts` wiring

```kotlin
plugins {
    java
    application
    kotlin("jvm") version "2.4.0"
    id("io.github.malczuuu.nullmarked") version "0.2.1"
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        // Kotlin defaults to "strict" for org.jspecify.annotations since 2.1.0; set explicitly so this build
        // doesn't silently change behavior if that default ever changes.
        freeCompilerArgs.add("-Xnullability-annotations=@org.jspecify.annotations:strict")
    }
}

application {
    mainClass = "com.example.app.MainKt"
}
```

No NullAway/Error Prone here - see [`example-nullmarked-nullaway`](../example-nullmarked-nullaway) for that pairing.
This example only needs the `nullmarked` plugin and a Kotlin compiler recent enough to read JSpecify annotations (2.1+;
`-Xnullability-annotations` lets you dial that down to `warning` or `ignore` per module if a codebase isn't ready for
`strict` yet).
