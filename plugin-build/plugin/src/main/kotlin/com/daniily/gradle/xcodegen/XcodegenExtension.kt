package com.daniily.gradle.xcodegen

import org.gradle.api.provider.Property
import javax.inject.Inject

abstract class XcodegenExtension
@Inject constructor(
    val name: String,
) {
    abstract val version: Property<String>

    // language="yaml"
    abstract val config: Property<String>
}
