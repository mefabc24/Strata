package com.mefabc24.strata.render.sprite

import com.badlogic.gdx.graphics.g2d.TextureRegion

/** One prepared animation frame and matching optional frame metadata. */
data class PreparedSpriteFrame<out M>(
    val texture: TextureRegion,
    val metadata: M
)

/**
 * Pairs common [SpriteFrames] playback with frame-specific prepared metadata.
 * Terrain can use [SpriteFrames] directly when it does not need metadata.
 */
class PreparedSprite<M> internal constructor(
    val sprite: SpriteFrames,
    metadata: List<M>
) {
    private val frames: List<PreparedSpriteFrame<M>>

    init {
        require(metadata.size == sprite.frameCount) {
            "Prepared sprite metadata count must match its frame count."
        }
        frames = List(sprite.frameCount) { index ->
            PreparedSpriteFrame(
                texture = sprite.frameAtIndex(index),
                metadata = metadata[index]
            )
        }
    }

    val frameCount: Int
        get() = frames.size

    fun frameAt(stateTime: Float): PreparedSpriteFrame<M> {
        return frames[sprite.frameIndexAt(stateTime)]
    }

    fun frameAtIndex(index: Int): PreparedSpriteFrame<M> {
        return frames[index]
    }
}
