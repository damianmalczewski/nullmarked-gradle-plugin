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
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.gradle.api.InvalidUserDataException
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.getByName
import org.gradle.kotlin.dsl.getByType
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class NullMarkedPluginTest {

  private lateinit var project: Project

  @BeforeEach
  fun beforeEach() {
    project = ProjectBuilder.builder().build()
  }

  private fun applyPlugins() {
    project.plugins.apply("java")
    project.plugins.apply("io.github.malczuuu.nullmarked")
  }

  private fun compileOnlyDependencies(): List<String> {
    // Graph resolution fires the withDependencies hook; missing repositories only make
    // individual components unresolvable, which is irrelevant here.
    project.configurations.getByName("compileClasspath").incoming.resolutionResult.root
    return project.configurations.getByName("compileOnly").dependencies.map { "${it.group}:${it.name}:${it.version}" }
  }

  private fun compileOnlyJSpecifyDependencies(): List<String> =
      compileOnlyDependencies().filter { it.startsWith("org.jspecify:") }

  @Test
  fun `registers extension with defaults`() {
    applyPlugins()

    val extension = project.extensions.getByType<NullMarkedExtension>()

    assertThat(extension.enabled.get()).isTrue()
    assertThat(extension.jspecifyVersion.get()).isEqualTo(JSPECIFY_VERSION)
  }

  @Test
  fun `registers generatePackageInfo task for java projects`() {
    applyPlugins()

    val task = project.tasks.getByName("generatePackageInfo")

    assertThat(task).isInstanceOf(GeneratePackageInfoTask::class.java)
  }

  @Test
  fun `does not register task without the java plugin`() {
    project.plugins.apply("io.github.malczuuu.nullmarked")

    assertThat(project.tasks.findByName("generatePackageInfo")).isNull()
    assertThat(project.extensions.findByType<NullMarkedExtension>()).isNotNull()
  }

  @Test
  fun `adds generated directory to the main source set`() {
    applyPlugins()

    val mainSourceSet =
        project.extensions.getByType<JavaPluginExtension>().sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME)
    val expected = project.layout.buildDirectory.dir("generated/sources/nullmarked/java/main").get().asFile

    assertThat(mainSourceSet.java.srcDirs).contains(expected)
  }

  @Test
  fun `task scans only hand-written source directories`() {
    applyPlugins()

    val task = project.tasks.getByName<GeneratePackageInfoTask>("generatePackageInfo")
    val generatedDir = task.outputDirectory.get().asFile

    assertThat(task.sourceDirectories.files)
        .contains(File(project.projectDir, "src/main/java"))
        .doesNotContain(generatedDir)
  }

  @Test
  fun `excludes generated package-info from javadoc`() {
    applyPlugins()

    val task = project.tasks.getByName<GeneratePackageInfoTask>("generatePackageInfo")
    val generatedDir = task.outputDirectory.get().asFile
    val generatedPackageInfo =
        File(generatedDir, "com/acme/package-info.java").apply {
          parentFile.mkdirs()
          writeText("package com.acme;")
        }
    val handWrittenSource =
        File(project.projectDir, "src/main/java/com/acme/Foo.java").apply {
          parentFile.mkdirs()
          writeText("package com.acme; class Foo {}")
        }

    val javadocSource = project.tasks.getByName<Javadoc>("javadoc").source.files

    assertThat(javadocSource).doesNotContain(generatedPackageInfo).contains(handWrittenSource)
  }

  @Test
  fun `adds jspecify as compileOnly dependency by default`() {
    applyPlugins()

    assertThat(compileOnlyJSpecifyDependencies()).containsExactly("org.jspecify:jspecify:${JSPECIFY_VERSION}")
  }

  @Test
  fun `respects configured jspecifyVersion`() {
    applyPlugins()
    project.extensions.getByType<NullMarkedExtension>().jspecifyVersion.set("0.9.0")

    assertThat(compileOnlyJSpecifyDependencies()).containsExactly("org.jspecify:jspecify:0.9.0")
  }

  @Test
  fun `accepts full dependency notation to use a jspecify fork`() {
    applyPlugins()
    project.extensions.getByType<NullMarkedExtension>().jspecifyVersion.set("com.example:jspecify-fork:1.2.3")

    assertThat(compileOnlyDependencies()).containsExactly("com.example:jspecify-fork:1.2.3")
  }

  @Test
  fun `rejects malformed jspecifyVersion notation`() {
    applyPlugins()
    project.extensions.getByType<NullMarkedExtension>().jspecifyVersion.set("org.jspecify:jspecify")

    assertThatThrownBy { compileOnlyDependencies() }.isInstanceOf(InvalidUserDataException::class.java)
  }

  @Test
  fun `does not add dependency when user declares jspecify in compileOnly`() {
    applyPlugins()
    project.dependencies.add("compileOnly", "org.jspecify:jspecify:0.3.0")

    assertThat(compileOnlyJSpecifyDependencies()).containsExactly("org.jspecify:jspecify:0.3.0")
  }

  @Test
  fun `does not add dependency when user declares jspecify in implementation`() {
    applyPlugins()
    project.dependencies.add("implementation", "org.jspecify:jspecify:0.3.0")

    assertThat(compileOnlyJSpecifyDependencies()).isEmpty()
  }

  @Test
  fun `does not add dependency when user declares jspecify in api`() {
    project.plugins.apply("java-library")
    project.plugins.apply("io.github.malczuuu.nullmarked")
    project.dependencies.add("api", "org.jspecify:jspecify:0.3.0")

    assertThat(compileOnlyJSpecifyDependencies()).isEmpty()
  }

  @Test
  fun `main source set spec is always present`() {
    applyPlugins()

    assertThat(project.extensions.getByType<NullMarkedExtension>().sourceSets.names)
        .contains(SourceSet.MAIN_SOURCE_SET_NAME)
  }

  @Test
  fun `does not register a task for an unconfigured source set`() {
    applyPlugins()

    assertThat(project.tasks.findByName("generateTestPackageInfo")).isNull()
  }

  @Test
  fun `registers a name-derived task for a configured source set`() {
    applyPlugins()
    project.extensions.getByType<NullMarkedExtension>().sourceSet("test") {}

    val task = project.tasks.getByName("generateTestPackageInfo")

    assertThat(task).isInstanceOf(GeneratePackageInfoTask::class.java)
  }

  @Test
  fun `adds generated directory to a configured non-main source set`() {
    applyPlugins()
    project.extensions.getByType<NullMarkedExtension>().sourceSet("test") {}

    val testSourceSet =
        project.extensions.getByType<JavaPluginExtension>().sourceSets.getByName(SourceSet.TEST_SOURCE_SET_NAME)
    val expected = project.layout.buildDirectory.dir("generated/sources/nullmarked/java/test").get().asFile

    assertThat(testSourceSet.java.srcDirs).contains(expected)
  }

  @Test
  fun `adds jspecify to testCompileOnly for a configured test source set`() {
    applyPlugins()
    project.extensions.getByType<NullMarkedExtension>().sourceSet("test") {}
    // Graph resolution fires the withDependencies hook; missing repositories only make individual
    // components unresolvable, which is irrelevant here.
    project.configurations.getByName("testCompileClasspath").incoming.resolutionResult.root

    val testCompileOnlyDependencies =
        project.configurations.getByName("testCompileOnly").dependencies.map {
          "${it.group}:${it.name}:${it.version}"
        }

    assertThat(testCompileOnlyDependencies).containsExactly("org.jspecify:jspecify:${JSPECIFY_VERSION}")
  }

  @Test
  fun `non-main source set does not check api or compileOnlyApi for an existing declaration`() {
    project.plugins.apply("java-library")
    project.plugins.apply("io.github.malczuuu.nullmarked")
    project.extensions.getByType<NullMarkedExtension>().sourceSet("test") {}
    project.dependencies.add("api", "org.jspecify:jspecify:0.3.0")
    project.configurations.getByName("testCompileClasspath").incoming.resolutionResult.root

    val testCompileOnlyDependencies =
        project.configurations.getByName("testCompileOnly").dependencies.map {
          "${it.group}:${it.name}:${it.version}"
        }

    assertThat(testCompileOnlyDependencies).containsExactly("org.jspecify:jspecify:${JSPECIFY_VERSION}")
  }
}
