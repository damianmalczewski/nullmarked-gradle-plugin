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

package io.github.malczuuu.nullmarked.dsl

import io.github.malczuuu.nullmarked.internal.VerificationMode
import javax.inject.Inject
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.property

/**
 * How strictly `package-info.java` files are verified, configured via `nullmarked { verify { ... } }` (and the same
 * block inside a `sourceSet(...)`, or directly on a `verifyPackageInfo` task). Whichever method is called last wins:
 * ```
 * nullmarked {
 *     verify {
 *         strict()
 *     }
 * }
 * ```
 *
 * - [lenient] (the default) only requires a `package-info.java` to exist; its content is not inspected.
 * - [explicit] additionally requires it to declare `@NullMarked` or `@NullUnmarked`; a bare file fails.
 * - [strict] additionally requires `@NullMarked` specifically; `@NullUnmarked` or a bare file fails.
 *
 * @param objects factory creating this spec's [mode] property
 */
abstract class NullMarkedVerifySpec @Inject constructor(objects: ObjectFactory) {

  /** Verification strictness, defaulting to lenient unless overridden by an inherited or explicit call. */
  internal val mode: Property<VerificationMode> = objects.property()

  /** Only requires a `package-info.java` to exist; its content is not inspected. */
  fun lenient() {
    mode.set(VerificationMode.LENIENT)
  }

  /** Additionally requires a `package-info.java` to declare `@NullMarked` or `@NullUnmarked`; a bare file fails. */
  fun explicit() {
    mode.set(VerificationMode.EXPLICIT)
  }

  /** Additionally requires `@NullMarked` specifically; `@NullUnmarked` or a bare file fails. */
  fun strict() {
    mode.set(VerificationMode.STRICT)
  }

  /**
   * Encodes [mode] for the verification task, which takes it as a plain string, keeping the internal [VerificationMode]
   * type out of the task's public API.
   *
   * @return this spec's mode, encoded
   */
  internal fun getEncodedMode(): Provider<String> = mode.map { it.name }
}
