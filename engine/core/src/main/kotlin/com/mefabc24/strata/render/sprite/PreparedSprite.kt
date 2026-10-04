package com.mefabc24.strata.render.sprite

import com.badlogic.gdx.graphics.g2d.TextureRegion

/**
 * Pairs common [SpriteFrames] playback with domain-specific prepared frames.
 * Terrain can use [SpriteFrames] directly when it does not need metadata.
 */
internal class PreparedSprite<F : Any> private constructor(
    val sprite: SpriteFrames,
    private val frames: List<F>
) {
    init {
        require(frames.size == sprite.frameCount) {
            "Prepared sprite frame count must match its sprite frame count."
        }
    }

    fun frameAt(stateTime: Float): F {
        return frames[sprite.frameIndexAt(stateTime)]
    }

    fun frameAtIndex(index: Int): F {
        return frames[index]
    }

    companion object {
        fun <M, F : Any> prepare(
            sprite: SpriteFrames,
            metadata: List<M>,
            frameFor: (TextureRegion, M) -> F
        ): PreparedSprite<F> {
            require(metadata.size == sprite.frameCount) {
                "Prepared sprite metadata count must match its frame count."
            }
            return PreparedSprite(
                sprite = sprite,
                frames = List(sprite.frameCount) { index ->
                    frameFor(sprite.frameAtIndex(index), metadata[index])
                }
            )
        }
    }
}
