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
    extension.verifyOnly.convention(false)
  }

  @Test
  fun `sourceSet spec inherits top-level defaults`() {
    extension.packages { exclude("com.acme..") }

    val spec = extension.sourceSets.maybeCreate("test")

    assertThat(spec.enabled.get()).isTrue()
    assertThat(spec.headerEnabled.get()).isTrue()
    assertThat(spec.verifyOnly.get()).isFalse()
    assertThat(spec.encodedRules).containsExactly("-com.acme..")
  }

  @Test
  fun `sourceSet spec inherits a top-level verifyOnly and can override it`() {
    extension.verifyOnly.set(true)

    val inheriting = extension.sourceSets.maybeCreate("test")
    extension.sourceSet("main21") { verifyOnly.set(false) }

    assertThat(inheriting.verifyOnly.get()).isTrue()
    assertThat(extension.sourceSets.getByName("main21").verifyOnly.get()).isFalse()
  }

  @Test
  fun `sourceSet spec tracks later top-level changes when not overridden`() {
    val spec = extension.sourceSets.maybeCreate("test")

    extension.enabled.set(false)

    assertThat(spec.enabled.get()).isFalse()
  }

  @Test
  fun `sourceSet block overrides only the properties it sets`() {
    extension.packages { exclude("com.acme..") }

    extension.sourceSet("test") { headerEnabled.set(false) }
    val spec = extension.sourceSets.getByName("test")

    assertThat(spec.headerEnabled.get()).isFalse()
    assertThat(spec.enabled.get()).isTrue()
    assertThat(spec.encodedRules).containsExactly("-com.acme..")
  }

  @Test
  fun `no-arg sourceSet opts a source set in without overriding anything`() {
    extension.packages { exclude("com.acme..") }

    extension.sourceSet("main21")
    val spec = extension.sourceSets.getByName("main21")

    assertThat(spec.enabled.get()).isTrue()
    assertThat(spec.headerEnabled.get()).isTrue()
    assertThat(spec.encodedRules).containsExactly("-com.acme..")
  }

  @Test
  fun `sourceSet rules follow the top-level ones whatever the block order`() {
    extension.sourceSet("test") { packages { include("com.acme.internal") } }
    extension.packages { exclude("..internal..") }

    assertThat(extension.sourceSets.getByName("test").encodedRules)
        .containsExactly("-..internal..", "+com.acme.internal")
  }

  @Test
  fun `rules of repeated blocks accumulate in declaration order`() {
    extension.packages { exclude("com.acme..") }
    extension.packages {
      include("com.acme.api")
      exclude("com.acme.api.internal")
    }

    assertThat(extension.sourceSets.maybeCreate("test").encodedRules)
        .containsExactly("-com.acme..", "+com.acme.api", "-com.acme.api.internal")
  }

  @Test
  fun `sourceSet rules do not leak into other source sets`() {
    extension.packages { exclude("com.acme..") }
    extension.sourceSet("test") { packages { include("com.acme.fixtures") } }
    extension.sourceSet("main21")

    assertThat(extension.sourceSets.getByName("main21").encodedRules).containsExactly("-com.acme..")
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

  private val NullMarkedSourceSetSpec.encodedRules: List<String>
    get() = encodedPackageSelectionRules().get()
}
