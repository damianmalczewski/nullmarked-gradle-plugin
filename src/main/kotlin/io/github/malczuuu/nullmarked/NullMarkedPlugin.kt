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

import io.github.malczuuu.nullmarked.dsl.NullMarkedExtension
import io.github.malczuuu.nullmarked.dsl.NullMarkedSourceSetSpec
import io.github.malczuuu.nullmarked.internal.JSPECIFY_VERSION
import io.github.malczuuu.nullmarked.internal.JSpecifyCoordinate
import io.github.malczuuu.nullmarked.internal.MINIMUM_GRADLE_VERSION
import io.github.malczuuu.nullmarked.internal.parseJSpecifyCoordinate
import io.github.malczuuu.nullmarked.tasks.GeneratePackageInfo
import io.github.malczuuu.nullmarked.tasks.VerifyPackageInfo
import org.gradle.api.GradleException
import org.gradle.api.InvalidUserDataException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.gradle.util.GradleVersion

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
 * `package-info.java`; with `enabled = false`, the plugin is inert for that source set - no generation, no
 * verification, and no dependency added.
 *
 * Requires Gradle [MINIMUM_GRADLE_VERSION] or later. Applying it to a project without the `java` plugin does nothing.
 */
open class NullMarkedPlugin : Plugin<Project> {

  /**
   * Apply this plugin to the given target object.
   *
   * @param target The target object
   */
  override fun apply(target: Project) {
    checkGradleVersion()

    val extension = target.extensions.create<NullMarkedExtension>("nullmarked")
    extension.enabled.convention(true)
    extension.headerEnabled.convention(true)
    extension.verifyOnly.convention(false)
    extension.jspecifyVersion.convention(JSPECIFY_VERSION)
    extension.sourceSets.maybeCreate(SourceSet.MAIN_SOURCE_SET_NAME)

    target.plugins.withType<JavaPlugin> {
      val javaSourceSets = target.extensions.getByType<JavaPluginExtension>().sourceSets

      extension.sourceSets.all {
        val spec = this
        javaSourceSets
            .matching { it.name == spec.name }
            .all {
              val packageInfoGenerationTask = configurePackageInfoGenerationTask(target, this, spec)
              configurePackageInfoVerificationTask(target, this, spec, packageInfoGenerationTask)
              configureDefaultJSpecifyDependency(target, this, spec, extension.jspecifyVersion)
            }
      }
    }

    // Without the java plugin the blocks above never run and the project has no source sets to check against, so
    // applying nullmarked to a java-less project stays a silent no-op and blanket `subprojects { apply(...) }` keeps
    // working.
    target.afterEvaluate {
      if (plugins.hasPlugin(JavaPlugin::class.java)) {
        checkOptedInSourceSetsExist(extension, extensions.getByType<JavaPluginExtension>().sourceSets)
      }
    }
  }

  private fun checkGradleVersion() {
    if (GradleVersion.current() < GradleVersion.version(MINIMUM_GRADLE_VERSION)) {
      throw GradleException(
          "The io.github.malczuuu.nullmarked plugin requires Gradle $MINIMUM_GRADLE_VERSION or later, but was applied " +
              "to Gradle ${GradleVersion.current().version}."
      )
    }
  }

  /**
   * Fails on source sets opted into via `nullmarked { sourceSet("...") }` that no Java `SourceSet` matches, which would
   * otherwise register no task and silently ignore the whole configuration block.
   */
  private fun checkOptedInSourceSetsExist(extension: NullMarkedExtension, javaSourceSets: SourceSetContainer) {
    val availableNames = javaSourceSets.names
    val unknownNames = extension.sourceSets.names.filterNot { it in availableNames }
    if (unknownNames.isEmpty()) {
      return
    }
    val subject =
        if (unknownNames.size == 1) {
          "no source set named ${unknownNames.single().quoted()}"
        } else {
          "no source sets named ${unknownNames.joinToString(", ") { it.quoted() }}"
        }
    throw InvalidUserDataException("nullmarked: $subject. Available: ${availableNames.joinToString(", ")}.")
  }

  private fun String.quoted(): String = "'$this'"

  private fun configurePackageInfoGenerationTask(
      project: Project,
      javaSourceSet: SourceSet,
      spec: NullMarkedSourceSetSpec,
  ): TaskProvider<GeneratePackageInfo> {
    val outputDir = project.layout.buildDirectory.dir("generated/sources/nullmarked/java/${javaSourceSet.name}")
    val outputDirPath = outputDir.map { it.asFile.toPath() }

    val generateTask =
        project.tasks.register<GeneratePackageInfo>(javaSourceSet.getTaskName("generate", "packageInfo")) {
          group = "generation"
          description = "Generates @NullMarked package-info.java files for packages missing them."

          // Scan only the hand-written source directories, not our own output.
          val inputDirFiles = project.provider { javaSourceSet.java.srcDirs - outputDir.get().asFile }

          sourceDirectories.from(inputDirFiles)
          // verifyOnly opts out of generated code; the source set is then expected to declare every package-info.java
          // by hand and the verification task enforces it.
          generationEnabled.set(spec.enabled.zip(spec.verifyOnly) { enabled, verifyOnly -> enabled && !verifyOnly })
          headerEnabled.set(spec.headerEnabled)
          inheritPackageRules(spec.packageRules)

          outputDirectory.set(outputDir)
        }

    javaSourceSet.java.srcDir(generateTask.flatMap { it.outputDirectory })

    // Generated package-info.java only carries @NullMarked, not documentation; keep it out of Javadoc output.
    project.tasks.withType<Javadoc>().configureEach {
      exclude { it.file.toPath().startsWith(outputDirPath.get()) }
    }

    return generateTask
  }

  private fun configurePackageInfoVerificationTask(
      project: Project,
      javaSourceSet: SourceSet,
      spec: NullMarkedSourceSetSpec,
      generateTask: TaskProvider<GeneratePackageInfo>,
  ) {
    val taskName = javaSourceSet.getTaskName("verify", "packageInfo")

    val verifyTask =
        project.tasks.register<VerifyPackageInfo>(taskName) {
          group = LifecycleBasePlugin.VERIFICATION_GROUP
          description = "Verifies that every package declares a package-info.java."

          // Verification has to see what generation produced, not only the hand-written sources.
          dependsOn(generateTask)
          sourceDirectories.from(project.provider { javaSourceSet.java.srcDirs })
          verificationEnabled.set(spec.enabled)
          verifyOnly.set(spec.verifyOnly)
          inheritPackageRules(spec.packageRules)

          buildCacheMarker.set(project.layout.buildDirectory.file("tmp/nullmarked/$taskName/verification.txt"))
        }

    project.tasks.named(javaSourceSet.compileJavaTaskName).configure { dependsOn(verifyTask) }
    project.tasks.named(LifecycleBasePlugin.CHECK_TASK_NAME).configure { dependsOn(verifyTask) }
  }

  private fun configureDefaultJSpecifyDependency(
      project: Project,
      javaSourceSet: SourceSet,
      spec: NullMarkedSourceSetSpec,
      jspecifyVersion: Provider<String>,
  ) {
    val configurations = project.configurations
    val dependencies = project.dependencies

    configurations.named(javaSourceSet.compileOnlyConfigurationName).configure {
      withDependencies {
        // A disabled source set is left entirely alone, dependency graph included, which is also the only way to opt
        // out of the dependency. verifyOnly still gets it: hand-written package-info.java files need @NullMarked on
        // the compile classpath.
        if (!spec.enabled.get()) {
          return@withDependencies
        }
        val coordinate = parseJSpecifyCoordinate(jspecifyVersion.get())
        if (coordinate.isJSpecifyDeclaredIn(this)) {
          return@withDependencies
        }
        val declaredElsewhere =
            findCandidateConfigurationNames(javaSourceSet).any { name ->
              val configuration = configurations.findByName(name)
              configuration != null && coordinate.isJSpecifyDeclaredIn(configuration.dependencies)
            }
        if (!declaredElsewhere) {
          add(dependencies.create("${coordinate.group}:${coordinate.name}:${coordinate.version}"))
        }
      }
    }
  }

  private fun findCandidateConfigurationNames(sourceSet: SourceSet): List<String> =
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

  private fun JSpecifyCoordinate.isJSpecifyDeclaredIn(dependencies: Iterable<Dependency>): Boolean = dependencies.any {
    it.group == group && it.name == name
  }
}
