import org.jetbrains.kotlin.konan.properties.loadProperties

plugins {
    id("com.daniily.gradle.xcodegen")
}

val app = loadProperties(requireNotNull(parent).file("app.properties").absolutePath)

xcodegen {
    version = "2.44.0"

    config = """
        name: iosApp
        options:
          bundleIdPrefix: $group
        targets:
          iosApp:
            type: application
            platform: iOS
            deploymentTarget: "14.0"
            sources: [ iosApp ]
            settings:
              PRODUCT_BUNDLE_IDENTIFIER: ${app["app.id"]}
              MARKETING_VERSION: ${app["app.versionName"]}
              CURRENT_PROJECT_VERSION: ${app["app.versionCode"]}
    """.trimIndent()
}
