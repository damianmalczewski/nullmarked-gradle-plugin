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
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

/**
 * Runs the core plugin scenario against the Gradle version given by the `compat.gradle.version` system property
 * (`-Pcompat.gradle.version=...` on the build; the CI workflow supplies it from its version matrix). Without the
 * property, the current TestKit Gradle version is used. Run via the `integrationTest` suite.
 */
class GradleCompatibilityTest {

  @TempDir lateinit var projectDir: File

  @Test
  fun `generates package-info, adds dependency and compiles`() {
    val gradleVersion: String? = System.getProperty("compat.gradle.version")
    val project = TestProject(projectDir)
    project.writeStandardBuild(rootProjectName = "compat-under-test")
    project.writeSampleSources()

    val runner = project.runner("compileJava")
    gradleVersion?.let(runner::withGradleVersion)
    val result = runner.build()

    assertThat(result.task(":generatePackageInfo")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
    assertThat(result.task(":compileJava")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
    assertThat(project.generatedPackageInfo("com.acme")).exists()
    assertThat(project.generatedPackageInfo("com.acme.manual")).doesNotExist()
    assertThat(project.compiledClass("com.acme.package-info")).exists()
  }
}
