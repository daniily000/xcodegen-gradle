plugins {
    kotlin("jvm")
    `java-gradle-plugin`
}

group = "com.daniily.gradle.xcodegen"
version = "0.0.1"

gradlePlugin {
    plugins {
        create("com.daniily.gradle.xcodegen") {
            id = "com.daniily.gradle.xcodegen"
            implementationClass = "com.daniily.gradle.xcodegen.XcodegenPlugin"
            version = "0.0.1"
        }
    }
}
