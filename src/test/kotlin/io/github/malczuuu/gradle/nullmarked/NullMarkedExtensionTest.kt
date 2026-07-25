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

import org.assertj.core.api.Assertions.assertThat
import org.gradle.api.Project
import org.gradle.kotlin.dsl.create
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class NullMarkedExtensionTest {

  private lateinit var project: Project
  private lateinit var extension: NullMarkedExtension

  @BeforeEach
  fun beforeEach() {
    project = ProjectBuilder.builder().build()
    extension = project.extensions.create<NullMarkedExtension>("nullmarked")
    extension.enabled.convention(true)
    extension.headerEnabled.convention(true)
    extension.excludedPackages.convention(emptyList())
  }

  @Test
  fun `sourceSet spec inherits top-level defaults`() {
    extension.excludedPackages.set(listOf("com.acme.."))

    val spec = extension.sourceSets.maybeCreate("test")

    assertThat(spec.enabled.get()).isTrue()
    assertThat(spec.headerEnabled.get()).isTrue()
    assertThat(spec.excludedPackages.get()).containsExactly("com.acme..")
  }

  @Test
  fun `sourceSet spec tracks later top-level changes when not overridden`() {
    val spec = extension.sourceSets.maybeCreate("test")

    extension.enabled.set(false)

    assertThat(spec.enabled.get()).isFalse()
  }

  @Test
  fun `sourceSet block overrides only the properties it sets`() {
    extension.excludedPackages.set(listOf("com.acme.."))

    extension.sourceSet("test") { headerEnabled.set(false) }
    val spec = extension.sourceSets.getByName("test")

    assertThat(spec.headerEnabled.get()).isFalse()
    assertThat(spec.enabled.get()).isTrue()
    assertThat(spec.excludedPackages.get()).containsExactly("com.acme..")
  }

  @Test
  fun `no-arg sourceSet opts a source set in without overriding anything`() {
    extension.excludedPackages.set(listOf("com.acme.."))

    extension.sourceSet("main21")
    val spec = extension.sourceSets.getByName("main21")

    assertThat(spec.enabled.get()).isTrue()
    assertThat(spec.headerEnabled.get()).isTrue()
    assertThat(spec.excludedPackages.get()).containsExactly("com.acme..")
  }

  @Test
  fun `sourceSet is idempotent for the same name`() {
    extension.sourceSet("test") { enabled.set(false) }
    extension.sourceSet("test") { headerEnabled.set(false) }

    val spec = extension.sourceSets.getByName("test")

    assertThat(spec.enabled.get()).isFalse()
    assertThat(spec.headerEnabled.get()).isFalse()
    assertThat(extension.sourceSets).hasSize(1)
  }
}
