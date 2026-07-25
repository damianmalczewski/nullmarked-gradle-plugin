import net.ltgt.gradle.errorprone.errorprone

plugins {
    java
    application
    kotlin("jvm") version "2.4.10"
    id("io.github.malczuuu.nullmarked") version "0.3.0"
    id("net.ltgt.errorprone") version "5.1.0"
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.errorprone {
        error("NullAway")
        option("NullAway:OnlyNullMarked", "true")
        option("NullAway:JSpecifyMode", "true")
    }
}

kotlin {
    jvmToolchain(25)
    compilerOptions {
        // Kotlin defaults to "strict" for org.jspecify.annotations since 2.1.0; set explicitly for reference.
        freeCompilerArgs.add("-Xnullability-annotations=@org.jspecify.annotations:strict")
    }
}

dependencies {
    errorprone("com.google.errorprone:error_prone_core:2.50.0")
    errorprone("com.uber.nullaway:nullaway:0.13.8")
}

application {
    mainClass = "com.example.app.MainKt"
}
