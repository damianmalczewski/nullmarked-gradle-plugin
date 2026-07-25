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
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class PackageFilterTest {

  @Test
  fun `excludes nothing without rules`() {
    val filter = PackageFilter(emptyList())

    assertThat(filter.isExcluded("com.acme")).isFalse()
  }

  @Test
  fun `keeps packages matched by no rule`() {
    val filter = PackageFilter(listOf("-com.acme.."))

    assertThat(filter.isExcluded("com.other")).isFalse()
  }

  @Test
  fun `skips excluded packages`() {
    val filter = PackageFilter(listOf("-com.acme.."))

    assertThat(filter.isExcluded("com.acme.internal")).isTrue()
  }

  @Test
  fun `later include re-admits an excluded package`() {
    val filter = PackageFilter(listOf("-..internal..", "+com.acme.internal"))

    assertThat(filter.isExcluded("com.acme.internal")).isFalse()
    assertThat(filter.isExcluded("com.other.internal")).isTrue()
  }

  @Test
  fun `later exclude wins over an earlier include`() {
    val filter = PackageFilter(listOf("+com.acme..", "-com.acme.internal.."))

    assertThat(filter.isExcluded("com.acme.api")).isFalse()
    assertThat(filter.isExcluded("com.acme.internal.impl")).isTrue()
  }

  @Test
  fun `only the last matching rule counts`() {
    val filter = PackageFilter(listOf("-com.acme..", "+com.acme..", "-com.acme.."))

    assertThat(filter.isExcluded("com.acme")).isTrue()
  }

  @Test
  fun `rules round-trip through their encoded form`() {
    val excluded = PackageRule(included = false, identifier = "com.acme..")
    val included = PackageRule(included = true, identifier = "com.acme.api")

    assertThat(excluded.encode()).isEqualTo("-com.acme..")
    assertThat(included.encode()).isEqualTo("+com.acme.api")
    assertThat(PackageRule.decode(excluded.encode())).isEqualTo(excluded)
    assertThat(PackageRule.decode(included.encode())).isEqualTo(included)
  }

  @Test
  fun `rejects an unprefixed rule`() {
    assertThatThrownBy { PackageRule.decode("com.acme") }.isInstanceOf(InvalidInputException::class.java)
  }

  @Test
  fun `rejects an invalid package identifier`() {
    assertThatThrownBy { PackageFilter(listOf("-com acme")) }.isInstanceOf(InvalidInputException::class.java)
  }
}
