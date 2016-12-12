package com.eigengraph.egf2.gradle

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project

class EGF2GenerationPlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        project.extensions.create('EGF2', EGF2Extension)

        if (project.plugins.hasPlugin('com.android.application') || project.plugins.hasPlugin('com.android.library')) {
            def config = project.EGF2

            def variants = null
            if (project.android.hasProperty('applicationVariants')) {
                variants = project.android.applicationVariants
            } else if (project.android.hasProperty('libraryVariants')) {
                variants = project.android.libraryVariants
            } else {
                throw new IllegalStateException('Android project must have applicationVariants or libraryVariants!')
            }

            variants.all { variant ->
                GenerateEGF2ModelsAndroidTask task = (GenerateEGF2ModelsAndroidTask) project.task(type: GenerateEGF2ModelsAndroidTask, "generateEGF2ModelsFor${variant.name.capitalize()}") {
                    id = [variant.mergedFlavor.applicationId, variant.buildType.applicationIdSuffix].findAll().join()
                    outputDir = project.file("$project.buildDir/generated/source/egf2/$variant.flavorName/$variant.buildType.name/")
                }

                variant.registerJavaGeneratingTask(task, (File) task.outputDir)
            }
        } else {
            throw new GradleException('EGF2GenerationPlugin: Java or Android plugin required')
        }
    }
}
