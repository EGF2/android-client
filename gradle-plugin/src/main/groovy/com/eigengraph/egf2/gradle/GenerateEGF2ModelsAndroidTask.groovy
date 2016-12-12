package com.eigengraph.egf2.gradle

import com.eigengraph.egf2.generator.EGF2Generator
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.incremental.IncrementalTaskInputs

class GenerateEGF2ModelsAndroidTask extends DefaultTask {
    @OutputDirectory
    File outputDir

    String id

    @TaskAction
    def generate(IncrementalTaskInputs inputs) {

        if (!inputs.incremental && outputDir.exists()) {
            outputDir.deleteDir()
        }

        if (!outputDir.exists()) {
            outputDir.mkdirs()
        }

        def configuration = project.EGF2
        configuration.targetDirectory = outputDir
        configuration.targetPackage = id

        EGF2Generator.INSTANCE.generate(configuration)
    }
}
