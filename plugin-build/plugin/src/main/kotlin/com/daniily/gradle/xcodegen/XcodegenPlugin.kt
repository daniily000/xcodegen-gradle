@file:OptIn(ExperimentalStdlibApi::class)

package com.daniily.gradle.xcodegen

import groovy.json.JsonSlurper
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.reflect.TypeOf
import org.gradle.api.tasks.Exec
import java.io.File
import java.net.HttpURLConnection
import java.net.URI
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
            val cachedDigestFile = gradle.gradleUserHomeDir
                .resolve("caches/xcodegen-gradle/$version/digest")
            val token = githubToken()

            val actualDigest = sequenceOf(savedDigestFile, cachedDigestFile)
                .mapNotNull { file ->
                    file.takeIf { it.isFile }?.readText()?.trim()?.takeIf { it.isNotEmpty() }
                }
                .firstOrNull()
                ?: fetchXcodegenZipDigest(version, token).also { digest ->
                    cachedDigestFile.parentFile.mkdirs()
                    cachedDigestFile.writeText(digest)
                    savedDigestFile.writeText(digest)
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

                download(
                    "https://github.com/yonaskolb/XcodeGen/releases/download/$version/xcodegen.zip",
                    xcodegenZip,
                )
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

private fun githubToken(): String? =
    sequenceOf("GITHUB_TOKEN", "GH_TOKEN")
        .mapNotNull(System::getenv)
        .firstOrNull { it.isNotBlank() }

private fun fetchXcodegenZipDigest(version: String, token: String?): String {
    val connection = openGitHub(
        "https://api.github.com/repos/yonaskolb/XcodeGen/releases/tags/$version",
        token,
    )
    try {
        val status = connection.responseCode
        val bytes = (if (status in 200..299) connection.inputStream else connection.errorStream)
            ?.use { it.readBytes() }
            ?: ByteArray(0)
        if (status !in 200..299) {
            throw GradleException(githubFailure(version, status, bytes, token))
        }
        val digest = (JsonSlurper().parse(bytes) as? Map<*, *>)
            ?.get("assets").let { it as? List<*> }
            ?.asSequence()
            ?.mapNotNull { it as? Map<*, *> }
            ?.find { it["name"].toString() == "xcodegen.zip" }
            ?.get("digest")
            ?.toString()
        if (digest.isNullOrBlank()) {
            throw GradleException("XcodeGen $version release does not publish a digest for xcodegen.zip")
        }
        return digest
    } finally {
        connection.disconnect()
    }
}

private fun download(url: String, destination: File) {
    val connection = openGitHub(url, token = null)
    try {
        val status = connection.responseCode
        if (status !in 200..299) {
            val body = connection.errorStream?.use { it.readBytes() }?.toString(Charsets.UTF_8).orEmpty()
            throw GradleException("Failed to download $url ($status). ${body.take(500)}")
        }
        connection.inputStream.use { input ->
            destination.outputStream().use { output -> input.copyTo(output) }
        }
    } finally {
        connection.disconnect()
    }
}

private fun openGitHub(url: String, token: String?): HttpURLConnection {
    val connection = (URI(url).toURL().openConnection() as HttpURLConnection).apply {
        setRequestProperty("User-Agent", "xcodegen-gradle")
        connectTimeout = 15_000
        readTimeout = 60_000
        instanceFollowRedirects = true
        if (url.startsWith("https://api.github.com/")) {
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
            if (token != null) {
                setRequestProperty("Authorization", "Bearer $token")
            }
        }
    }
    return connection
}

private fun githubFailure(version: String, status: Int, body: ByteArray, token: String?): String {
    val message = runCatching {
        (JsonSlurper().parse(body) as? Map<*, *>)?.get("message")?.toString()
    }.getOrNull()?.takeIf { it.isNotBlank() }
        ?: body.toString(Charsets.UTF_8).trim().take(500)
    val hint = if (token == null && (status == 403 || status == 429)) {
        " Set GITHUB_TOKEN or GH_TOKEN to authenticate the request (anonymous calls are limited to 60 per hour per IP)."
    } else {
        ""
    }
    return "Failed to resolve XcodeGen $version from GitHub ($status). $message$hint"
}
