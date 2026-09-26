package com.mefabc24.strata.scene

import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.mefabc24.strata.camera.CameraSettings
import com.mefabc24.strata.input.ControlsSettings
import com.mefabc24.strata.iso.IsoWorldView
import com.mefabc24.strata.render.`object`.ObjectVisual
import com.mefabc24.strata.render.entity.EntityVisual
import com.mefabc24.strata.render.preview.PlacementPreview
import com.mefabc24.strata.render.RenderStats
import com.mefabc24.strata.render.RenderingSettings
import com.mefabc24.strata.world.PlacedObject
import com.mefabc24.strata.world.Tile
import com.mefabc24.strata.world.TilePosition
import com.mefabc24.strata.world.World
import com.mefabc24.strata.world.WorldEntity

internal data class SceneWorldViewSpec(
    val world: World,
    val textureFor: (Tile, Float) -> TextureRegion?,
    val objectVisualFor: (PlacedObject) -> ObjectVisual?,
    val entityVisualFor: (WorldEntity) -> EntityVisual?,
    val cameraSettings: CameraSettings,
    val controlsSettings: ControlsSettings,
    val renderingSettings: RenderingSettings,
    val debugGridSettings: DebugGridSettings
)

internal interface SceneWorldView {
    val publicView: IsoWorldView?
    val inputProcessor: InputProcessor
    val hoveredTile: TilePosition?
    val renderStats: RenderStats

    fun update(delta: Float)

    fun render(previews: List<PlacementPreview>)

    fun resize(width: Int, height: Int)

    fun dispose()
}

internal fun interface SceneWorldViewFactory {
    fun create(spec: SceneWorldViewSpec): SceneWorldView
}

internal object DefaultSceneWorldViewFactory : SceneWorldViewFactory {
    override fun create(spec: SceneWorldViewSpec): SceneWorldView {
        return DefaultSceneWorldView(
            IsoWorldView(
                world = spec.world,
                textureFor = spec.textureFor,
                objectVisualFor = spec.objectVisualFor,
                entityVisualFor = spec.entityVisualFor,
                cameraSettings = spec.cameraSettings,
                controls = spec.controlsSettings,
                renderingSettings = spec.renderingSettings,
                debugGridSettings = spec.debugGridSettings
            )
        )
    }
}

private class DefaultSceneWorldView(
    override val publicView: IsoWorldView
) : SceneWorldView {
    override val inputProcessor: InputProcessor
        get() = publicView.inputProcessor

    override val hoveredTile: TilePosition?
        get() = publicView.hoveredTile

    override val renderStats: RenderStats
        get() = publicView.renderStats

    override fun update(delta: Float) {
        publicView.update(delta)
    }

    override fun render(previews: List<PlacementPreview>) {
        publicView.render(previews)
    }

    override fun resize(width: Int, height: Int) {
        publicView.resize(width, height)
    }

    override fun dispose() {
        publicView.dispose()
    }
}
