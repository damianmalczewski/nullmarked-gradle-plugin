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

package io.github.malczuuu.gradle.nullmarked

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.SourceSet

/**
 * Applies JSpecify conventions to a Java project:
 *
 * - generates a `@NullMarked` `package-info.java` for every non-empty package of the `main` source set that does not
 *   declare one (configurable via the `nullmarked` extension),
 * - adds `org.jspecify:jspecify` as a `compileOnly` dependency unless the build script declares a JSpecify dependency
 *   itself.
 */
class NullMarkedPlugin : Plugin<Project> {

  override fun apply(project: Project) {
    val extension = project.extensions.create("nullmarked", NullMarkedExtension::class.java)
    extension.generatePackageInfo.convention(true)
    extension.jspecifyVersion.convention(DEFAULT_JSPECIFY_VERSION)

    project.plugins.withType(JavaPlugin::class.java) {
      configurePackageInfoGeneration(project, extension)
      configureDefaultDependency(project, extension)
    }
  }

  private fun configurePackageInfoGeneration(project: Project, extension: NullMarkedExtension) {
    val mainSourceSet =
        project.extensions
            .getByType(JavaPluginExtension::class.java)
            .sourceSets
            .getByName(SourceSet.MAIN_SOURCE_SET_NAME)

    val outputDir = project.layout.buildDirectory.dir("generated/sources/nullmarked/java/${mainSourceSet.name}")

    val generateTask =
        project.tasks.register(TASK_NAME, GeneratePackageInfoTask::class.java) {
          group = "generation"
          description = "Generates @NullMarked package-info.java files for packages missing them."
          outputDirectory.set(outputDir)
          // Scan only the hand-written source directories, not our own output.
          val outputDirFile = outputDir.get().asFile
          sourceDirectories.from(project.provider { mainSourceSet.java.srcDirs - outputDirFile })
          onlyIf { extension.generatePackageInfo.get() }
        }

    mainSourceSet.java.srcDir(generateTask.flatMap(GeneratePackageInfoTask::outputDirectory))
  }

  private fun configureDefaultDependency(project: Project, extension: NullMarkedExtension) {
    project.configurations.getByName(JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME).withDependencies {
      if (jspecifyDeclaredIn(this)) {
        return@withDependencies
      }
      val declaredElsewhere =
          listOf(
                  JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME,
                  JavaPlugin.API_CONFIGURATION_NAME,
                  JavaPlugin.COMPILE_ONLY_API_CONFIGURATION_NAME,
              )
              .any { name ->
                val configuration = project.configurations.findByName(name)
                configuration != null && jspecifyDeclaredIn(configuration.dependencies)
              }
      if (!declaredElsewhere) {
        add(project.dependencies.create("$JSPECIFY_GROUP:jspecify:${extension.jspecifyVersion.get()}"))
      }
    }
  }

  private fun jspecifyDeclaredIn(dependencies: Iterable<Dependency>): Boolean = dependencies.any {
    it.group == JSPECIFY_GROUP
  }

  companion object {
    const val DEFAULT_JSPECIFY_VERSION = "1.0.0"

    private const val JSPECIFY_GROUP = "org.jspecify"
    private const val TASK_NAME = "generatePackageInfo"
  }
}
