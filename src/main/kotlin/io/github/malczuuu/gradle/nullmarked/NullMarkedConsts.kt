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

/** Default value of `nullmarked.jspecifyVersion` used when the build script does not configure one. */
const val DEFAULT_JSPECIFY_VERSION = "1.0.0"

/** Group of the default `org.jspecify:jspecify` dependency coordinate. */
internal const val JSPECIFY_GROUP = "org.jspecify"

/** Name of the default `org.jspecify:jspecify` dependency coordinate. */
internal const val JSPECIFY_NAME = "jspecify"

/** Name the `GeneratePackageInfoTask` is registered under. */
internal const val TASK_NAME = "generatePackageInfo"

/** ArchUnit-style package identifier syntax accepted by [PackagePattern.of]. */
internal val VALID_IDENTIFIER = Regex("""(\.\.)?[\w*]+((\.|\.\.)[\w*]+)*(\.\.)?|\.\.""")

private class ResourceAnchor

/** Plugin version baked into the jar as a resource at build time; `unknown` if the resource is missing. */
internal val PLUGIN_VERSION: String =
    ResourceAnchor::class.java.getResourceAsStream("nullmarked.properties")?.use { stream ->
      Properties().apply { load(stream) }.getProperty("version")
    } ?: "unknown"
