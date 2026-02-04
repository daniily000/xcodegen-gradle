@file:OptIn(ExperimentalStdlibApi::class)

package com.daniily.gradle.xcodegen

import groovy.json.JsonSlurper
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.reflect.TypeOf
import org.gradle.api.tasks.Exec
import java.security.MessageDigest
import kotlin.reflect.javaType
import kotlin.reflect.typeOf

class XcodegenPlugin : Plugin<Project> {

    override fun apply(target: Project): Unit = with(target) {
        val extension = objects.newInstance(XcodegenExtension::class.java, name)
        val type = TypeOf.typeOf<XcodegenExtension>(typeOf<XcodegenExtension>().javaType)

        extensions.add(type, "xcodegen", extension)

        afterEvaluate {

            val version = extension.version
            val xcodegenDir = rootProject.layout.buildDirectory.dir("xcodegen/$version").get()
            xcodegenDir.asFile.apply { if (!exists()) mkdirs() }

            val savedDigestFile = xcodegenDir.file("digest").asFile

            val actualDigest = savedDigestFile.takeIf { it.exists() }
                ?.inputStream()?.reader()?.readText()
                .let { savedDigest ->
                    if (savedDigest == null) {
                        val digest =
                            uri("https://api.github.com/repos/yonaskolb/XcodeGen/releases/tags/$version")
                                .toURL()
                                .openStream().use { it.readAllBytes() }
                                .let { JsonSlurper().parse(it) as? Map<String, Any> }
                                ?.let { it.getOrDefault("assets", null) as? List<Map<String, Any>> }
                                ?.find { it["name"].toString() == "xcodegen.zip" }
                                ?.get("digest")
                                ?.toString()

                        if (digest != null) {
                            savedDigestFile.writeText(digest)
                        }
                        digest
                    } else savedDigest
                }

            val xcodegenZip = xcodegenDir.file("xcodegen.zip").asFile
            val sha256 = MessageDigest.getInstance("sha256")
            val xcodegenZipDigest = xcodegenZip
                .takeIf { it.exists() }
                ?.readBytes()
                ?.let(sha256::digest)
                ?.let { "sha256:${it.toHexString()}" }

            if (xcodegenZipDigest != actualDigest) {
                if (xcodegenZip.exists()) {
                    xcodegenZip.delete()
                }

                uri("https://github.com/yonaskolb/XcodeGen/releases/download/$version/xcodegen.zip")
                    .toURL()
                    .openStream()
                    .use { input ->
                        xcodegenZip.outputStream().use { it.write(input.readBytes()) }
                    }
            }

            val dist = xcodegenDir.dir("dist").asFile
            if (!dist.exists()) {
                dist.mkdirs()
                copy { spec ->
                    spec.from(zipTree(xcodegenZip))
                    spec.into(dist)
                }
            }

            tasks.register("runXcodegen", Exec::class.java) { exec ->
                val config = extension.config
                val projectYmlPath = if (config != null) {
                    val projectXcodegenDir = layout.buildDirectory.dir("xcodegen").get().apply {
                        if (!asFile.exists()) asFile.mkdirs()
                    }
                    val projectYml = projectXcodegenDir.file("project.yml")
                    projectYml.asFile.apply {
                        if (exists()) {
                            delete()
                            createNewFile()
                        }
                        writeText(config)
                    }.absolutePath
                } else {
                    layout.projectDirectory.file("project.yml").asFile.absolutePath
                }
                exec.workingDir = project.layout.projectDirectory.asFile
                exec.commandLine = listOf(
                    xcodegenDir.file("dist/xcodegen/bin/xcodegen").asFile.absolutePath,
                    "-s",
                    projectYmlPath,
                    "-r",
                    layout.projectDirectory.asFile.absolutePath,
                    "-p",
                    layout.projectDirectory.asFile.absolutePath,
                )
            }
        }
    }
}
