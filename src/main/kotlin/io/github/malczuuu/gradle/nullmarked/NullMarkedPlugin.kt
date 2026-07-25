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

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import org.gradle.language.base.plugins.LifecycleBasePlugin

/**
 * Applies JSpecify's `NullMarked` convention to a Java project:
 *
 * - generates a `@NullMarked` `package-info.java` for every non-empty package of each configured source set (`main`
 *   always, others via the `nullmarked { sourceSet("...") { ... } }` DSL) that does not declare one,
 * - verifies before compilation that every such package ends up with a `package-info.java`, hand-written or generated,
 * - adds `org.jspecify:jspecify` as a `compileOnly`-equivalent dependency of each configured source set unless the
 *   build script declares a JSpecify dependency itself.
 *
 * With `verifyOnly`, generation is skipped and verification fails on packages without a hand-written
 * `package-info.java`; with `enabled = false`, both generation and verification are off.
 */
open class NullMarkedPlugin : Plugin<Project> {

  /**
   * Apply this plugin to the given target object.
   *
   * @param target The target object
   */
  override fun apply(target: Project) {
    val extension = target.extensions.create<NullMarkedExtension>("nullmarked")
    extension.enabled.convention(true)
    extension.headerEnabled.convention(true)
    extension.verifyOnly.convention(false)
    extension.excludedPackages.convention(emptyList())
    extension.jspecifyVersion.convention(JSPECIFY_VERSION)
    extension.sourceSets.maybeCreate(SourceSet.MAIN_SOURCE_SET_NAME)

    target.plugins.withType<JavaPlugin> {
      val javaSourceSets = target.extensions.getByType<JavaPluginExtension>().sourceSets

      extension.sourceSets.all {
        val spec = this
        javaSourceSets
            .matching { it.name == spec.name }
            .all {
              val generateTask = configurePackageInfoGeneration(target, this, spec)
              configurePackageInfoVerification(target, this, spec, generateTask)
              configureDefaultDependency(target, this, extension.jspecifyVersion)
            }
      }
    }
  }

  private fun configurePackageInfoGeneration(
      project: Project,
      javaSourceSet: SourceSet,
      spec: NullMarkedSourceSetSpec,
  ): TaskProvider<GeneratePackageInfoTask> {
    val outputDir = project.layout.buildDirectory.dir("generated/sources/nullmarked/java/${javaSourceSet.name}")
    val outputDirFile = outputDir.get().asFile

    val generateTask =
        project.tasks.register<GeneratePackageInfoTask>(javaSourceSet.getTaskName("generate", "packageInfo")) {
          group = "generation"
          description = "Generates @NullMarked package-info.java files for packages missing them."

          // Scan only the hand-written source directories, not our own output.
          val inputDirFiles = project.provider { javaSourceSet.java.srcDirs - outputDirFile }

          sourceDirectories.from(inputDirFiles)
          // verifyOnly opts out of generated code; the source set is then expected to declare
          // every package-info.java by hand and the verification task enforces it.
          generationEnabled.set(spec.enabled.zip(spec.verifyOnly) { enabled, verifyOnly -> enabled && !verifyOnly })
          headerEnabled.set(spec.headerEnabled)
          excludedPackages.set(spec.excludedPackages)

          outputDirectory.set(outputDir)
        }

    javaSourceSet.java.srcDir(generateTask.flatMap(GeneratePackageInfoTask::outputDirectory))

    // Generated package-info.java only carries @NullMarked, not documentation; keep it out of Javadoc output.
    project.tasks.withType<Javadoc>().configureEach {
      exclude { it.file.toPath().startsWith(outputDirFile.toPath()) }
    }

    return generateTask
  }

  /**
   * Registers the verification task for [javaSourceSet] and makes its compilation depend on it. The task scans the
   * whole source set, generated output included, so it verifies the end state in both the generating and the
   * `verifyOnly` setup.
   */
  private fun configurePackageInfoVerification(
      project: Project,
      javaSourceSet: SourceSet,
      spec: NullMarkedSourceSetSpec,
      generateTask: TaskProvider<GeneratePackageInfoTask>,
  ) {
    val taskName = javaSourceSet.getTaskName("verify", "packageInfo")

    val verifyTask =
        project.tasks.register<VerifyPackageInfoTask>(taskName) {
          group = LifecycleBasePlugin.VERIFICATION_GROUP
          description = "Verifies that every package declares a package-info.java."

          // Verification has to see what generation produced, not only the hand-written sources.
          dependsOn(generateTask)
          sourceDirectories.from(project.provider { javaSourceSet.java.srcDirs })
          verificationEnabled.set(spec.enabled)
          verifyOnly.set(spec.verifyOnly)
          excludedPackages.set(spec.excludedPackages)

          markerFile.set(project.layout.buildDirectory.file("tmp/nullmarked/$taskName/verification.txt"))
        }

    project.tasks.named(javaSourceSet.compileJavaTaskName).configure { dependsOn(verifyTask) }
  }

  private fun configureDefaultDependency(
      project: Project,
      javaSourceSet: SourceSet,
      jspecifyVersion: Provider<String>,
  ) {
    project.configurations.getByName(javaSourceSet.compileOnlyConfigurationName).withDependencies {
      val coordinate = parseJspecifyCoordinate(jspecifyVersion.get())
      if (jspecifyDeclaredIn(this, coordinate)) {
        return@withDependencies
      }
      val declaredElsewhere =
          candidateConfigurationNames(javaSourceSet).any { name ->
            val configuration = project.configurations.findByName(name)
            configuration != null && jspecifyDeclaredIn(configuration.dependencies, coordinate)
          }
      if (!declaredElsewhere) {
        add(project.dependencies.create("${coordinate.group}:${coordinate.name}:${coordinate.version}"))
      }
    }
  }

  /**
   * Configuration names checked for an existing JSpecify declaration before adding one. `main` also checks `api` and
   * `compileOnlyApi`, which the `java-library` plugin only creates for `main`; other source sets have no such
   * equivalent by convention, so only their own `compileOnly`/`implementation` configurations are checked.
   */
  private fun candidateConfigurationNames(sourceSet: SourceSet): List<String> =
      if (sourceSet.name == SourceSet.MAIN_SOURCE_SET_NAME) {
        listOf(
            JavaPlugin.API_CONFIGURATION_NAME,
            JavaPlugin.COMPILE_ONLY_API_CONFIGURATION_NAME,
            sourceSet.compileOnlyConfigurationName,
            sourceSet.implementationConfigurationName,
        )
      } else {
        listOf(sourceSet.compileOnlyConfigurationName, sourceSet.implementationConfigurationName)
      }

  private fun jspecifyDeclaredIn(dependencies: Iterable<Dependency>, coordinate: JSpecifyCoordinate): Boolean =
      dependencies.any {
        it.group == coordinate.group && it.name == coordinate.name
      }
}
