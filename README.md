# Gradle JSpecify Plugin

Gradle plugin applying [JSpecify](https://jspecify.dev/) conventions to Java projects.

## What it does

Applying `io.github.malczuuu.gradle-jspecify` to a Java project:

1. **Generates `package-info.java`** - for every non-empty package of the `main` source set that does not declare its
   own `package-info.java`, the `generatePackageInfo` task generates one annotated with `@NullMarked` into
   `build/generated/sources/jspecify/java/main`. The generated directory is registered as a source directory, so
   `compileJava` picks it up automatically. Hand-written `package-info.java` files always win - no file is generated for
   their packages.
2. **Adds the JSpecify dependency** - `org.jspecify:jspecify:1.0.0` is added as `compileOnly` unless the build script
   already declares it in `compileOnly`, `implementation`, `api`, or `compileOnlyApi`.

## Usage

> [!IMPORTANT]
> The project is in its draft stage and is not published anywhere. Usage requires building it to local Maven repository.

1. Build project to local Maven repository.
   ```sh
   ./gradlew publishToMavenLocal
   ```
2. Add `mavenLocal` to `pluginManagement` in `settings.gradle.kts`.
   ```kt
   pluginManagement {
       repositories {
           gradlePluginPortal()
           mavenCentral()
           mavenLocal()
       }
   }
   ```
3. Apply plugin in `build.gradle.kts`.
   ```kotlin
   plugins {
       java
       id("io.github.malczuuu.gradle-jspecify") version "0.1.0-SNAPSHOT"
   }
   ```

Configuration (all optional):

```kotlin
jspecify {
    generatePackageInfo = true  // default; set false to disable generation
    jspecifyVersion = "1.0.0"   // version of the auto-added org.jspecify:jspecify dependency
}
```

## Building

```sh
./gradlew build            # compiles the plugin and runs its unit tests
./gradlew integrationTest  # Gradle version compatibility matrix (TestKit, slow)
```
