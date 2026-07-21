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

import org.gradle.api.InvalidUserDataException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.SourceSet
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType

/**
 * Applies JSpecify's `NullMarked` convention to a Java project:
 *
 * - generates a `@NullMarked` `package-info.java` for every non-empty package of the `main` source set that does not
 *   declare one (configurable via the `nullmarked` extension),
 * - adds `org.jspecify:jspecify` as a `compileOnly` dependency unless the build script declares a JSpecify dependency
 *   itself.
 */
open class NullMarkedPlugin : Plugin<Project> {

  override fun apply(project: Project) {
    val extension = project.extensions.create<NullMarkedExtension>("nullmarked")
    extension.enabled.convention(true)
    extension.headerEnabled.convention(true)
    extension.excludedPackages.convention(emptyList())
    extension.jspecifyVersion.convention(DEFAULT_JSPECIFY_VERSION)

    project.plugins.withType<JavaPlugin> {
      configurePackageInfoGeneration(project, extension)
      configureDefaultDependency(project, extension)
    }
  }

  private fun configurePackageInfoGeneration(project: Project, extension: NullMarkedExtension) {
    val mainSourceSet =
        project.extensions.getByType<JavaPluginExtension>().sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME)

    val outputDir = project.layout.buildDirectory.dir("generated/sources/nullmarked/java/${mainSourceSet.name}")

    val generateTask =
        project.tasks.register<GeneratePackageInfoTask>(TASK_NAME) {
          group = "generation"
          description = "Generates @NullMarked package-info.java files for packages missing them."

          val outputDirFile = outputDir.get().asFile

          // Scan only the hand-written source directories, not our own output.
          val inputDirFiles = project.provider { mainSourceSet.java.srcDirs - outputDirFile }

          sourceDirectories.from(inputDirFiles)
          generationEnabled.set(extension.enabled)
          headerEnabled.set(extension.headerEnabled)
          excludedPackages.set(extension.excludedPackages)

          outputDirectory.set(outputDir)
        }

    mainSourceSet.java.srcDir(generateTask.flatMap(GeneratePackageInfoTask::outputDirectory))
  }

  private fun configureDefaultDependency(project: Project, extension: NullMarkedExtension) {
    project.configurations.getByName(JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME).withDependencies {
      val coordinate = parseJspecifyCoordinate(extension.jspecifyVersion.get())
      if (jspecifyDeclaredIn(this, coordinate)) {
        return@withDependencies
      }
      val declaredElsewhere =
          listOf(
                  JavaPlugin.API_CONFIGURATION_NAME,
                  JavaPlugin.COMPILE_ONLY_API_CONFIGURATION_NAME,
                  JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME,
                  JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME,
              )
              .any { name ->
                val configuration = project.configurations.findByName(name)
                configuration != null && jspecifyDeclaredIn(configuration.dependencies, coordinate)
              }
      if (!declaredElsewhere) {
        add(project.dependencies.create("${coordinate.group}:${coordinate.name}:${coordinate.version}"))
      }
    }
  }

  private fun jspecifyDeclaredIn(dependencies: Iterable<Dependency>, coordinate: JSpecifyCoordinate): Boolean =
      dependencies.any {
        it.group == coordinate.group && it.name == coordinate.name
      }

  /**
   * Accepts either a bare version (`"1.0.0"`, applied to the default `org.jspecify:jspecify` coordinate) or a full
   * `group:name:version` dependency notation, letting users point at a fork of JSpecify.
   */
  private fun parseJspecifyCoordinate(value: String): JSpecifyCoordinate {
    val parts = value.split(":")
    return when (parts.size) {
      1 -> JSpecifyCoordinate(JSPECIFY_GROUP, JSPECIFY_NAME, parts[0])
      3 -> JSpecifyCoordinate(parts[0], parts[1], parts[2])
      else ->
          throw InvalidUserDataException(
              "nullmarked.jspecifyVersion must be either a version (e.g. \"1.0.0\") or a full dependency notation " +
                  "(e.g. \"org.jspecify:jspecify:1.0.0\"), but was: \"$value\""
          )
    }
  }

  private data class JSpecifyCoordinate(val group: String, val name: String, val version: String)

  companion object {
    const val DEFAULT_JSPECIFY_VERSION = "1.0.0"

    private const val JSPECIFY_GROUP = "org.jspecify"
    private const val JSPECIFY_NAME = "jspecify"
    private const val TASK_NAME = "generatePackageInfo"
  }
}
