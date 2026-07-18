/*
 * Copyright 2026-current Damian Malczewski
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

package io.github.malczuuu.gradle.jspecify

import org.gradle.api.provider.Property

/** Configuration for the JSpecify plugin, available in build scripts as `jspecify { ... }`. */
abstract class JSpecifyExtension {

  /**
   * Whether `package-info.java` files annotated with `@NullMarked` should be generated for non-empty packages that do
   * not declare one. Defaults to `true`.
   */
  abstract val generatePackageInfo: Property<Boolean>

  /**
   * Version of the `org.jspecify:jspecify` artifact added as a `compileOnly` dependency when the build script does not
   * declare JSpecify itself. Defaults to [JSpecifyPlugin.DEFAULT_JSPECIFY_VERSION].
   */
  abstract val jspecifyVersion: Property<String>
}
