package com.mefabc24.strata.tools.atlas

import java.nio.file.Path
import kotlin.system.exitProcess

fun main(arguments: Array<String>) {
    if (arguments.size != 3) {
        System.err.println(
            "Usage: TextureAtlasPacker <sourceDir> <outputDir> <atlasName>"
        )
        exitProcess(2)
    }

    try {
        val result = TextureAtlasPacker.pack(
            AtlasPackingConfig(
                sourceDirectory = Path.of(arguments[0]),
                outputDirectory = Path.of(arguments[1]),
                atlasName = arguments[2]
            )
        )
        println("Generated ${result.metadata}")
        result.pages.forEach { println("Generated $it") }
    } catch (failure: Exception) {
        System.err.println("Texture atlas packing failed: ${failure.message}")
        exitProcess(1)
    }
}
