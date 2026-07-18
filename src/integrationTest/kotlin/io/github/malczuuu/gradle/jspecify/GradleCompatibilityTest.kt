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

package io.github.malczuuu.gradle.jspecify

import java.io.File
import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

/**
 * Runs the core plugin scenario against multiple Gradle versions (latest checked minor per line). Run via the
 * `integrationTest` suite.
 */
class GradleCompatibilityTest {

  @TempDir lateinit var projectDir: File

  @ParameterizedTest(name = "gradle {0}")
  @ValueSource(
      strings =
          [
              "8.2",
              "8.3",
              "8.4",
              "8.5",
              "8.6",
              "8.7",
              "8.8",
              "8.9",
              "8.10",
              "8.11",
              "8.12",
              "8.13",
              "8.14",
              "9.0.0",
              "9.1.0",
              "9.2.0",
              "9.3.0",
              "9.4.0",
              "9.5.0",
              "9.6.0",
          ]
  )
  fun `generates package-info, adds dependency and compiles`(gradleVersion: String) {
    val project = TestProject(projectDir)
    project.writeStandardBuild(rootProjectName = "compat-under-test")
    project.writeSampleSources()

    val result = project.runner("compileJava").withGradleVersion(gradleVersion).build()

    assertThat(result.task(":generatePackageInfo")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
    assertThat(result.task(":compileJava")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
    assertThat(project.generatedPackageInfo("com.acme")).exists()
    assertThat(project.generatedPackageInfo("com.acme.manual")).doesNotExist()
    assertThat(project.compiledClass("com.acme.package-info")).exists()
  }
}
