import com.diffplug.spotless.LineEnding
import com.diffplug.spotless.kotlin.KtfmtStep
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.gradle.plugin.compatibility.compatibility
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    `kotlin-dsl`
    `maven-publish`
    id("com.diffplug.spotless") version "8.8.0"
    id("org.gradle.plugin-compatibility") version "1.0.0"
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
    testImplementation(platform("org.junit:junit-bom:6.1.2"))

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

    website = "https://github.com/damianmalczewski/gradle-nullmarked-plugin"
    vcsUrl = "https://github.com/damianmalczewski/gradle-nullmarked-plugin"

    plugins {
        create("gradle-nullmarked") {
            id = "io.github.malczuuu.nullmarked"
            implementationClass = "io.github.malczuuu.gradle.nullmarked.NullMarkedPlugin"
            displayName = "NullMarked Gradle Plugin"
            description =
                "Generates @NullMarked package-info.java for packages missing it and adds the JSpecify annotations as a compileOnly dependency."
            compatibility {
                features {
                    configurationCache = true
                }
            }
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

tasks.named<Test>("integrationTest") {
    providers.gradleProperty("compat.gradle.version").orNull?.let {
        systemProperty("compat.gradle.version", it)
    }
}

tasks.register("allTest") {
    description = "Runs all tests (unit + integration)."
    group = "verification"
    dependsOn(tasks.named<Test>("test"), tasks.named<Test>("integrationTest"))
}

defaultTasks("spotlessApply", "build")
