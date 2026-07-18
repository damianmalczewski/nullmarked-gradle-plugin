import com.diffplug.spotless.LineEnding
import com.diffplug.spotless.kotlin.KtfmtStep
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    `kotlin-dsl`
    `maven-publish`
    id("com.diffplug.spotless") version "8.8.0"
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

kotlin {
    jvmToolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
    compilerOptions {
        javaParameters = true
        apiVersion = KotlinVersion.KOTLIN_2_0
        languageVersion = KotlinVersion.KOTLIN_2_0
    }
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:6.1.1"))

    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    testImplementation("org.assertj:assertj-core:3.27.7")

    testImplementation(gradleTestKit())
}

testing {
    suites {
        val test by getting(JvmTestSuite::class) {
            useJUnitJupiter()
        }

        val integrationTest by registering(JvmTestSuite::class) {
            useJUnitJupiter()

            targets {
                all {
                    testTask.configure {
                        shouldRunAfter(tasks.test)
                    }
                }
            }
        }
    }
}

val integrationTestImplementation by configurations.getting {
    extendsFrom(configurations.testImplementation.get())
}

val integrationTestRuntimeOnly by configurations.getting {
    extendsFrom(configurations.testRuntimeOnly.get())
}

sourceSets["integrationTest"].apply {
    compileClasspath += sourceSets["main"].output + sourceSets["test"].output
    runtimeClasspath += sourceSets["main"].output + sourceSets["test"].output
}

gradlePlugin {
    testSourceSets(sourceSets["test"], sourceSets["integrationTest"])

    plugins {
        create("gradle-jspecify") {
            id = "io.github.malczuuu.gradle-jspecify"
            implementationClass = "io.github.malczuuu.gradle.jspecify.JSpecifyPlugin"
            displayName = "JSpecify Gradle Plugin"
            description =
                "Generates @NullMarked package-info.java for packages missing it and adds the JSpecify annotations as a compileOnly dependency."
        }
    }
}

spotless {
    kotlin {
        target("**/src/**/*.kt")
        licenseHeaderFile("${rootProject.rootDir}/gradle/license-header.kt")

        ktfmt("0.64").metaStyle().configure {
            it.setMaxWidth(120)
            it.setRemoveUnusedImports(true)
            it.setTrailingCommaManagementStrategy(KtfmtStep.TrailingCommaManagementStrategy.COMPLETE)
        }
        endWithNewline()
        lineEndings = LineEnding.UNIX
    }

    kotlinGradle {
        target("**/*.gradle.kts")
        targetExclude("**/build/**")

        ktlint("1.8.0").editorConfigOverride(mapOf("max_line_length" to "120"))
        endWithNewline()
        lineEndings = LineEnding.UNIX
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    testLogging {
        events(
            TestLogEvent.PASSED,
            TestLogEvent.FAILED,
            TestLogEvent.SKIPPED,
        )
        exceptionFormat = TestExceptionFormat.FULL
        showStandardStreams = true
    }
}

tasks.register("allTest") {
    description = "Runs all tests (unit + integration)."
    group = "verification"
    dependsOn(tasks.named<Test>("test"), tasks.named<Test>("integrationTest"))
}

defaultTasks("spotlessApply", "build")
