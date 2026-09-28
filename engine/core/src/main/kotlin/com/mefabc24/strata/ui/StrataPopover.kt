package com.mefabc24.strata.ui

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.Touchable

/** Non-interactive floating content that stays within its UI stage. */
class StrataPopover internal constructor(
    private val stage: Stage,
    val content: StrataPanel,
    val popoverWidth: Float,
    val popoverHeight: Float
) {
    init {
        require(popoverWidth.isFinite() && popoverWidth > 0f)
        require(popoverHeight.isFinite() && popoverHeight > 0f)
        content.setSize(popoverWidth, popoverHeight)
        content.touchable = Touchable.disabled
        content.isVisible = false
        stage.addActor(content)
    }

    val visible: Boolean get() = content.isVisible

    /** Shows beside [container], vertically aligned to [anchor], and clamps to the viewport. */
    fun showRightOf(container: Actor, anchor: Actor, margin: Float = 8f) {
        val containerOrigin = container.localToStageCoordinates(Vector2())
        val anchorTop = anchor.localToStageCoordinates(Vector2(0f, anchor.height))
        val stageWidth = stage.viewport.worldWidth
        val stageHeight = stage.viewport.worldHeight
        var x = containerOrigin.x + container.width + margin
        if (x + popoverWidth + margin > stageWidth) {
            x = containerOrigin.x - popoverWidth - margin
        }
        val y = (anchorTop.y - popoverHeight).coerceIn(
            margin,
            (stageHeight - popoverHeight - margin).coerceAtLeast(margin)
        )
        content.setPosition(
            x.coerceIn(margin, (stageWidth - popoverWidth - margin).coerceAtLeast(margin)),
            y
        )
        content.toFront()
        content.isVisible = true
    }

    fun hide() {
        content.isVisible = false
    }
}

