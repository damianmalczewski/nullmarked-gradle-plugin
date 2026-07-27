plugins {
    application
    java
    id("io.github.malczuuu.nullmarked") version "0.6.0"
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

application {
    mainClass = "com.example.executors.Main"
}

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
    group = "application"
    description = "Runs the built jar on a Java 21 launcher, exercising the virtual-thread ExecutorFactory override."
    classpath = files(tasks.named<Jar>("jar").flatMap { it.archiveFile })
    mainClass = application.mainClass
    javaLauncher = javaToolchains.launcherFor { languageVersion = JavaLanguageVersion.of(21) }
}

nullmarked {
    // main already gets a @NullMarked package-info by default; main21 is a separate compilation with its own
    // package-info requirement, so it needs opting in too.
    sourceSet("main21")
}

defaultTasks("build")
