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

import java.util.Properties
import org.gradle.api.InvalidUserDataException

/** Default value of `nullmarked.jspecifyVersion` used when the build script does not configure one. */
internal const val JSPECIFY_VERSION = "1.0.0"

/** Group of the default `org.jspecify:jspecify` dependency coordinate. */
internal const val JSPECIFY_GROUP = "org.jspecify"

/** Name of the default `org.jspecify:jspecify` dependency coordinate. */
internal const val JSPECIFY_NAME = "jspecify"

/** ArchUnit-style package identifier syntax accepted by [PackagePattern.of]. */
internal val VALID_IDENTIFIER = Regex("""(\.\.)?[\w*]+((\.|\.\.)[\w*]+)*(\.\.)?|\.\.""")

/** Plugin version baked into the jar as a resource at build time; `unknown` if the resource is missing. */
internal val PLUGIN_VERSION: String =
    ResourceAnchor::class.java.getResourceAsStream("nullmarked.properties")?.use { stream ->
      Properties().apply { load(stream) }.getProperty("version")
    } ?: "unknown"

/**
 * A resolved `group:name:version` dependency coordinate for a JSpecify (or JSpecify fork) artifact.
 *
 * @property group dependency group, e.g. `org.jspecify`
 * @property name dependency name, e.g. `jspecify`
 * @property version dependency version
 */
internal data class JSpecifyCoordinate(val group: String, val name: String, val version: String)

/**
 * Accepts either a bare version (`"1.0.0"`, applied to the default `org.jspecify:jspecify` coordinate) or a full
 * `group:name:version` dependency notation, letting users point at a fork of JSpecify.
 *
 * @param value `nullmarked.jspecifyVersion` value to parse
 * @return the resolved coordinate
 * @throws InvalidUserDataException if [value] is neither a bare version nor a full `group:name:version` notation
 */
internal fun parseJspecifyCoordinate(value: String): JSpecifyCoordinate {
  val parts = value.split(":")
  return when (parts.size) {
    1 -> JSpecifyCoordinate(JSPECIFY_GROUP, JSPECIFY_NAME, parts[0])
    3 -> JSpecifyCoordinate(parts[0], parts[1], parts[2])
    else ->
        throw InvalidUserDataException(
            "nullmarked.jspecifyVersion must be either a version (e.g. \"1.0.0\") or a full dependency notation " +
                "(e.g. \"org.jspecify:jspecify:1.0.0\"), but was: \"$value\""
        )
  }
}

private class ResourceAnchor
