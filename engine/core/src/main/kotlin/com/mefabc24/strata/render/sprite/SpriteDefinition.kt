package com.mefabc24.strata.render.sprite

/**
 * Defines one static or animated sprite source during scene setup.
 *
 * File paths are resolved relative to the directory of the registry using the
 * definition. Atlas paths are scene-level paths and are used as supplied.
 */
open class SpriteDefinitionBuilder internal constructor(
    private val resolvePath: (String) -> String
) {
    private var definedSource: SpriteSource? = null

    /** Uses one image file as a static, one-frame sprite. */
    fun sprite(path: String) {
        require(path.isNotBlank()) { "Sprite path must not be blank." }
        define(SpriteSource.Static(resolvePath(path)))
    }

    /** Uses ordered image files as a looping animation. */
    fun animated(
        frames: List<String>,
        frameDuration: Float
    ) {
        require(frames.isNotEmpty()) {
            "An animation must contain at least one frame path."
        }
        require(frames.all { it.isNotBlank() }) {
            "Animation frame paths must not be blank."
        }
        define(
            SpriteSource.AnimatedFiles(
                paths = frames.map(resolvePath),
                frameDuration = frameDuration
            )
        )
    }

    /** Uses a tightly packed sprite sheet as a looping animation. */
    fun spriteSheet(
        path: String,
        frameWidth: Int,
        frameHeight: Int,
        frameDuration: Float,
        frameCount: Int? = null
    ) {
        require(path.isNotBlank()) { "Sprite sheet path must not be blank." }
        define(
            SpriteSource.SpriteSheet(
                path = resolvePath(path),
                frameWidth = frameWidth,
                frameHeight = frameHeight,
                frameDuration = frameDuration,
                frameCount = frameCount
            )
        )
    }

    /** Uses one untrimmed, unrotated texture-atlas region. */
    fun atlas(
        atlas: String,
        region: String
    ) {
        define(SpriteSource.AtlasRegion(atlas, region))
    }

    /** Uses indexed texture-atlas regions as a looping animation. */
    fun animatedAtlas(
        atlas: String,
        region: String,
        frameDuration: Float
    ) {
        define(SpriteSource.AtlasAnimation(atlas, region, frameDuration))
    }

    internal fun define(source: SpriteSource) {
        check(definedSource == null) {
            "A sprite definition must contain exactly one sprite source."
        }
        definedSource = source
    }

    internal open fun build(): SpriteSource {
        return definedSource
            ?: error("A sprite definition must contain exactly one sprite source.")
    }
}

internal fun spriteSource(
    resolvePath: (String) -> String,
    configure: SpriteDefinitionBuilder.() -> Unit
): SpriteSource {
    return SpriteDefinitionBuilder(resolvePath).apply(configure).build()
}
