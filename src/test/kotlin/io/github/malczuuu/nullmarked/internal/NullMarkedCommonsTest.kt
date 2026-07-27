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
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.gradle.api.GradleException
import org.gradle.api.InvalidUserDataException
import org.gradle.util.GradleVersion
import org.junit.jupiter.api.Test

class NullMarkedCommonsTest {

  @Test
  fun `parses a bare version against the default jspecify coordinate`() {
    val result = parseJSpecifyCoordinate("1.0.0")

    assertThat(result).isEqualTo(JSpecifyCoordinate(JSPECIFY_GROUP, JSPECIFY_NAME, "1.0.0"))
  }

  @Test
  fun `parses a full group-name-version notation`() {
    val result = parseJSpecifyCoordinate("com.example:jspecify-fork:1.2.3")

    assertThat(result).isEqualTo(JSpecifyCoordinate("com.example", "jspecify-fork", "1.2.3"))
  }

  @Test
  fun `rejects notation with the wrong number of segments`() {
    assertThatThrownBy { parseJSpecifyCoordinate("org.jspecify:jspecify") }
        .isInstanceOf(InvalidUserDataException::class.java)
        .hasMessageContaining("org.jspecify:jspecify")
  }

  @Test
  fun `rejects a gradle older than the minimum supported one`() {
    assertThatThrownBy { requireMinimumGradleVersion(GradleVersion.version("8.2")) }
        .isInstanceOf(GradleException::class.java)
        .hasMessageContaining("requires Gradle $MINIMUM_GRADLE_VERSION or later")
        .hasMessageContaining("to Gradle 8.2")
  }

  @Test
  fun `accepts the minimum supported gradle`() {
    assertThatCode { requireMinimumGradleVersion(GradleVersion.version(MINIMUM_GRADLE_VERSION)) }
        .doesNotThrowAnyException()
  }

  @Test
  fun `reads the plugin version off the properties resource`() {
    val version = readPluginVersion { "version=1.2.3".byteInputStream() }

    assertThat(version).isEqualTo("1.2.3")
  }

  @Test
  fun `falls back to unknown when the properties resource is missing`() {
    assertThat(readPluginVersion { null }).isEqualTo("unknown")
  }

  @Test
  fun `falls back to unknown when the properties resource carries no version`() {
    assertThat(readPluginVersion { "other=1.2.3".byteInputStream() }).isEqualTo("unknown")
  }

  @Test
  fun `stamps the version baked into the jar at build time`() {
    assertThat(PLUGIN_VERSION).isNotEqualTo("unknown")
  }
}
