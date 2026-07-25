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
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.gradle.api.tasks.VerificationException
import org.gradle.kotlin.dsl.register
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class VerifyPackageInfoTaskTest {

  @TempDir lateinit var projectDir: File

  private lateinit var testProject: TestProject
  private lateinit var sourceDir: File
  private lateinit var markerFile: File
  private lateinit var task: VerifyPackageInfoTask

  @BeforeEach
  fun beforeEach() {
    val project = ProjectBuilder.builder().withProjectDir(projectDir).build()
    testProject = TestProject(projectDir)
    sourceDir = testProject.file("src/main/java").apply { mkdirs() }
    markerFile = testProject.file("build/tmp/nullmarked/verifyPackageInfo/verification.txt")
    task =
        project.tasks
            .register<VerifyPackageInfoTask>("verifyPackageInfo") {
              sourceDirectories.from(sourceDir)
              markerFile.set(this@VerifyPackageInfoTaskTest.markerFile)
            }
            .get()
  }

  private fun writeSource(relativePath: String, content: String = "class Placeholder {}") {
    testProject.writeSource(relativePath, content)
  }

  @Test
  fun `passes when every package declares a package-info`() {
    writeSource("com/acme/Foo.java")
    writeSource("com/acme/package-info.java", "package com.acme;")

    assertThatCode { task.verifyPackageInfos() }.doesNotThrowAnyException()
    assertThat(markerFile).exists()
  }

  @Test
  fun `fails listing every package without a package-info`() {
    writeSource("com/acme/Foo.java")
    writeSource("com/other/Bar.java")

    assertThatThrownBy { task.verifyPackageInfos() }
        .isInstanceOf(VerificationException::class.java)
        .hasMessageContaining("2 package(s)")
        .hasMessageContaining("com.acme")
        .hasMessageContaining("com.other")
    assertThat(markerFile).doesNotExist()
  }

  @Test
  fun `failure suggests disabling verifyOnly only when it is on`() {
    writeSource("com/acme/Foo.java")
    task.verifyOnly.set(true)

    assertThatThrownBy { task.verifyPackageInfos() }.hasMessageContaining("disabling nullmarked.verifyOnly")
  }

  @Test
  fun `failure does not suggest disabling verifyOnly when it is already off`() {
    writeSource("com/acme/Foo.java")

    assertThatThrownBy { task.verifyPackageInfos() }
        .hasMessageContaining("nullmarked.excludedPackages")
        .hasMessageNotContaining("verifyOnly")
  }

  @Test
  fun `passes for a package whose package-info comes from another source directory`() {
    val generatedDir = testProject.file("build/generated/sources/nullmarked/java/main")
    task.sourceDirectories.from(generatedDir)
    writeSource("com/acme/Foo.java")
    TestProject.packageInfoUnder(generatedDir, "com.acme").apply {
      parentFile.mkdirs()
      writeText("package com.acme;")
    }

    assertThatCode { task.verifyPackageInfos() }.doesNotThrowAnyException()
  }

  @Test
  fun `passes when the only offending package is excluded`() {
    writeSource("com/acme/Foo.java")
    task.excludedPackages.set(listOf("com.acme.."))

    assertThatCode { task.verifyPackageInfos() }.doesNotThrowAnyException()
  }

  @Test
  fun `passes without checking when verification is disabled`() {
    writeSource("com/acme/Foo.java")
    task.verificationEnabled.set(false)

    assertThatCode { task.verifyPackageInfos() }.doesNotThrowAnyException()
    assertThat(markerFile).exists()
  }

  @Test
  fun `passes for the default package`() {
    writeSource("TopLevel.java")

    assertThatCode { task.verifyPackageInfos() }.doesNotThrowAnyException()
  }

  @Test
  fun `passes for an empty source set`() {
    assertThatCode { task.verifyPackageInfos() }.doesNotThrowAnyException()
    assertThat(markerFile).exists()
  }

  @Test
  fun `marker content does not change between runs`() {
    writeSource("com/acme/Foo.java")
    writeSource("com/acme/package-info.java", "package com.acme;")
    task.verifyPackageInfos()
    val before = markerFile.readText()

    task.verifyPackageInfos()

    assertThat(markerFile.readText()).isEqualTo(before)
  }
}
