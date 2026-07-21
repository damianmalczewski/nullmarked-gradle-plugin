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
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class PackagePatternTest {

  @Test
  fun `plain identifier matches only the exact package`() {
    val pattern = PackagePattern.of("org.acme")

    assertThat(pattern.matches("org.acme")).isTrue()
    assertThat(pattern.matches("org.acme.util")).isFalse()
    assertThat(pattern.matches("org")).isFalse()
    assertThat(pattern.matches("org.acmeutil")).isFalse()
  }

  @Test
  fun `trailing dot-dot matches the package and all subpackages`() {
    val pattern = PackagePattern.of("org.acme..")

    assertThat(pattern.matches("org.acme")).isTrue()
    assertThat(pattern.matches("org.acme.util")).isTrue()
    assertThat(pattern.matches("org.acme.util.internal")).isTrue()
    assertThat(pattern.matches("org.acmeutil")).isFalse()
    assertThat(pattern.matches("org")).isFalse()
  }

  @Test
  fun `leading dot-dot matches the package under any parent`() {
    val pattern = PackagePattern.of("..internal")

    assertThat(pattern.matches("internal")).isTrue()
    assertThat(pattern.matches("org.acme.internal")).isTrue()
    assertThat(pattern.matches("org.internal.acme")).isFalse()
  }

  @Test
  fun `middle dot-dot matches any packages in between`() {
    val pattern = PackagePattern.of("org..util")

    assertThat(pattern.matches("org.util")).isTrue()
    assertThat(pattern.matches("org.acme.util")).isTrue()
    assertThat(pattern.matches("org.acme.deep.util")).isTrue()
    assertThat(pattern.matches("org.acme.utils")).isFalse()
  }

  @Test
  fun `surrounding dot-dot matches any package containing the segment`() {
    val pattern = PackagePattern.of("..internal..")

    assertThat(pattern.matches("internal")).isTrue()
    assertThat(pattern.matches("org.internal")).isTrue()
    assertThat(pattern.matches("org.internal.acme")).isTrue()
    assertThat(pattern.matches("org.acme")).isFalse()
  }

  @Test
  fun `star matches within a single segment only`() {
    val pattern = PackagePattern.of("org.*.util")

    assertThat(pattern.matches("org.acme.util")).isTrue()
    assertThat(pattern.matches("org.acme.deep.util")).isFalse()
    assertThat(pattern.matches("org.util")).isFalse()
  }

  @Test
  fun `dot-dot alone matches every package`() {
    val pattern = PackagePattern.of("..")

    assertThat(pattern.matches("org")).isTrue()
    assertThat(pattern.matches("org.acme.util")).isTrue()
  }

  @Test
  fun `rejects invalid identifiers`() {
    assertThatThrownBy { PackagePattern.of("org...acme") }.isInstanceOf(IllegalArgumentException::class.java)
    assertThatThrownBy { PackagePattern.of(".org.acme") }.isInstanceOf(IllegalArgumentException::class.java)
    assertThatThrownBy { PackagePattern.of("org.acme.") }.isInstanceOf(IllegalArgumentException::class.java)
    assertThatThrownBy { PackagePattern.of("") }.isInstanceOf(IllegalArgumentException::class.java)
  }
}
