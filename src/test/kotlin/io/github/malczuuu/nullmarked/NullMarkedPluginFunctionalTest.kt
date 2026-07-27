/*
 * Copyright 2026-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.malczuuu.nullmarked

import io.github.malczuuu.nullmarked.fixtures.TestProject
import java.io.File
import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class NullMarkedPluginFunctionalTest {

  @TempDir lateinit var projectDir: File

  private lateinit var project: TestProject

  @BeforeEach
  fun beforeEach() {
    project = TestProject(projectDir)
    project.writeStandardBuild()
    project.writeSampleSources()
  }

  @Test
  fun `compiles with generated package-info and auto-added jspecify dependency`() {
    val result = project.runner("compileJava").build()

    assertThat(result.task(":generatePackageInfo")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
    assertThat(result.task(":verifyPackageInfo")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
    assertThat(result.task(":compileJava")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
    assertThat(project.generatedPackageInfo("com.acme")).exists()
    assertThat(project.generatedPackageInfo("com.acme.manual")).doesNotExist()
    assertThat(project.compiledClass("com.acme.package-info")).exists()
    assertThat(project.compiledClass("com.acme.Foo")).exists()
  }

  @Test
  fun `generatePackageInfo is up-to-date on unchanged sources`() {
    project.runner("generatePackageInfo").build()
    val secondRun = project.runner("generatePackageInfo").build()

    assertThat(secondRun.task(":generatePackageInfo")?.outcome).isEqualTo(TaskOutcome.UP_TO_DATE)
  }

  @Test
  fun `jspecify appears on the compile classpath`() {
    val result = project.runner("dependencies", "--configuration", "compileClasspath").build()

    assertThat(result.output).contains("org.jspecify:jspecify:1.0.0")
  }

  @Test
  fun `jspecify is added when the compile classpath is resolved from a task action`() {
    project.appendToBuildScript(
        """
        tasks.register("resolveCompileClasspath") {
            val compileClasspath = configurations["compileClasspath"].incoming.files
            doLast {
                compileClasspath.files.forEach { println("RESOLVED " + it.name) }
            }
        }
        """
    )

    project.runner("resolveCompileClasspath", "--configuration-cache").build()
    val secondRun = project.runner("resolveCompileClasspath", "--configuration-cache").build()

    assertThat(secondRun.output).contains("Configuration cache entry reused")
    assertThat(secondRun.output).contains("RESOLVED jspecify-1.0.0.jar")
  }

  @Test
  fun `user-declared jspecify version wins`() {
    project.appendToBuildScript(
        """
        dependencies {
            compileOnly("org.jspecify:jspecify:0.3.0")
        }
        """
    )

    val result = project.runner("dependencies", "--configuration", "compileClasspath").build()

    assertThat(result.output).contains("org.jspecify:jspecify:0.3.0")
    assertThat(result.output).doesNotContain("org.jspecify:jspecify:1.0.0")
  }

  @Test
  fun `generation can be disabled via the extension`() {
    project.appendToBuildScript(
        """
        nullmarked {
            enabled = false
        }

        // A disabled plugin adds no jspecify dependency, so the sample sources bring their own.
        dependencies {
            compileOnly("org.jspecify:jspecify:1.0.0")
        }
        """
    )

    val result = project.runner("compileJava").build()

    assertThat(result.task(":generatePackageInfo")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
    assertThat(project.generatedPackageInfo("com.acme")).doesNotExist()
    assertThat(result.task(":compileJava")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
  }

  @Test
  fun `disabling the plugin skips the jspecify dependency`() {
    project.appendToBuildScript(
        """
        nullmarked {
            enabled = false
        }
        """
    )

    val result = project.runner("dependencies", "--configuration", "compileClasspath").build()

    assertThat(result.output).doesNotContain("org.jspecify:jspecify")
  }

  @Test
  fun `verifyOnly still adds the jspecify dependency`() {
    project.appendToBuildScript(
        """
        nullmarked {
            verifyOnly = true
        }
        """
    )

    val result = project.runner("dependencies", "--configuration", "compileClasspath").build()

    assertThat(result.output).contains("org.jspecify:jspecify:1.0.0")
  }

  @Test
  fun `excluded packages are skipped during generation`() {
    project.appendToBuildScript(
        """
        nullmarked {
            packages {
                exclude("com.acme..")
            }
        }
        """
    )

    val result = project.runner("generatePackageInfo").build()

    assertThat(result.task(":generatePackageInfo")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
    assertThat(project.generatedPackageInfo("com.acme")).doesNotExist()
  }

  @Test
  fun `changing a package rule re-runs generation`() {
    project.runner("generatePackageInfo").build()
    assertThat(project.generatedPackageInfo("com.acme")).exists()

    project.appendToBuildScript(
        """
        nullmarked {
            packages {
                exclude("com.acme..")
            }
        }
        """
    )
    val secondRun = project.runner("generatePackageInfo").build()

    assertThat(secondRun.task(":generatePackageInfo")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
    assertThat(project.generatedPackageInfo("com.acme")).doesNotExist()
  }

  @Test
  fun `packages can be excluded by configuring the tasks directly`() {
    project.appendToBuildScript(
        """
        tasks.withType<io.github.malczuuu.nullmarked.tasks.GeneratePackageInfo>().configureEach {
            packages {
                exclude("com.acme..")
            }
        }

        tasks.withType<io.github.malczuuu.nullmarked.tasks.VerifyPackageInfo>().configureEach {
            packages {
                exclude("com.acme..")
            }
        }
        """
    )

    val result = project.runner("compileJava", "--configuration-cache").build()

    assertThat(result.task(":verifyPackageInfo")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
    assertThat(project.generatedPackageInfo("com.acme")).doesNotExist()
  }

  @Test
  fun `task-level package rules are evaluated after the extension ones`() {
    project.appendToBuildScript(
        """
        nullmarked {
            packages {
                exclude("com.acme..")
            }
        }

        tasks.withType<io.github.malczuuu.nullmarked.tasks.GeneratePackageInfo>().configureEach {
            packages {
                include("com.acme")
            }
        }
        """
    )

    project.runner("generatePackageInfo").build()

    assertThat(project.generatedPackageInfo("com.acme")).exists()
  }

  @Test
  fun `package rules survive the configuration cache`() {
    project.appendToBuildScript(
        """
        nullmarked {
            packages {
                exclude("com.acme..")
            }
        }
        """
    )

    project.runner("generatePackageInfo", "--configuration-cache").build()
    val secondRun = project.runner("generatePackageInfo", "--configuration-cache").build()

    assertThat(secondRun.output).contains("Configuration cache entry reused")
    assertThat(project.generatedPackageInfo("com.acme")).doesNotExist()
  }

  @Test
  fun `a later include re-admits an excluded package`() {
    project.writeSource(
        "com/acme/internal/Impl.java",
        """
        package com.acme.internal;

        class Impl {}
        """,
    )
    project.appendToBuildScript(
        """
        nullmarked {
            packages {
                exclude("com.acme..")
                include("com.acme.internal")
            }
        }
        """
    )

    val result = project.runner("generatePackageInfo").build()

    assertThat(result.task(":generatePackageInfo")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
    assertThat(project.generatedPackageInfo("com.acme")).doesNotExist()
    assertThat(project.generatedPackageInfo("com.acme.internal")).exists()
  }

  @Test
  fun `source set rules apply after the top-level ones`() {
    project.writeTestSource(
        "com/acme/FooTest.java",
        """
        package com.acme;

        class FooTest {}
        """,
    )
    project.appendToBuildScript(
        """
        nullmarked {
            sourceSet("test") {
                packages {
                    include("com.acme")
                }
            }
            packages {
                exclude("com.acme..")
            }
        }
        """
    )

    val result = project.runner("compileJava", "compileTestJava").build()

    assertThat(result.task(":generateTestPackageInfo")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
    assertThat(project.generatedPackageInfo("com.acme")).doesNotExist()
    assertThat(project.generatedPackageInfo("com.acme", sourceSet = "test")).exists()
  }

  @Test
  fun `configured test source set gets its own generation task and dependency`() {
    project.writeTestSource(
        "com/acme/FooTest.java",
        """
        package com.acme;

        class FooTest {}
        """,
    )
    project.appendToBuildScript(
        """
        nullmarked {
            sourceSet("test") {}
        }
        """
    )

    val result = project.runner("compileTestJava").build()

    assertThat(result.task(":generateTestPackageInfo")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
    assertThat(result.task(":compileTestJava")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
    assertThat(project.generatedPackageInfo("com.acme", sourceSet = "test")).exists()
  }

  @Test
  fun `jspecify appears on the test compile classpath for a configured test source set`() {
    project.writeTestSource(
        "com/acme/FooTest.java",
        """
        package com.acme;

        class FooTest {}
        """,
    )
    project.appendToBuildScript(
        """
        nullmarked {
            sourceSet("test") {}
        }
        """
    )

    val result = project.runner("dependencies", "--configuration", "testCompileClasspath").build()

    assertThat(result.output).contains("org.jspecify:jspecify:1.0.0")
  }

  @Test
  fun `custom main21 source set for a multi-release jar gets generation and dependency`() {
    project.writeSource(
        "com/acme/main/Foo.java",
        """
        package com.acme.main;

        class Foo {}
        """,
    )
    project.write(
        "src/main21/java/com/acme/main21/Foo21.java",
        """
        package com.acme.main21;

        class Foo21 {}
        """,
    )
    project.appendToBuildScript(
        """
        sourceSets {
            create("main21") {
                java.srcDir("src/main21/java")
            }
        }

        // Mirrors a real multi-release-jar setup: main compiles at the project's default toolchain
        // (17 here), main21 pins its own compile task to JDK 21.
        tasks.named<org.gradle.api.tasks.compile.JavaCompile>("compileMain21Java").configure {
            javaCompiler = javaToolchains.compilerFor {
                languageVersion = org.gradle.jvm.toolchain.JavaLanguageVersion.of(21)
            }
        }

        nullmarked {
            sourceSet("main21") {}
        }
        """
    )

    val result = project.runner("compileJava", "compileMain21Java").build()

    assertThat(result.task(":generatePackageInfo")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
    assertThat(result.task(":compileJava")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
    assertThat(project.generatedPackageInfo("com.acme.main")).exists()

    assertThat(result.task(":generateMain21PackageInfo")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
    assertThat(result.task(":compileMain21Java")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
    assertThat(project.generatedPackageInfo("com.acme.main21", sourceSet = "main21")).exists()
  }

  @Test
  fun `unconfigured source sets are not touched`() {
    project.writeTestSource(
        "com/acme/FooTest.java",
        """
        package com.acme;

        class FooTest {}
        """,
    )

    val result = project.runner("tasks", "--all").build()

    assertThat(result.output).doesNotContain("generateTestPackageInfo")
  }

  @Test
  fun `verification passes on generated and hand-written package-info files`() {
    val result = project.runner("verifyPackageInfo").build()

    assertThat(result.task(":generatePackageInfo")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
    assertThat(result.task(":verifyPackageInfo")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
  }

  @Test
  fun `verifyPackageInfo is up-to-date on unchanged sources`() {
    project.runner("verifyPackageInfo").build()
    val secondRun = project.runner("verifyPackageInfo").build()

    assertThat(secondRun.task(":verifyPackageInfo")?.outcome).isEqualTo(TaskOutcome.UP_TO_DATE)
  }

  @Test
  fun `verifyOnly fails the build listing packages without a hand-written package-info`() {
    project.appendToBuildScript(
        """
        nullmarked {
            verifyOnly = true
        }
        """
    )

    val result = project.runner("compileJava").buildAndFail()

    assertThat(result.task(":verifyPackageInfo")?.outcome).isEqualTo(TaskOutcome.FAILED)
    assertThat(result.output).contains("1 package(s) without a package-info.java").contains("com.acme")
    assertThat(result.output).contains("disabling nullmarked.verifyOnly")
    assertThat(result.output).doesNotContain("com.acme.manual")
    assertThat(project.generatedPackageInfo("com.acme")).doesNotExist()
  }

  @Test
  fun `verifyOnly passes once every package declares a package-info`() {
    project.writeSource(
        "com/acme/package-info.java",
        """
        @NullMarked
        package com.acme;

        import org.jspecify.annotations.NullMarked;
        """,
    )
    project.appendToBuildScript(
        """
        nullmarked {
            verifyOnly = true
        }
        """
    )

    val result = project.runner("compileJava").build()

    assertThat(result.task(":verifyPackageInfo")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
    assertThat(result.task(":compileJava")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
    assertThat(project.generatedPackageInfo("com.acme")).doesNotExist()
  }

  @Test
  fun `verifyOnly skips excluded packages`() {
    project.appendToBuildScript(
        """
        nullmarked {
            verifyOnly = true
            packages {
                exclude("com.acme")
            }
        }
        """
    )

    val result = project.runner("verifyPackageInfo").build()

    assertThat(result.task(":verifyPackageInfo")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
  }

  @Test
  fun `disabling the plugin disables verification too`() {
    project.appendToBuildScript(
        """
        nullmarked {
            enabled = false
            verifyOnly = true
        }

        // A disabled plugin adds no jspecify dependency, so the sample sources bring their own.
        dependencies {
            compileOnly("org.jspecify:jspecify:1.0.0")
        }
        """
    )

    val result = project.runner("compileJava").build()

    assertThat(result.task(":verifyPackageInfo")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
    assertThat(result.task(":compileJava")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
  }

  @Test
  fun `verifyOnly applies per source set`() {
    project.writeTestSource(
        "com/acme/FooTest.java",
        """
        package com.acme;

        class FooTest {}
        """,
    )
    project.appendToBuildScript(
        """
        nullmarked {
            sourceSet("test") {
                verifyOnly = true
            }
        }
        """
    )

    val result = project.runner("compileTestJava").buildAndFail()

    assertThat(result.task(":verifyTestPackageInfo")?.outcome).isEqualTo(TaskOutcome.FAILED)
    assertThat(project.generatedPackageInfo("com.acme", sourceSet = "test")).doesNotExist()
  }

  @Test
  fun `disabling generation removes previously generated files`() {
    project.runner("generatePackageInfo").build()
    assertThat(project.generatedPackageInfo("com.acme")).exists()

    project.appendToBuildScript(
        """
        nullmarked {
            enabled = false
        }
        """
    )
    val result = project.runner("generatePackageInfo").build()

    assertThat(result.task(":generatePackageInfo")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
    assertThat(project.generatedPackageInfo("com.acme")).doesNotExist()
  }

  @Test
  fun `fails on a source set no java source set matches`() {
    project.appendToBuildScript(
        """
        nullmarked {
            sourceSet("itnegrationTest") {
                verifyOnly = true
            }
        }
        """
    )

    val result = project.runner("generatePackageInfo").buildAndFail()

    assertThat(result.output).contains("nullmarked: no source set named 'itnegrationTest'")
    assertThat(result.output).contains("Available: main, test.")
  }

  @Test
  fun `fails listing every source set no java source set matches`() {
    project.appendToBuildScript(
        """
        nullmarked {
            sourceSet("nope")
            sourceSet("alsoNope")
        }
        """
    )

    val result = project.runner("generatePackageInfo").buildAndFail()

    assertThat(result.output).contains("nullmarked: no source sets named 'alsoNope', 'nope'")
  }

  @Test
  fun `accepts a source set registered after the nullmarked block`() {
    project.appendToBuildScript(
        """
        nullmarked {
            sourceSet("extra")
        }

        sourceSets {
            create("extra")
        }
        """
    )

    val result = project.runner("generateExtraPackageInfo").build()

    assertThat(result.task(":generateExtraPackageInfo")?.outcome).isEqualTo(TaskOutcome.NO_SOURCE)
  }

  @Test
  fun `rejects an invalid package identifier at configuration time`() {
    project.appendToBuildScript(
        """
        nullmarked {
            packages {
                exclude("com.acme!!")
            }
        }
        """
    )

    val result = project.runner("tasks").buildAndFail()

    assertThat(result.output).contains("Invalid package identifier 'com.acme!!'")
  }

  @Test
  fun `compileJava runs generation and verification`() {
    val result = project.runner("compileJava").build()

    assertThat(result.task(":generatePackageInfo")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
    assertThat(result.task(":verifyPackageInfo")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
  }

  @Test
  fun `generation task sits in the build group`() {
    val result = project.runner("tasks", "--group", "generation").build()

    assertThat(result.output).contains("generatePackageInfo")
  }

  @Test
  fun `does nothing when applied without the java plugin`() {
    project.write("settings.gradle.kts", "rootProject.name = \"under-test\"")
    project.write(
        "build.gradle.kts",
        """
        plugins {
            id("io.github.malczuuu.nullmarked")
        }
        """,
    )

    val result = project.runner("tasks").build()

    assertThat(result.output).doesNotContain("generatePackageInfo")
  }
}
