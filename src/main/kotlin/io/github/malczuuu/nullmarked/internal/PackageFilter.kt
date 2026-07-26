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

import org.gradle.api.InvalidUserDataException

/**
 * Decides whether a package is processed, following the rules of the `nullmarked { packages { ... } }` block: the last
 * rule matching a package wins, and a package matched by no rule at all is processed.
 *
 * @param encodedRules rules in declaration order, as encoded by [PackageRule.encode]
 * @throws InvalidUserDataException if a rule is malformed or carries an invalid package identifier
 */
internal class PackageFilter(encodedRules: List<String>) {

  private val rules: List<Pair<PackagePattern, Boolean>> =
      encodedRules.map { PackageRule.decode(it) }.map { PackagePattern(it.identifier) to !it.included }

  /**
   * Checks [packageName] against the rules, letting the last matching one decide. Packages no rule matches are not
   * excluded.
   *
   * @param packageName package to check, e.g. `com.acme.internal`
   * @return whether the package is skipped
   */
  fun isExcluded(packageName: String): Boolean =
      rules.lastOrNull { (pattern, _) -> pattern.matches(packageName) }?.second ?: false
}
