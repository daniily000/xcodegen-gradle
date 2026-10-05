package com.daniily.gradle.xcodegen

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import javax.inject.Inject

abstract class RunXcodegenTask : DefaultTask() {

    @get:InputFile
    abstract val executable: RegularFileProperty

    @get:InputFile
    abstract val spec: RegularFileProperty

    @get:Internal
    abstract val projectDirectory: DirectoryProperty

    @get:Inject
    abstract val execOperations: ExecOperations

    init {
        group = "xcodegen"
        description = "Generates the Xcode project with XcodeGen."
    }

    @TaskAction
    fun run() {
        val directory = projectDirectory.get().asFile
        val projectSpec = this.spec.get().asFile
        execOperations.exec { exec ->
            exec.executable(executable.get().asFile)
            exec.workingDir(directory)
            exec.args(
                "-s", projectSpec.absolutePath,
                "-r", directory.absolutePath,
                "-p", directory.absolutePath,
            )
        }
    }
}
