package com.mefabc24.sandbox

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.Scaling
import com.mefabc24.strata.iso.IsoProjection
import com.mefabc24.strata.iso.IsoWorldView
import com.mefabc24.strata.iso.TileGeometry
import com.mefabc24.strata.ui.StrataUi

/**
 * Displays a Sandbox-only crosshair over the currently hovered world tile.
 */
internal class SandboxHoverCrosshair(
    ui: StrataUi,
    private val view: IsoWorldView,
    private val tools: SandboxToolController,
    tileGeometry: TileGeometry,
    private val offsetY: Float = 0f
) : Disposable {

    private val projection = IsoProjection(tileGeometry)

    private val texture =
        Texture(Gdx.files.internal("ui/selection/crosshair3.png"))

    private val image = Image(
        TextureRegionDrawable(texture)
    ).apply {
        setScaling(Scaling.stretch)
        touchable = Touchable.disabled
        isVisible = false
    }

    private val bottomLeft = Vector3()
    private val topRight = Vector3()

    init {
        ui.stage.addActor(image)
    }

    fun update() {
        if (tools.mode == SandboxMode.BUILD) {
            image.isVisible = false
            return
        }

        val tile = view.hoveredTile
        if (tile == null) {
            image.isVisible = false
            return
        }

        val tileTop = projection.tileToWorld(
            tile.x,
            tile.y
        )

        bottomLeft.set(
            tileTop.x - projection.tileWidth / 2f,
            tileTop.y - projection.tileHeight,
            0f
        )

        topRight.set(
            tileTop.x + projection.tileWidth / 2f,
            tileTop.y,
            0f
        )

        view.camera.project(bottomLeft)
        view.camera.project(topRight)

        image.setBounds(
            bottomLeft.x,
            bottomLeft.y + offsetY,
            topRight.x - bottomLeft.x,
            topRight.y - bottomLeft.y
        )

        image.isVisible = true
    }

    override fun dispose() {
        image.remove()
        texture.dispose()
    }
}