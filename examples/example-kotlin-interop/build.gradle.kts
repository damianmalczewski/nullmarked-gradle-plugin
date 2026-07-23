plugins {
    java
    application
    kotlin("jvm") version "2.4.0"
    id("io.github.malczuuu.nullmarked") version "0.2.1"
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

kotlin {
    jvmToolchain(25)
    compilerOptions {
        // Kotlin defaults to "strict" for org.jspecify.annotations since 2.1.0; set explicitly so this build
        // doesn't silently change behavior if that default ever changes.
        freeCompilerArgs.add("-Xnullability-annotations=@org.jspecify.annotations:strict")
    }
}

application {
    mainClass = "com.example.app.MainKt"
}
