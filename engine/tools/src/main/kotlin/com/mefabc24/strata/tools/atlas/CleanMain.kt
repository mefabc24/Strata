package com.mefabc24.strata.tools.atlas

import java.nio.file.Path
import kotlin.system.exitProcess

fun main(arguments: Array<String>) {
    if (arguments.size != 2) {
        System.err.println(
            "Usage: TextureAtlasCleaner <outputDir> <atlasName>"
        )
        exitProcess(2)
    }

    try {
        TextureAtlasPacker.clean(
            outputDirectory = Path.of(arguments[0]),
            atlasName = arguments[1]
        ).forEach { println("Removed $it") }
    } catch (failure: Exception) {
        System.err.println("Texture atlas cleanup failed: ${failure.message}")
        exitProcess(1)
    }
}
