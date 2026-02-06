plugins {
    alias(libs.plugins.kotlin.jvm)
    `java-gradle-plugin`
    alias(libs.plugins.gradle.publish)
}

group = "com.daniily.gradle.xcodegen"
version = "0.0.1"

gradlePlugin {
    website = "https://github.com/daniily000/xcodegen-gradle"
    vcsUrl = "https://github.com/daniily000/xcodegen-gradle"

    plugins {
        register("com.daniily.gradle.xcodegen") {
            id = "com.daniily.gradle.xcodegen"
            displayName = "XcodeGen for Gradle"
            description = "Integrate iOS app with XcodeGen into your Gradle project"
            tags = listOf("xcodegen",  "ios", "kmp", "kotlin multiplatform",)
            implementationClass = "com.daniily.gradle.xcodegen.XcodegenPlugin"
        }
    }
}
