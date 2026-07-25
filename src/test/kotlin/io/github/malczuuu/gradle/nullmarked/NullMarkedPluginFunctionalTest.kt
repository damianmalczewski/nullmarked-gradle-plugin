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

package io.github.malczuuu.gradle.nullmarked

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
        """
    )

    val result = project.runner("compileJava").build()

    assertThat(result.task(":generatePackageInfo")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
    assertThat(project.generatedPackageInfo("com.acme")).doesNotExist()
    assertThat(result.task(":compileJava")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
  }

  @Test
  fun `excluded packages are skipped during generation`() {
    project.appendToBuildScript(
        """
        nullmarked {
            excludedPackages = listOf("com.acme..")
        }
        """
    )

    val result = project.runner("generatePackageInfo").build()

    assertThat(result.task(":generatePackageInfo")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
    assertThat(project.generatedPackageInfo("com.acme")).doesNotExist()
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
        tasks.named<org.gradle.api.tasks.compile.JavaCompile>("compileMain21Java") {
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
}
