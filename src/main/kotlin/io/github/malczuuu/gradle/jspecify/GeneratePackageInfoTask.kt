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
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.IgnoreEmptyDirectories
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.TaskAction

/**
 * Scans the hand-written Java source directories of a source set and generates a `package-info.java` annotated with
 * `org.jspecify.annotations.NullMarked` for every package that contains at least one Java file but no
 * `package-info.java` of its own.
 */
@CacheableTask
abstract class GeneratePackageInfoTask : DefaultTask() {

  /** Hand-written Java source directories to scan (the generated output directory is excluded). */
  @get:InputFiles
  @get:SkipWhenEmpty
  @get:IgnoreEmptyDirectories
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val sourceDirectories: ConfigurableFileCollection

  /** Directory the generated `package-info.java` files are written to. */
  @get:OutputDirectory abstract val outputDirectory: DirectoryProperty

  @TaskAction
  fun generate() {
    val outputDir = outputDirectory.get().asFile
    outputDir.deleteRecursively()
    outputDir.mkdirs()

    val packagesToGenerate = sortedSetOf<String>()
    val packagesWithPackageInfo = sortedSetOf<String>()

    sourceDirectories.files.filter(File::isDirectory).forEach { sourceDir ->
      sourceDir
          .walkTopDown()
          .filter { it.isFile && it.extension == "java" }
          .forEach { file ->
            val packageName = packageNameOf(sourceDir, file)
            if (file.name == "package-info.java") {
              packagesWithPackageInfo += packageName
            } else if (packageName.isNotEmpty()) {
              packagesToGenerate += packageName
            }
          }
    }

    packagesToGenerate.removeAll(packagesWithPackageInfo)

    val generationDate =
        OffsetDateTime.now().truncatedTo(ChronoUnit.SECONDS).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)

    packagesToGenerate.forEach { packageName ->
      val packageDir = outputDir.resolve(packageName.replace('.', '/'))
      packageDir.mkdirs()
      packageDir
          .resolve("package-info.java")
          .writeText(
              """
                @Generated(
                    value = "io.github.malczuuu.gradle-jspecify",
                    date = "$generationDate",
                    comments = "Generated to apply @NullMarked to the whole package."
                )
                @NullMarked
                package $packageName;

                import javax.annotation.processing.Generated;
                import org.jspecify.annotations.NullMarked;
                """
                  .trimIndent() + "\n"
          )
    }
  }

  private fun packageNameOf(sourceRoot: File, javaFile: File): String {
    val parent = javaFile.parentFile
    if (parent == null || parent == sourceRoot) {
      return ""
    }
    return parent.relativeTo(sourceRoot).path.replace(File.separatorChar, '.')
  }
}
