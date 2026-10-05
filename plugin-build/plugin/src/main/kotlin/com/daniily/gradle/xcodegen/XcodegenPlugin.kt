package com.daniily.gradle.xcodegen

import org.gradle.api.Plugin
import org.gradle.api.Project

class XcodegenPlugin : Plugin<Project> {

    override fun apply(target: Project): Unit = with(target) {
        val extension = extensions.create("xcodegen", XcodegenExtension::class.java, name)
        extension.version.convention("2.44.1")

        val downloadXcodegen = tasks.register("downloadXcodegen", DownloadXcodegenTask::class.java) { task ->
            task.version.set(extension.version)
            task.outputDirectory.set(layout.buildDirectory.dir(extension.version.map { "xcodegen/$it" }))
            task.gradleUserHome.set(gradle.gradleUserHomeDir)
        }
        val writeXcodegenSpec = tasks.register("writeXcodegenSpec", WriteXcodegenSpecTask::class.java) { task ->
            task.config.set(extension.config)
            task.outputFile.set(layout.buildDirectory.file("xcodegen/project.yml"))
        }
        val spec = extension.config
            .flatMap { writeXcodegenSpec.flatMap { it.outputFile } }
            .orElse(layout.projectDirectory.file("project.yml"))

        tasks.register("runXcodegen", RunXcodegenTask::class.java) { task ->
            task.executable.set(downloadXcodegen.flatMap { download ->
                download.outputDirectory.file("dist/xcodegen/bin/xcodegen")
            })
            task.spec.set(spec)
            task.projectDirectory.set(layout.projectDirectory)
        }
    }
}
