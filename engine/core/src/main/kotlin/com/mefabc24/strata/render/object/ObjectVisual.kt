package com.mefabc24.strata.render.`object`

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.mefabc24.strata.render.sprite.SpriteFrames

/** A prepared object frame and its matching pixel-alpha mask. */
data class ObjectVisualFrame(
    val texture: TextureRegion,
    val alphaMask: AlphaMask?
)

/**
 * Defines how a placed object is drawn and resolves its current frame.
 */
class ObjectVisual internal constructor(
    val sprite: SpriteFrames,
    alphaMasks: List<AlphaMask?>,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val width: Float? = null,
    val height: Float? = null,
    val scale: Float = 1f
) {
    private val frames: List<ObjectVisualFrame>

    /** Creates a static object visual with the existing API. */
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
            "Each object animation frame must have one alpha-mask entry."
        }
        require(scale.isFinite() && scale > 0f) {
            "Sprite scale must be positive and finite."
        }
        require(width == null || (width.isFinite() && width > 0f)) {
            "Sprite width must be positive and finite."
        }
        require(height == null || (height.isFinite() && height > 0f)) {
            "Sprite height must be positive and finite."
        }

        frames = List(sprite.frameCount) { index ->
            ObjectVisualFrame(
                texture = sprite.frameAtIndex(index),
                alphaMask = alphaMasks[index]
            )
        }
    }

    /** The static texture or first animation frame. */
    val texture: TextureRegion
        get() = frames.first().texture

    /** The static mask or first animation frame's mask. */
    val alphaMask: AlphaMask?
        get() = frames.first().alphaMask

    /** Resolves a prepared texture/mask pair without allocating. */
    fun frameAt(stateTime: Float): ObjectVisualFrame {
        return frames[sprite.frameIndexAt(stateTime)]
    }
}
