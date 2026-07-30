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

/**
 * How strictly `package-info.java` files are verified, configured via `nullmarked { verify { ... } }`, see
 * [io.github.malczuuu.nullmarked.dsl.NullMarkedVerifySpec]. Excluded packages (via the `packages { ... }` rules) are
 * skipped entirely, whatever the mode.
 */
internal enum class VerificationMode {

  /** A package needs a `package-info.java` to exist; its content is not inspected. */
  LENIENT,

  /** A package needs a `package-info.java` declaring `@NullMarked` or `@NullUnmarked`; a bare file fails. */
  EXPLICIT,

  /** A package needs a `package-info.java` declaring `@NullMarked`; `@NullUnmarked` or a bare file fails. */
  STRICT,
}
