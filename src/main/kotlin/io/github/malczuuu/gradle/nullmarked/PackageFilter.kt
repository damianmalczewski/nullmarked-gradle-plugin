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

/**
 * A single `include`/`exclude` call argument of the `nullmarked { packages { ... } }` block, paired with the package
 * identifier it applies to. Rules are kept in declaration order and evaluated by [PackageFilter].
 *
 * @param included whether packages matching [identifier] are processed (`include`) or skipped (`exclude`)
 * @param identifier package identifier (ArchUnit syntax, see [PackagePattern])
 */
internal data class PackageRule(val included: Boolean, val identifier: String) {

  /**
   * Encodes this rule as a single string, so it can cross into task inputs without a public type: the [identifier]
   * prefixed with `+` when included and `-` when excluded.
   *
   * @return this rule in its encoded form, e.g. `-com.acme..`
   */
  fun encode(): String = (if (included) INCLUDE_PREFIX else EXCLUDE_PREFIX) + identifier

  companion object {
    private const val INCLUDE_PREFIX = '+'
    private const val EXCLUDE_PREFIX = '-'

    /**
     * Decodes a rule encoded by [encode]. Package identifiers never start with `+` or `-`, so the prefix is
     * unambiguous.
     *
     * @param encoded rule as returned by [encode]
     * @return decoded rule
     * @throws InvalidInputException when [encoded] carries no known prefix
     */
    fun decode(encoded: String): PackageRule =
        when (encoded.firstOrNull()) {
          INCLUDE_PREFIX -> PackageRule(included = true, identifier = encoded.substring(1))
          EXCLUDE_PREFIX -> PackageRule(included = false, identifier = encoded.substring(1))
          else -> throw InvalidInputException("Malformed package rule \"$encoded\".")
        }
  }
}

/**
 * Decides whether a package is processed, following the rules of the `nullmarked { packages { ... } }` block: the last
 * rule matching a package wins, and a package matched by no rule at all is processed.
 *
 * @param encodedRules rules in declaration order, as encoded by [PackageRule.encode]
 * @throws InvalidInputException if a rule is malformed or carries an invalid package identifier
 */
internal class PackageFilter(encodedRules: List<String>) {

  private val rules: List<Pair<PackagePattern, Boolean>> =
      encodedRules.map { PackageRule.decode(it) }.map { PackagePattern.of(it.identifier) to !it.included }

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
