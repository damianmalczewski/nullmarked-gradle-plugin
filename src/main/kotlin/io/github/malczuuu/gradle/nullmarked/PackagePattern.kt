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
 * Matches package names against a package identifier following ArchUnit's syntax:
 * - a plain identifier matches exactly one package, e.g. `org.acme` matches only `org.acme`,
 * - `*` matches any sequence of characters within a single package segment, e.g. `org.*.util` matches `org.acme.util`
 *   but not `org.acme.deep.util`,
 * - `..` matches any number of packages, including zero, e.g. `org.acme..` matches `org.acme` and
 *   `org.acme.util.internal`, while `..internal..` matches any package containing an `internal` segment.
 */
internal class PackagePattern private constructor(private val regex: Regex) {

  fun matches(packageName: String): Boolean = regex.matches(packageName)

  companion object {

    private val VALID_IDENTIFIER = Regex("""(\.\.)?[\w*]+((\.|\.\.)[\w*]+)*(\.\.)?|\.\.""")

    fun of(identifier: String): PackagePattern {
      require(VALID_IDENTIFIER.matches(identifier)) { "Invalid package identifier '$identifier' [nullmarked plugin]." }
      return PackagePattern(Regex(toRegex(identifier)))
    }

    private fun toRegex(identifier: String): String {
      if (identifier == "..") {
        return """\w+(?:\.\w+)*"""
      }
      val parts = identifier.split("..")
      return buildString {
        parts.forEachIndexed { index, part ->
          when {
            part.isEmpty() && index == 0 -> append("""(?:\w+\.)*""") // leading '..': any parent packages
            part.isEmpty() -> append("""(?:\.\w+)*""") // trailing '..': any subpackages
            else -> {
              if (index > 0 && parts[index - 1].isNotEmpty()) {
                append("""\.(?:\w+\.)*""") // middle '..': the separating dot plus any packages in between
              }
              append(part.split('.').joinToString("""\.""") { segment -> segment.replace("*", """\w+""") })
            }
          }
        }
      }
    }
  }
}
