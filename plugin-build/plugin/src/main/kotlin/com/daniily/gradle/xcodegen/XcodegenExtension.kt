package com.daniily.gradle.xcodegen

import javax.inject.Inject

abstract class XcodegenExtension
@Inject constructor(
    val name: String,
) {
    var version: String = "2.44.1"
    // language="yaml"
    var config: String? = null
}
