package com.mefabc24.sandbox

import com.badlogic.gdx.graphics.OrthographicCamera
import com.mefabc24.strata.StrataGame
import com.mefabc24.strata.iso.IsoProjection
import com.mefabc24.strata.render.IsoGridRenderer
import com.mefabc24.strata.world.World

class SandboxGame : StrataGame {

    private lateinit var world: World
    private lateinit var camera: OrthographicCamera
    private lateinit var renderer: IsoGridRenderer
    private lateinit var projection: IsoProjection

    override fun create() {
        world = World(10, 10) { _, _ ->
            SandboxTile(TerrainType.GRASS)
        }

        camera = OrthographicCamera().apply {
            setToOrtho(false, 1280f, 720f)
            position.set(0f, -160f, 0f)
            update()
        }

        projection = IsoProjection(
            tileWidth = 64f,
            tileHeight = 32f
        )

        renderer = IsoGridRenderer(projection)
    }

    override fun update(delta: Float) {
        // update game logic
    }

    override fun render() {
        renderer.render(world, camera)
    }

    override fun dispose() {
        renderer.dispose()
    }
}