import com.diffplug.spotless.LineEnding
import com.diffplug.spotless.kotlin.KtfmtStep
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.gradle.plugin.compatibility.compatibility
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    jacoco
    `kotlin-dsl`
    `maven-publish`
    signing
    id("com.diffplug.spotless") version "8.8.0"
    id("org.gradle.plugin-compatibility") version "1.0.0"
    id("com.gradle.plugin-publish") version "2.1.1"
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
        val test = getByName<JvmTestSuite>("test") {
            useJUnitJupiter()
        }
        register<JvmTestSuite>("integrationTest") {
            useJUnitJupiter()
            targets {
                all {
                    testTask.configure {
                        shouldRunAfter(test)
                    }
                }
            }
        }
    }
}

configurations.getByName("integrationTestImplementation") {
    extendsFrom(configurations.testImplementation.get())
}

configurations.getByName("integrationTestRuntimeOnly") {
    extendsFrom(configurations.testRuntimeOnly.get())
}

sourceSets["integrationTest"].apply {
    compileClasspath += sourceSets["main"].output + sourceSets["test"].output
    runtimeClasspath += sourceSets["main"].output + sourceSets["test"].output
}

val generatePluginProperties = tasks.register("generatePluginProperties") {
    group = "generation"
    description = "Generates plugin.properties with plugin local metadata."

    val pluginVersion = version.toString()
    val outputDir = layout.buildDirectory.dir("generated/resources/plugin-properties")

    inputs.property("version", pluginVersion)
    outputs.dir(outputDir)

    doLast {
        outputDir.get().file("io/github/malczuuu/gradle/nullmarked/plugin.properties").asFile.apply {
            parentFile.mkdirs()
            writeText("version=$pluginVersion\n")
        }
    }
}

sourceSets["main"].resources.srcDir(generatePluginProperties)

gradlePlugin {
    testSourceSets(sourceSets["test"], sourceSets["integrationTest"])
    website = "https://github.com/damianmalczewski/gradle-nullmarked-plugin"
    vcsUrl = "https://github.com/damianmalczewski/gradle-nullmarked-plugin.git"
    plugins {
        create("gradle-nullmarked") {
            id = "io.github.malczuuu.nullmarked"
            implementationClass = "io.github.malczuuu.gradle.nullmarked.NullMarkedPlugin"
            displayName = "Gradle NullMarked Plugin"
            description = "Generates @NullMarked-annotated package-info.java for packages missing it."
            tags = listOf("JSpecify", "NullMarked", "package-info.java")
            compatibility {
                features {
                    configurationCache = true
                }
            }
        }
    }
}

publishing {
    publications.withType<MavenPublication>().configureEach {
        pom {
            name = "Gradle NullMarked Plugin"
            description = "Generates @NullMarked-annotated package-info.java for packages missing it."
            url = "https://github.com/damianmalczewski/gradle-nullmarked-plugin"
            inceptionYear = "2026"
            licenses {
                license {
                    name = "The Apache License, Version 2.0"
                    url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
                }
            }
            developers {
                developer {
                    id = "damianmalczewski"
                    name = "Damian Malczewski"
                    url = "https://github.com/damianmalczewski"
                }
            }
            scm {
                connection = "scm:git:git://github.com/damianmalczewski/gradle-nullmarked-plugin.git"
                developerConnection = "scm:git:ssh://github.com/damianmalczewski/gradle-nullmarked-plugin.git"
                url = "https://github.com/damianmalczewski/gradle-nullmarked-plugin"
            }
            issueManagement {
                system = "GitHub Issues"
                url = "https://github.com/damianmalczewski/gradle-nullmarked-plugin/issues"
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

tasks.named<JavaCompile>("compileJava").configure {
    options.release = 8
}

tasks.named<KotlinCompile>("compileKotlin").configure {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_1_8
    }
}

tasks.withType<Test>().configureEach {
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

tasks.named<Test>("integrationTest").configure {
    providers.gradleProperty("compat.gradle.version").orNull?.let {
        systemProperty("compat.gradle.version", it)
    }
}

tasks.named<JacocoReport>("jacocoTestReport").configure {
    dependsOn(tasks.named("test"))

    reports {
        xml.required = true
        html.required = true
        csv.required = false
    }
}

tasks.named<Task>("check").configure {
    finalizedBy(tasks.named("jacocoTestReport"))
}

tasks.register("allTest") {
    description = "Runs all tests (unit + integration)."
    group = "verification"
    dependsOn(tasks.named<Test>("test"), tasks.named<Test>("integrationTest"))
}

defaultTasks("spotlessApply", "build")
