import org.jetbrains.kotlin.konan.properties.loadProperties

plugins {
    kotlin("jvm")
}

val app = loadProperties(requireNotNull(parent).file("app.properties").absolutePath)

group = app["app.id"].toString()
version = app["app.versionName"].toString()
