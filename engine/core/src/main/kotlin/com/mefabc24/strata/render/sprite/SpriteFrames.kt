package com.mefabc24.strata.render.sprite

import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Array

/**
 * Resolves static or looping animated texture regions at a state time.
 */
class SpriteFrames private constructor(
    private val frames: List<TextureRegion>,
    private val animation: Animation<TextureRegion>?
) {
    val frameCount: Int
        get() = frames.size

    val isAnimated: Boolean
        get() = animation != null

    /** Resolves the current texture region without allocating. */
    fun frameAt(stateTime: Float): TextureRegion {
        return frames[frameIndexAt(stateTime)]
    }

    /** Resolves the current index through libGDX Animation. */
    fun frameIndexAt(stateTime: Float): Int {
        require(stateTime.isFinite() && stateTime >= 0f) {
            "Animation state time must be finite and non-negative."
        }

        return animation?.getKeyFrameIndex(stateTime) ?: 0
    }

    /** Returns a prepared frame by index. */
    fun frameAtIndex(index: Int): TextureRegion {
        return frames[index]
    }

    companion object {
        fun static(texture: TextureRegion): SpriteFrames {
            return SpriteFrames(
                frames = listOf(texture),
                animation = null
            )
        }

        fun animated(
            frames: List<TextureRegion>,
            frameDuration: Float
        ): SpriteFrames {
            require(frames.isNotEmpty()) {
                "An animation must contain at least one frame."
            }
            validateFrameDuration(frameDuration)

            val preparedFrames = frames.toList()
            val first = preparedFrames.first()

            require(
                preparedFrames.all {
                    it.regionWidth == first.regionWidth &&
                        it.regionHeight == first.regionHeight
                }
            ) {
                "Animation frames must have identical pixel dimensions."
            }

            val keyFrames = Array<TextureRegion>(preparedFrames.size)
            preparedFrames.forEach(keyFrames::add)

            return SpriteFrames(
                frames = preparedFrames,
                animation = Animation(
                    frameDuration,
                    keyFrames,
                    Animation.PlayMode.LOOP
                )
            )
        }

        internal fun validateFrameDuration(frameDuration: Float) {
            require(frameDuration.isFinite() && frameDuration > 0f) {
                "Frame duration must be finite and positive."
            }
        }
    }
}
