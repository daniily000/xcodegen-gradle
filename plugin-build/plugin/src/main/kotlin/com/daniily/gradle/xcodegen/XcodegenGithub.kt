@file:OptIn(ExperimentalStdlibApi::class)

package com.daniily.gradle.xcodegen

import groovy.json.JsonSlurper
import org.gradle.api.GradleException
import java.io.File
import java.net.HttpURLConnection
import java.net.URI
import java.security.MessageDigest

internal fun githubToken(): String? =
    sequenceOf("GITHUB_TOKEN", "GH_TOKEN")
        .mapNotNull(System::getenv)
        .firstOrNull { it.isNotBlank() }

internal fun fetchXcodegenZipDigest(version: String, token: String?): String {
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

internal fun download(url: String, destination: File) {
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

internal fun sha256(file: File): String {
    val digest = MessageDigest.getInstance("SHA-256")
    file.inputStream().use { input ->
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            digest.update(buffer, 0, read)
        }
    }
    return "sha256:${digest.digest().toHexString()}"
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
