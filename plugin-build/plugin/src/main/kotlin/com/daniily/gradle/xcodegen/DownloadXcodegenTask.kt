package com.daniily.gradle.xcodegen

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File
import javax.inject.Inject

abstract class DownloadXcodegenTask : DefaultTask() {

    @get:Input
    abstract val version: Property<String>

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @get:Internal
    abstract val gradleUserHome: DirectoryProperty

    @get:Inject
    abstract val archiveOperations: ArchiveOperations

    @get:Inject
    abstract val fileSystemOperations: FileSystemOperations

    init {
        group = "xcodegen"
        description = "Downloads the XcodeGen release used by runXcodegen."
    }

    @TaskAction
    fun provision() {
        val version = version.get()
        val cache = File(gradleUserHome.get().asFile, "caches/xcodegen-gradle/$version")
        cache.mkdirs()

        val digestFile = File(cache, "digest")
        val digest = digestFile.takeIf { it.isFile }?.readText()?.trim()?.takeIf { it.isNotEmpty() }
            ?: fetchXcodegenZipDigest(version, githubToken()).also(digestFile::writeText)

        val zip = File(cache, "xcodegen.zip")
        if (!zip.isFile || sha256(zip) != digest) {
            if (zip.exists()) zip.delete()
            download(
                "https://github.com/yonaskolb/XcodeGen/releases/download/$version/xcodegen.zip",
                zip,
            )
            val actual = sha256(zip)
            if (actual != digest) {
                zip.delete()
                throw GradleException("Checksum mismatch for XcodeGen $version: expected $digest, got $actual")
            }
        }

        val dist = outputDirectory.get().asFile.resolve("dist")
        if (dist.exists()) dist.deleteRecursively()
        dist.mkdirs()
        fileSystemOperations.copy { spec ->
            spec.from(archiveOperations.zipTree(zip))
            spec.into(dist)
        }
    }
}
