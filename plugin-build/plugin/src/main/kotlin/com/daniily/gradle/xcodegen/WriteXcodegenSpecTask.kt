package com.daniily.gradle.xcodegen

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

abstract class WriteXcodegenSpecTask : DefaultTask() {

    @get:Input
    @get:Optional
    abstract val config: Property<String>

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    init {
        group = "xcodegen"
        description = "Writes the generated project.yml used by runXcodegen."
        onlyIf("config is set") { config.isPresent }
    }

    @TaskAction
    fun write() {
        val destination = outputFile.get().asFile
        destination.parentFile.mkdirs()
        destination.writeText(config.get())
    }
}
