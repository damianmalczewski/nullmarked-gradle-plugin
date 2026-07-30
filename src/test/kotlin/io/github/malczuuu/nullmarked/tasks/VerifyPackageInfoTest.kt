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

package io.github.malczuuu.nullmarked.tasks

import io.github.malczuuu.nullmarked.fixtures.TestProject
import io.github.malczuuu.nullmarked.internal.PackageRule
import io.github.malczuuu.nullmarked.internal.VerificationMode
import java.io.File
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.VerificationException
import org.gradle.kotlin.dsl.listProperty
import org.gradle.kotlin.dsl.property
import org.gradle.kotlin.dsl.register
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class VerifyPackageInfoTest {

  @TempDir lateinit var projectDir: File

  private lateinit var project: Project
  private lateinit var testProject: TestProject
  private lateinit var sourceDir: File
  private lateinit var markerFile: File
  private lateinit var task: VerifyPackageInfo

  @BeforeEach
  fun beforeEach() {
    project = ProjectBuilder.builder().withProjectDir(projectDir).build()
    testProject = TestProject(projectDir)
    sourceDir = testProject.file("src/main/java").apply { mkdirs() }
    markerFile = testProject.file("build/tmp/nullmarked/verifyPackageInfo/verification.txt")
    task =
        project.tasks
            .register<VerifyPackageInfo>("verifyPackageInfo") {
              sourceDirectories.from(sourceDir)
              buildCacheMarker.set(this@VerifyPackageInfoTest.markerFile)
            }
            .get()
  }

  private fun writeSource(relativePath: String, content: String = "class Placeholder {}") {
    testProject.writeSource(relativePath, content)
  }

  /** Stands in for the rules the plugin feeds the task from the `nullmarked { ... }` DSL. */
  private fun inheritedRules(vararg rules: PackageRule): Provider<List<PackageRule>> =
      project.objects.listProperty<PackageRule>().apply { set(rules.toList()) }

  /** Stands in for the mode the plugin feeds the task from the `nullmarked { ... }` DSL. */
  private fun inheritedMode(mode: VerificationMode): Provider<VerificationMode> =
      project.objects.property<VerificationMode>().apply { set(mode) }

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
        .hasMessageContaining("com.acme: missing package-info.java")
        .hasMessageContaining("com.other: missing package-info.java")
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
        .hasMessageContaining("nullmarked.packages")
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
    task.packages { exclude("com.acme..") }

    assertThatCode { task.verifyPackageInfos() }.doesNotThrowAnyException()
  }

  @Test
  fun `passes when the only offending package is excluded by an inherited rule`() {
    writeSource("com/acme/Foo.java")
    task.inheritPackageRules(inheritedRules(PackageRule(included = false, identifier = "com.acme..")))

    assertThatCode { task.verifyPackageInfos() }.doesNotThrowAnyException()
  }

  @Test
  fun `evaluates the task's own packages block after the inherited rules`() {
    writeSource("com/acme/Foo.java")
    task.inheritPackageRules(inheritedRules(PackageRule(included = false, identifier = "com.acme..")))
    task.packages { include("com.acme") }

    assertThatThrownBy { task.verifyPackageInfos() }.hasMessageContaining("com.acme")
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

  @Test
  fun `lenient mode ignores content of an existing package-info`() {
    writeSource("com/acme/Foo.java")
    writeSource("com/acme/package-info.java", "package com.acme;")
    task.verify { lenient() }

    assertThatCode { task.verifyPackageInfos() }.doesNotThrowAnyException()
  }

  @Test
  fun `explicit mode fails a bare package-info`() {
    writeSource("com/acme/Foo.java")
    writeSource("com/acme/package-info.java", "package com.acme;")
    task.verify { explicit() }

    assertThatThrownBy { task.verifyPackageInfos() }
        .hasMessageContaining("com.acme: package-info.java present but declares neither @NullMarked nor @NullUnmarked")
  }

  @Test
  fun `explicit mode passes a package-info declaring NullMarked`() {
    writeSource("com/acme/Foo.java")
    writeSource(
        "com/acme/package-info.java",
        """
        @NullMarked
        package com.acme;

        import org.jspecify.annotations.NullMarked;
        """,
    )
    task.verify { explicit() }

    assertThatCode { task.verifyPackageInfos() }.doesNotThrowAnyException()
  }

  @Test
  fun `explicit mode passes a package-info declaring NullUnmarked`() {
    writeSource("com/acme/Foo.java")
    writeSource(
        "com/acme/package-info.java",
        """
        @NullUnmarked
        package com.acme;

        import org.jspecify.annotations.NullUnmarked;
        """,
    )
    task.verify { explicit() }

    assertThatCode { task.verifyPackageInfos() }.doesNotThrowAnyException()
  }

  @Test
  fun `explicit mode recognizes a fully-qualified annotation`() {
    writeSource("com/acme/Foo.java")
    writeSource(
        "com/acme/package-info.java",
        """
        @org.jspecify.annotations.NullMarked
        package com.acme;
        """,
    )
    task.verify { explicit() }

    assertThatCode { task.verifyPackageInfos() }.doesNotThrowAnyException()
  }

  @Test
  fun `strict mode fails a package-info declaring only NullUnmarked`() {
    writeSource("com/acme/Foo.java")
    writeSource(
        "com/acme/package-info.java",
        """
        @NullUnmarked
        package com.acme;

        import org.jspecify.annotations.NullUnmarked;
        """,
    )
    task.verify { strict() }

    assertThatThrownBy { task.verifyPackageInfos() }
        .hasMessageContaining("com.acme: package-info.java declares @NullUnmarked, but @NullMarked is required")
  }

  @Test
  fun `strict mode fails a bare package-info`() {
    writeSource("com/acme/Foo.java")
    writeSource("com/acme/package-info.java", "package com.acme;")
    task.verify { strict() }

    assertThatThrownBy { task.verifyPackageInfos() }.hasMessageContaining("declares neither @NullMarked")
  }

  @Test
  fun `strict mode passes a package-info declaring NullMarked`() {
    writeSource("com/acme/Foo.java")
    writeSource(
        "com/acme/package-info.java",
        """
        @NullMarked
        package com.acme;

        import org.jspecify.annotations.NullMarked;
        """,
    )
    task.verify { strict() }

    assertThatCode { task.verifyPackageInfos() }.doesNotThrowAnyException()
  }

  @Test
  fun `annotations are recognized in any order`() {
    writeSource("com/acme/Foo.java")
    writeSource(
        "com/acme/package-info.java",
        """
        package com.acme;

        import org.jspecify.annotations.NullMarked;

        @NullMarked class Dummy {}
        """,
    )
    task.verify { explicit() }

    assertThatCode { task.verifyPackageInfos() }.doesNotThrowAnyException()
  }

  @Test
  fun `explicit mode fails a package-info declaring both NullMarked and NullUnmarked`() {
    writeSource("com/acme/Foo.java")
    writeSource(
        "com/acme/package-info.java",
        """
        @NullMarked
        @NullUnmarked
        package com.acme;
        """,
    )
    task.verify { explicit() }

    assertThatThrownBy { task.verifyPackageInfos() }
        .hasMessageContaining("com.acme: package-info.java declares both @NullMarked and @NullUnmarked")
  }

  @Test
  fun `strict mode also fails conflicting annotations`() {
    writeSource("com/acme/Foo.java")
    writeSource(
        "com/acme/package-info.java",
        """
        @NullMarked
        @NullUnmarked
        package com.acme;
        """,
    )
    task.verify { strict() }

    assertThatThrownBy { task.verifyPackageInfos() }.hasMessageContaining("declares both @NullMarked and @NullUnmarked")
  }

  @Test
  fun `defaults to lenient mode`() {
    writeSource("com/acme/Foo.java")
    writeSource("com/acme/package-info.java", "package com.acme;")

    assertThatCode { task.verifyPackageInfos() }.doesNotThrowAnyException()
  }

  @Test
  fun `inherits the mode fed by the plugin`() {
    writeSource("com/acme/Foo.java")
    writeSource("com/acme/package-info.java", "package com.acme;")
    task.inheritVerificationMode(inheritedMode(VerificationMode.STRICT))

    assertThatThrownBy { task.verifyPackageInfos() }.hasMessageContaining("declares neither @NullMarked")
  }

  @Test
  fun `the task's own verify block overrides an inherited mode`() {
    writeSource("com/acme/Foo.java")
    writeSource("com/acme/package-info.java", "package com.acme;")
    task.inheritVerificationMode(inheritedMode(VerificationMode.STRICT))
    task.verify { lenient() }

    assertThatCode { task.verifyPackageInfos() }.doesNotThrowAnyException()
  }
}
