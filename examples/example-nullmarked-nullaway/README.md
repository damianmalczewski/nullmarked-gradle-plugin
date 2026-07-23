# NullMarked & NullAway example

[![CI | Examples](https://github.com/damianmalczewski/nullmarked-gradle-plugin/actions/workflows/ci-examples.yml/badge.svg)](https://github.com/damianmalczewski/nullmarked-gradle-plugin/actions/workflows/ci-examples.yml)

Standalone Gradle project showing `io.github.malczuuu.nullmarked` used together with [NullAway][NullAway]
and [Error Prone][ErrorProne].

## Table of Contents

- [What it demonstrates](#what-it-demonstrates)
    - [Breaking it on purpose](#breaking-it-on-purpose)
- [Building](#building)
- [Relevant `build.gradle.kts` wiring](#relevant-buildgradlekts-wiring)

## What it demonstrates

NullAway only checks code inside `@NullMarked` scope - packages, classes, or modules carrying that annotation.
Everywhere else it stays silent, even if a method takes or returns `@Nullable` values. So the nullness protection
NullAway/Error Prone give you is only as complete as your `@NullMarked` coverage: miss annotating a package and NullAway
stops catching bugs in it. Protecting the whole project against nullness failures then means every package needs one,
`@NullMarked`-annotated:

```java
@org.jspecify.annotations.NullMarked
package com.example.orders;
```

Projects wiring up NullAway typically hand-write that `package-info.java` per package just to carry the annotation. If
the project is not a shared library, the file has no other purpose, yet every package needs one or NullAway silently
stops checking it. This example applies the `nullmarked` plugin instead, so `package-info.java`
is generated for any package that doesn't already have one:

- `src/main/java/com/example/orders` - has no `package-info.java` in source control. Run `./gradlew build` and
  `generatePackageInfo` writes one into `build/generated/sources/nullmarked/java/main/com/example/orders/`, annotated
  with `@NullMarked`. `compileJava` picks it up automatically, and NullAway enforces nullness for
  `Order` and `OrderService`.
- `src/main/java/com/example/orders/pricing` - a _"legacy"_ package that intentionally keeps its own hand-written
  `package-info.java` without `@NullMarked` (not yet migrated to JSpecify annotations). A hand-written
  `package-info.java` always wins, so the plugin leaves it alone and NullAway ignores nullness in it.

### Breaking it on purpose

Delete `src/main/java/com/example/orders/pricing/package-info.java` and run `./gradlew build` to see the other
direction: the plugin now generates a `@NullMarked` one for `pricing` too, `LegacyPriceCalculator.priceFor`'s
`String couponCode` parameter becomes non-null, and `compileJava` fails because `OrderService` passes it a
`@Nullable` value:

```
> Compilation failed; see the compiler output below.
  .../OrderService.java:11: error: [NullAway] passing @Nullable parameter 'coupon' where @NonNull is required
      int price = priceCalculator.priceFor(order.getId(), coupon);
                                                          ^
      (see http://t.uber.com/nullaway )
  1 error
```

## Building

```sh
./gradlew build
```

`generatePackageInfo` runs before `compileJava`; inspect its output:

```sh
cat build/generated/sources/nullmarked/java/main/com/example/orders/package-info.java
```

## Relevant `build.gradle.kts` wiring

```kotlin
plugins {
    java
    id("net.ltgt.errorprone") version "5.1.0"
    id("io.github.malczuuu.nullmarked") version "0.2.1"
}

nullmarked {
    enabled = true
}

dependencies {
    errorprone("com.google.errorprone:error_prone_core:2.50.0")
    errorprone("com.uber.nullaway:nullaway:0.13.8")
}

tasks.withType<JavaCompile>().configureEach {
    options.errorprone {
        error("NullAway")
        option("NullAway:OnlyNullMarked", "true")
        option("NullAway:JSpecifyMode", "true")
    }
}
```

`NullAway:OnlyNullMarked` restricts checking to `@NullMarked` packages - exactly the packages this plugin generates
`package-info.java` for. `LegacyPriceCalculator` in `pricing` stays unannotated and uninspected only because its
hand-written `package-info.java` keeps that package out of `@NullMarked` scope, same as any not-yet-migrated code in a
larger codebase.

[NullAway]: https://github.com/uber/NullAway

[ErrorProne]: https://errorprone.info
