# NullMarked & multiple source sets example

Standalone Gradle project showing `io.github.malczuuu.nullmarked` opted into a non-`main` source set via
`nullmarked { sourceSet("...") }`. The concrete case here is a [multi-release jar][JEP238] ([MRJAR][JEP238]): a Java 17
baseline plus a Java 21 override that swaps a platform-thread pool for virtual threads.

## Table of Contents

- [What it demonstrates](#what-it-demonstrates)
- [Building and running](#building-and-running)
- [Relevant `build.gradle.kts` wiring](#relevant-buildgradlekts-wiring)
- [Using an unreleased plugin version](#using-an-unreleased-plugin-version)

## What it demonstrates

`ExecutorFactory.create()` has two implementations, both `com.example.executors.ExecutorFactory`, same public API:

- `src/main/java` - the baseline, compiled against Java 17, returns a cached platform-thread pool. This is what any JVM
  below 21 runs.
- `src/main21/java` - the override, compiled against Java 21, returns a virtual thread executor. Packaged into the jar's
  `META-INF/versions/21`, so a JVM running on 21+ loads this class instead of the base one - no version checks or
  reflection needed, the jar's `Multi-Release: true` manifest attribute and the JVM's own class loading handle it. It
  has its own utility subpackage with a small diagnostics tool (checking what thread is it).

Both `src/main/java` and `src/main21/java` declare the same package, `com.example.executors`, but they're two entirely
separate compilations (`compileJava` and `compileMain21Java`). The `main` source set is configured by `nullmarked` by
default. The `main21` requires an explicit setup.

```kotlin
nullmarked {
    sourceSet("main21")
}
```

Building generates a `package-info.java` for `com.example.executors` under both

- `build/generated/sources/nullmarked/java/main/`,
- `build/generated/sources/nullmarked/java/main21/`.

Then it adds `org.jspecify:jspecify` to both `compileOnly` and `main21CompileOnly`.

That leaves two related packaging questions the `Jar` task wiring answers explicitly:

- **A package present in both `main` and `main21`** (`com.example.executors`) gets a generated `package-info.class` in
  *both* compiled outputs - two separate compilations, so each needs its own. But the jar only needs one: the base entry
  (`com/example/executors/package-info.class`) already covers every JVM, including 21+, so the `main21` copy would be a
  redundant duplicate under `META-INF/versions/21`. The `Jar` task excludes it explicitly.
- **A package present only in `main21`** (`com.example.executors.virtual`, see `ExecutorDiagnostics` - it calls
  `Thread.isVirtual()`, a Java 21 API, so it genuinely can't exist under `main`) has no base entry to fall back on. Its
  `package-info.class` is the only copy anywhere, so it ships under `META-INF/versions/21` uncontested.

```sh
jar tf build/libs/example-multi-source-sets-1.0.0-SNAPSHOT.jar | grep package-info
# com/example/executors/package-info.class
# META-INF/versions/21/com/example/executors/virtual/package-info.class
```

## Building and running

```sh
./gradlew build
```

Two `application`-plugin-backed run tasks show the override actually taking effect at runtime:

The default run on Java 17. Uses the project's default toolchain (17) and the raw `main` output directly, so it only
ever sees the platform-thread baseline.

```sh
./gradlew run
# Java 17 -> Thread[pool-1-thread-1,5,main]
```

The multi-release run on Java 21 (with additional `ExecutorDiagnostics` call). Executes the assembled *jar* itself
(`build/libs/{...}.jar`) through a Java 21 launcher: it's the JVM's own multi-release-jar class loading, triggered by
the jar's `Multi-Release: true` manifest attribute, that picks `META-INF/versions/21/.../ExecutorFactory.class` over the
base one - not a Gradle-side switch.

```sh
./gradlew run21
# NOTICE: virtual thread, state NEW
# Java 21 -> VirtualThread[#20]/runnable@ForkJoinPool-1-worker-1
```

Inspect the **jar** directly to see both versions of `ExecutorFactory.class` sitting side by side, or run it manually
with `java -jar`:

```sh
jar tf build/libs/example-multi-source-sets-1.0.0-SNAPSHOT.jar | grep ExecutorFactory
# com/example/executors/ExecutorFactory.class
# META-INF/versions/21/com/example/executors/ExecutorFactory.class

/path/to/jdk-17/bin/java -jar build/libs/example-multi-source-sets-1.0.0-SNAPSHOT.jar
/path/to/jdk-21/bin/java -jar build/libs/example-multi-source-sets-1.0.0-SNAPSHOT.jar
```

## Relevant `build.gradle.kts` wiring

```kotlin
sourceSets {
    create("main21") {
        java.srcDir("src/main21/java")
    }
}

// main21 overrides ExecutorFactory with a Java 21 (virtual-thread) implementation; pin its own compile task to a JDK 21
// toolchain regardless of the project's default (17)
tasks.named<JavaCompile>("compileMain21Java").configure {
    javaCompiler = javaToolchains.compilerFor { languageVersion = JavaLanguageVersion.of(21) }
}

// Package main21's classes into META-INF/versions/21, so a JVM running on 21+ picks the virtual-thread ExecutorFactory
// while everything below 21 keeps using the platform-thread one.
tasks.named<Jar>("jar").configure {
    val mainClassesDirs = sourceSets["main"].output.classesDirs
    into("META-INF/versions/21") {
        from(sourceSets["main21"].output) {
            // com.example.executors has package-info.class at the jar root already (main covers it); don't re-ship an
            // identical copy under versions/21. com.example.executors.virtual has no main counterpart at all, so its
            // package-info.class isn't excluded here - it's the only place it exists.
            exclude {
                it.path.endsWith("package-info.class") && mainClassesDirs.any { dir -> dir.resolve(it.path).exists() }
            }
        }
    }
    manifest {
        attributes(
            "Multi-Release" to "true",
            "Main-Class" to "com.example.executors.Main",
        )
    }
}

// `run` (from the `application` plugin) uses the project's default toolchain (17) and the raw `main` output, so it
// always exercises the platform-thread baseline. `run21` instead executes the assembled jar itself on a Java 21
// launcher, so the JVM's own multi-release-jar class loading picks the META-INF/versions/21 override, i.e. the
// virtual-thread ExecutorFactory.
tasks.register<JavaExec>("run21") {
    classpath = files(tasks.named<Jar>("jar").flatMap { it.archiveFile })
    mainClass = application.mainClass
    javaLauncher = javaToolchains.launcherFor { languageVersion = JavaLanguageVersion.of(21) }
}

nullmarked {
    sourceSet("main21")
}
```

None of that is `nullmarked`-specific - it's the standard way to build an MRJAR in Gradle. The only plugin-specific line
is the `sourceSet("main21")`; without it `main21` would compile fine but stay outside `@NullMarked` scope, same as any
source set the plugin isn't told about.

[JEP238]: https://openjdk.org/jeps/238
