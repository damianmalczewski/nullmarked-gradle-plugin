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

package io.github.malczuuu.nullmarked

import javax.inject.Inject
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.kotlin.dsl.listProperty

/**
 * Which packages the plugin processes, configured via `nullmarked { packages { ... } }` (and the same block inside a
 * `sourceSet(...)`).
 *
 * Every [include] and [exclude] call appends to an ordered list of rules, and the **last** rule matching a package
 * decides its fate. A package matched by no rule is processed, so with no rules at all every package is. A source set's
 * own rules are evaluated after the top-level ones, letting it re-admit a package the top level excluded:
 * ```
 * nullmarked {
 *     packages {
 *         exclude("..internal..")
 *     }
 *     sourceSet("test") {
 *         packages {
 *             include("com.acme.internal.fixtures")
 *         }
 *     }
 * }
 * ```
 *
 * Package identifiers follow ArchUnit's syntax, see [PackagePattern].
 *
 * @param objects factory creating this spec's [rules] list
 */
abstract class NullMarkedPackagesSpec @Inject constructor(objects: ObjectFactory) {

  /** Rules in declaration order, inherited ones first. */
  internal val rules: ListProperty<PackageRule> = objects.listProperty<PackageRule>()

  /**
   * Skips the packages matching [packages], unless a later rule includes them again.
   *
   * @param packages package identifiers (ArchUnit syntax, see [PackagePattern])
   */
  fun exclude(vararg packages: String) {
    addRules(included = false, packages = packages)
  }

  /**
   * Processes the packages matching [packages], overriding rules declared before this one. Only useful to carve
   * exceptions out of an earlier [exclude], as packages are processed by default.
   *
   * @param packages package identifiers (ArchUnit syntax, see [PackagePattern])
   */
  fun include(vararg packages: String) {
    addRules(included = true, packages = packages)
  }

  private fun addRules(included: Boolean, packages: Array<out String>) {
    rules.addAll(packages.map { PackageRule(included = included, identifier = it) })
  }
}
