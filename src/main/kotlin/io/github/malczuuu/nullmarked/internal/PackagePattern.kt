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
 * Matches package names against a package identifier following ArchUnit's syntax:
 * - a plain identifier matches exactly one package, e.g. `org.acme` matches only `org.acme`,
 * - `*` matches any sequence of characters within a single package segment, e.g. `org.*.util` matches `org.acme.util`
 *   but not `org.acme.deep.util`,
 * - `..` matches any number of packages, including zero, e.g. `org.acme..` matches `org.acme` and
 *   `org.acme.util.internal`, while `..internal..` matches any package containing an `internal` segment.
 */
internal class PackagePattern private constructor(private val regex: Regex) {

  /**
   * Compiles a package identifier into a pattern, rejecting malformed ones up front.
   *
   * @param identifier package identifier in ArchUnit syntax (see class docs)
   * @throws InvalidUserDataException if [identifier] is not a valid package identifier
   */
  constructor(identifier: String) : this(compile(identifier))

  /**
   * Tests a package name against this pattern.
   *
   * @param packageName dotted package name to test, e.g. `org.acme.util`
   * @return `true` if [packageName] matches this pattern
   */
  fun matches(packageName: String): Boolean = regex.matches(packageName)

  companion object {

    /**
     * Rejects a malformed package identifier without compiling it, letting the `nullmarked { packages { ... } }` block
     * fail at the `include`/`exclude` call site instead of at task execution.
     *
     * @param identifier package identifier in ArchUnit syntax (see class docs)
     * @throws InvalidUserDataException if [identifier] is not a valid package identifier
     */
    fun validate(identifier: String) {
      if (!PACKAGE_IDENTIFIER_REGEX.matches(identifier)) {
        throw InvalidUserDataException("Invalid package identifier '$identifier'.")
      }
    }

    private fun compile(identifier: String): Regex {
      validate(identifier)
      return Regex(toRegex(identifier))
    }

    private fun toRegex(identifier: String): String {
      if (identifier == "..") {
        return """\w+(?:\.\w+)*"""
      }
      val parts = identifier.split("..")
      return parts.indices.joinToString("") { index -> partToRegex(parts, index) }
    }

    private fun partToRegex(parts: List<String>, index: Int): String {
      val part = parts[index]
      if (part.isEmpty()) {
        return if (index == 0) """(?:\w+\.)*""" else """(?:\.\w+)*""" // leading/trailing '..'
      }
      val separator =
          if (index > 0 && parts[index - 1].isNotEmpty()) {
            """\.(?:\w+\.)*""" // middle '..': the separating dot plus any packages in between
          } else {
            ""
          }
      return separator + part.split(".").joinToString("""\.""") { segment -> segment.replace("*", """\w+""") }
    }
  }
}
