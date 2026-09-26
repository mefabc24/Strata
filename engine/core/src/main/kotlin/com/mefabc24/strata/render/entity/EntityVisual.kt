package com.mefabc24.strata.render.entity

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.mefabc24.strata.render.`object`.AlphaMask
import com.mefabc24.strata.render.sprite.SpriteFrames

/** A prepared entity frame and its matching pixel-alpha mask. */
data class EntityVisualFrame(
    val texture: TextureRegion,
    val alphaMask: AlphaMask?
)

/** Defines a bottom-center anchored entity sprite. */
class EntityVisual internal constructor(
    val sprite: SpriteFrames,
    alphaMasks: List<AlphaMask?>,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val width: Float? = null,
    val height: Float? = null,
    val scale: Float = 1f
) {
    private val frames: List<EntityVisualFrame>

    constructor(
        texture: TextureRegion,
        offsetX: Float = 0f,
        offsetY: Float = 0f,
        alphaMask: AlphaMask? = null,
        width: Float? = null,
        height: Float? = null,
        scale: Float = 1f
    ) : this(
        sprite = SpriteFrames.static(texture),
        alphaMasks = listOf(alphaMask),
        offsetX = offsetX,
        offsetY = offsetY,
        width = width,
        height = height,
        scale = scale
    )

    init {
        require(alphaMasks.size == sprite.frameCount) {
            "Each entity animation frame must have one alpha-mask entry."
        }
        require(offsetX.isFinite() && offsetY.isFinite()) {
            "Entity sprite offsets must be finite."
        }
        require(width == null || (width.isFinite() && width > 0f)) {
            "Entity sprite width must be positive and finite."
        }
        require(height == null || (height.isFinite() && height > 0f)) {
            "Entity sprite height must be positive and finite."
        }
        require(scale.isFinite() && scale > 0f) {
            "Entity sprite scale must be positive and finite."
        }

        frames = List(sprite.frameCount) { index ->
            EntityVisualFrame(
                texture = sprite.frameAtIndex(index),
                alphaMask = alphaMasks[index]
            )
        }
    }

    val texture: TextureRegion
        get() = frames.first().texture

    fun frameAt(stateTime: Float): EntityVisualFrame {
        return frames[sprite.frameIndexAt(stateTime)]
    }
}
