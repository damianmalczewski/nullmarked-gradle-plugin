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
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class NullMarkedFunctionsTest {

  @TempDir lateinit var projectDir: File

  private lateinit var testProject: TestProject
  private lateinit var sourceDir: File

  @BeforeEach
  fun beforeEach() {
    testProject = TestProject(projectDir)
    sourceDir = testProject.file("src/main/java").apply { mkdirs() }
  }

  private fun writeSource(relativePath: String, content: String = "class Placeholder {}") {
    testProject.writeSource(relativePath, content)
  }

  @Test
  fun `finds packages containing java files`() {
    writeSource("com/acme/Foo.java")
    writeSource("com/acme/util/Util.java")

    val result = findPackagesToGenerate(listOf(sourceDir), emptyList())

    assertThat(result).containsExactlyInAnyOrder("com.acme", "com.acme.util")
  }

  @Test
  fun `skips the default package`() {
    writeSource("TopLevel.java")

    val result = findPackagesToGenerate(listOf(sourceDir), emptyList())

    assertThat(result).isEmpty()
  }

  @Test
  fun `skips packages with a hand-written package-info`() {
    writeSource("com/acme/Foo.java")
    writeSource("com/acme/package-info.java", "package com.acme;")

    val result = findPackagesToGenerate(listOf(sourceDir), emptyList())

    assertThat(result).doesNotContain("com.acme")
  }

  @Test
  fun `skips intermediate directories without java files`() {
    writeSource("com/acme/deep/Foo.java")

    val result = findPackagesToGenerate(listOf(sourceDir), emptyList())

    assertThat(result).containsExactly("com.acme.deep")
  }

  @Test
  fun `ignores non-java files`() {
    writeSource("com/acme/notes.txt", "not java")

    val result = findPackagesToGenerate(listOf(sourceDir), emptyList())

    assertThat(result).isEmpty()
  }

  @Test
  fun `excludes a single package without touching its subpackages`() {
    writeSource("com/acme/Foo.java")
    writeSource("com/acme/util/Util.java")

    val result = findPackagesToGenerate(listOf(sourceDir), listOf("com.acme"))

    assertThat(result).containsExactly("com.acme.util")
  }

  @Test
  fun `excludes a package subtree with a dot-dot pattern`() {
    writeSource("com/acme/Foo.java")
    writeSource("com/acme/util/Util.java")
    writeSource("com/other/Bar.java")

    val result = findPackagesToGenerate(listOf(sourceDir), listOf("com.acme.."))

    assertThat(result).containsExactly("com.other")
  }

  @Test
  fun `merges packages across multiple source directories`() {
    val secondSourceDir = File(projectDir, "src/main/java-extra").apply { mkdirs() }
    writeSource("com/acme/Foo.java")
    File(secondSourceDir, "com/acme/package-info.java").apply {
      parentFile.mkdirs()
      writeText("package com.acme;")
    }

    val result = findPackagesToGenerate(listOf(sourceDir, secondSourceDir), emptyList())

    assertThat(result).doesNotContain("com.acme")
  }

  @Test
  fun `ignores non-directory entries in source directories`() {
    val notADirectory = File(projectDir, "not-a-dir.txt").apply { writeText("placeholder") }

    val result = findPackagesToGenerate(listOf(notADirectory), emptyList())

    assertThat(result).isEmpty()
  }

  @Test
  fun `finds existing package-info files by package name`() {
    val outputDir = testProject.file("build/generated-package-info")
    TestProject.packageInfoUnder(outputDir, "com.acme").apply {
      parentFile.mkdirs()
      writeText("package com.acme;")
    }

    val result = findExistingPackageInfos(outputDir)

    assertThat(result.keys).containsExactly("com.acme")
    assertThat(result.getValue("com.acme")).exists()
  }

  @Test
  fun `returns no existing files for an empty output directory`() {
    val outputDir = testProject.file("build/generated-package-info").apply { mkdirs() }

    val result = findExistingPackageInfos(outputDir)

    assertThat(result).isEmpty()
  }

  @Test
  fun `expected content contains header, annotation and package declaration`() {
    writeSource("com/acme/Foo.java")

    val result = computeExpectedPackageInfos(listOf(sourceDir), emptyList(), true, "1.2.3")

    assertThat(result.keys).containsExactly("com.acme")
    assertThat(result.getValue("com.acme"))
        .isEqualTo(
            """
            //
            // Generated by NullMarked Gradle Plugin version "1.2.3".
            //

            @org.jspecify.annotations.NullMarked
            package com.acme;
            """
                .trimIndent() + "\n"
        )
  }

  @Test
  fun `expected content omits header when headerEnabled is false`() {
    writeSource("com/acme/Foo.java")

    val result = computeExpectedPackageInfos(listOf(sourceDir), emptyList(), false, "1.2.3")

    assertThat(result.getValue("com.acme"))
        .isEqualTo(
            """
            @org.jspecify.annotations.NullMarked
            package com.acme;
            """
                .trimIndent() + "\n"
        )
  }

  @Test
  fun `expected content excludes matched packages`() {
    writeSource("com/acme/Foo.java")
    writeSource("com/other/Bar.java")

    val result = computeExpectedPackageInfos(listOf(sourceDir), listOf("com.acme"), true, "1.2.3")

    assertThat(result.keys).containsExactly("com.other")
  }
}
