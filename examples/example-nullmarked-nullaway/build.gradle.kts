import net.ltgt.gradle.errorprone.errorprone

plugins {
    java
    id("io.github.malczuuu.nullmarked") version "0.7.0"
    id("net.ltgt.errorprone") version "5.1.0"
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
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

defaultTasks("build")
