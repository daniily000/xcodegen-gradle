# XcodeGen for Gradle

Integrate [XcodeGen](https://github.com/yonaskolb/XcodeGen) tool into your Gradle project.

## Stability note

This plugin is under construction. Though it already does the job, its API might be deprecated fast.

## Setup

1. Add your iOS project directory to `settings.gradle` as a module,
2. Add `id("com.daniily.gradle.xcodegen")` plugin to your iOS project module,
3. Set up your iOS project with `xcodegen` extension:

```kotlin
plugins {
    id("com.daniily.gradle.xcodegen")
}

xcodegen {
    // Set XcodeGen tool version
    version = "2.44.1"
    
    // Set project.yml contents
    config = """
        name: iosApp
        options:
          bundleIdPrefix: com.daniily.gradle.xcodegen.example
        targets:
          iosApp:
            type: application
            platform: iOS
            deploymentTarget: "14.0"
            sources: [iosApp]    
    """
    // or skip to use module-level project.yml file
}
```

Sync the project and run `runXcodegen` Gradle task.

## Problem solving

In a Kotlin Multiplatform project, it is hard to pass build data to iOS application. Moreover –
changing it, especially in a team, leads to conflicts in `project.pbxproj` file.

[XcodeGen](https://github.com/yonaskolb/XcodeGen) allows setting up a iOS project easily,
with a strict configuration written inside `project.yml`. But what if you wanted to provide a bundle
ID, version or some other configuration settings?

Use this plugin to integrate XcodeGen tool and be able to create and manage your iOS project from
Gradle.  

## How it works

### XcodeGen version

By default, the applied plugin downloads the exact XcodeGen version, which is latest at the time of
the plugin release. Use `version` property to use the exact XcodeGen version:

```kotlin
xcodegen {
    version = "2.44.1"
}
```

The requested version will be downloaded into the `build` folder of your _root_ project.

### project.yml configuration

The plugin allows setting the contents of `project.yml` file:

```kotlin
xcodegen {
    config = """
        name: iosApp
        targets:
          iosApp:
            type: application
            platform: iOS
            deploymentTarget: "14.0"
            sources: [iosApp]    
            settings:
              PRODUCT_BUNDLE_IDENTIFIER: $bundleId
              MARKETING_VERSION: $versionName
              CURRENT_PROJECT_VERSION: $versionCode
    """
}
```

It allows providing the required data from your Gradle build system – you can provide bundle ID,
version or even set the complex conditions:

```kotlin
xcodegen {
    config = """
        name: iosApp
        targets:
          iosApp:
            type: application
            platform: iOS
            deploymentTarget: "14.0"
            sources: [iosApp]    
            settings:
              PRODUCT_BUNDLE_IDENTIFIER: $bundleId
              MARKETING_VERSION: $versionName
              CURRENT_PROJECT_VERSION: $versionCode
        ${if (isDemo) """
          demoApp:
            type: application
            platform: iOS
            deploymentTarget: "14.0"
            sources: [ demoApp ]
        """ else "" }
    """
}
```

`runXcodegen` task will generate `project.yml` in the build directory of a module and use it for
generating the Xcode project. If `config` is not set, the `project.yml` file will be used in the
module directory.
