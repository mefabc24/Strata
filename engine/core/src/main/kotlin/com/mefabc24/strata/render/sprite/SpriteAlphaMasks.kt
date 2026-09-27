package com.mefabc24.strata.render.sprite

/** Caches frame-matched alpha masks for any supported sprite source. */
internal class SpriteAlphaMaskCache(
    private val loadFile: (String) -> AlphaMask?,
    private val loadSheet: (
        path: String,
        frameWidth: Int,
        frameHeight: Int,
        frameCount: Int?
    ) -> List<AlphaMask?>,
    private val sheetWidthFor: (String) -> Int
) {
    private val files = mutableMapOf<String, AlphaMask?>()
    private val sheets = mutableMapOf<SpriteSheetMaskKey, List<AlphaMask?>>()

    fun masksFor(
        source: SpriteSource,
        atlasMasks: Map<SpriteSource, List<AlphaMask?>>
    ): List<AlphaMask?> {
        return when (source) {
            is SpriteSource.Static -> listOf(file(source.path))
            is SpriteSource.AnimatedFiles -> source.assetPaths.map(::file)
            is SpriteSource.SpriteSheet -> sheet(
                source.path,
                source.frameWidth,
                source.frameHeight,
                source.frameCount
            )
            is SpriteSource.SpriteSheetRow -> {
                val allMasks = sheet(
                    source.path,
                    source.frameWidth,
                    source.frameHeight,
                    null
                )
                val columns = sheetWidthFor(source.path) / source.frameWidth
                val count = source.framesPerRow ?: columns
                val start = source.row * columns
                check(start + count <= allMasks.size) {
                    "Directional sprite-sheet masks do not contain row ${source.row}."
                }
                allMasks.subList(start, start + count)
            }
            is SpriteSource.AtlasRegion,
            is SpriteSource.AtlasAnimation -> checkNotNull(atlasMasks[source]) {
                "Atlas alpha masks were not prepared for $source."
            }
        }
    }

    private fun file(path: String): AlphaMask? {
        if (path in files) return files[path]
        return loadFile(path).also { files[path] = it }
    }

    private fun sheet(
        path: String,
        frameWidth: Int,
        frameHeight: Int,
        frameCount: Int?
    ): List<AlphaMask?> {
        val key = SpriteSheetMaskKey(path, frameWidth, frameHeight, frameCount)
        return sheets.getOrPut(key) {
            loadSheet(path, frameWidth, frameHeight, frameCount)
        }
    }

    private data class SpriteSheetMaskKey(
        val path: String,
        val frameWidth: Int,
        val frameHeight: Int,
        val frameCount: Int?
    )
}
