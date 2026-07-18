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

import java.io.File
import org.gradle.testkit.runner.GradleRunner

/**
 * Fixture around a temporary Gradle project directory, shared by functional and integration tests. Provides file
 * helpers, the standard sample project (two packages, one with a hand-written `package-info.java`) and a preconfigured
 * [GradleRunner].
 */
class TestProject(val dir: File) {

  fun file(relativePath: String): File = File(dir, relativePath)

  fun write(relativePath: String, content: String): File =
      file(relativePath).apply {
        parentFile.mkdirs()
        writeText(content.trimIndent() + "\n")
      }

  fun writeSource(relativePath: String, content: String = "class Placeholder {}"): File =
      write("src/main/java/$relativePath", content)

  fun appendToBuildScript(content: String) {
    file("build.gradle.kts").appendText("\n" + content.trimIndent() + "\n")
  }

  /** Writes `settings.gradle.kts` and a `build.gradle.kts` applying `java` and the plugin under test. */
  fun writeStandardBuild(rootProjectName: String = "under-test") {
    write("settings.gradle.kts", "rootProject.name = \"$rootProjectName\"")
    write(
        "build.gradle.kts",
        """
        plugins {
            java
            id("io.github.malczuuu.gradle-jspecify")
        }

        repositories {
            mavenCentral()
        }
        """,
    )
    // Keep TestKit daemons small and short-lived; the compatibility matrix spawns one daemon per
    // Gradle version and accumulating full-size idle daemons destabilizes the test run.
    write(
        "gradle.properties",
        """
        org.gradle.jvmargs=-Xmx512m
        org.gradle.daemon.idletimeout=10000
        """,
    )
  }

  /** Writes `com.acme.Foo` (nullable usage, no `package-info.java`) and `com.acme.manual` with a hand-written one. */
  fun writeSampleSources() {
    writeSource(
        "com/acme/Foo.java",
        """
        package com.acme;

        import org.jspecify.annotations.Nullable;

        public class Foo {
            public String orDefault(@Nullable String value) {
                return value == null ? "default" : value;
            }
        }
        """,
    )
    writeSource(
        "com/acme/manual/Bar.java",
        """
        package com.acme.manual;

        public class Bar {}
        """,
    )
    writeSource(
        "com/acme/manual/package-info.java",
        """
        @NullMarked
        package com.acme.manual;

        import org.jspecify.annotations.NullMarked;
        """,
    )
  }

  fun runner(vararg arguments: String): GradleRunner =
      GradleRunner.create().withProjectDir(dir).withPluginClasspath().withArguments(*arguments)

  fun generatedPackageInfo(packageName: String): File =
      packageInfoUnder(file("build/generated/sources/jspecify/java/main"), packageName)

  fun compiledClass(binaryName: String): File = file("build/classes/java/main/${binaryName.replace('.', '/')}.class")

  companion object {
    /** `package-info.java` location for [packageName] under an arbitrary generation output [root]. */
    fun packageInfoUnder(root: File, packageName: String): File =
        File(root, packageName.replace('.', '/') + "/package-info.java")
  }
}
