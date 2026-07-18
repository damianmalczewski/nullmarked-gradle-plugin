/*
 * Copyright 2026-current Damian Malczewski
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
