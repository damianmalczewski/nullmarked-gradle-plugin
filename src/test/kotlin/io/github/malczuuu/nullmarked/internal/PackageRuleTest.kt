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

package io.github.malczuuu.nullmarked.internal

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.gradle.api.InvalidUserDataException
import org.junit.jupiter.api.Test

class PackageRuleTest {

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
    assertThatThrownBy { PackageRule.decode("com.acme") }.isInstanceOf(InvalidUserDataException::class.java)
  }
}
