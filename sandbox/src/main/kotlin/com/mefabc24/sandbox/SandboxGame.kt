package com.mefabc24.sandbox

import com.mefabc24.strata.StrataGame
import com.mefabc24.strata.world.World

class SandboxGame : StrataGame {

    private lateinit var world: World

    override fun create() {
        world = World(10, 10) { _, _ ->
            SandboxTile(TerrainType.GRASS)
        }

        println("World created: ${world.width} x ${world.height}")

        println("Before: ${world.getTile(2, 3)}")

        world.setTile(2, 3, SandboxTile(TerrainType.WATER))

        println("After: ${world.getTile(2, 3)}")

        println("Outside: ${world.getTile(20, 20)}")
    }

    override fun update(delta: Float) {
        // update game logic
    }

    override fun render() {
        // render game world
    }

    override fun dispose() {
        println("Strata sandbox disposed")
    }
}