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
 * A single `package-info.java` problem found by [findPackageInfoViolations], reported by the verification task.
 *
 * @property packageName package the violation belongs to
 */
internal sealed class PackageInfoViolation(val packageName: String) {

  /** No `package-info.java` at all. */
  class Missing(packageName: String) : PackageInfoViolation(packageName)

  /** `package-info.java` exists but declares neither `@NullMarked` nor `@NullUnmarked`. */
  class Bare(packageName: String) : PackageInfoViolation(packageName)

  /** `package-info.java` declares `@NullUnmarked`, but [VerificationMode.STRICT] requires `@NullMarked`. */
  class NotNullMarked(packageName: String) : PackageInfoViolation(packageName)

  /** `package-info.java` declares both `@NullMarked` and `@NullUnmarked`. */
  class Conflicting(packageName: String) : PackageInfoViolation(packageName)

  /**
   * Explanation appended after the package name in the verification failure message.
   *
   * @return a human-readable description of this violation
   */
  fun reason(): String =
      when (this) {
        is Missing -> "missing package-info.java"
        is Bare -> "package-info.java present but declares neither @NullMarked nor @NullUnmarked"
        is NotNullMarked -> "package-info.java declares @NullUnmarked, but @NullMarked is required in strict mode"
        is Conflicting -> "package-info.java declares both @NullMarked and @NullUnmarked"
      }
}
