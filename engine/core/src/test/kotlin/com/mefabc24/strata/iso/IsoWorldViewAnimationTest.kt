package com.mefabc24.strata.iso

import com.mefabc24.strata.testing.TestGdxEnvironment
import com.mefabc24.strata.world.Tile
import com.mefabc24.strata.world.World
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class IsoWorldViewAnimationTest {

    private class TestTile : Tile

    @BeforeTest
    fun installTestEnvironment() {
        TestGdxEnvironment.install()
    }

    @Test
    fun `simulation delta alone advances the shared animation time`() {
        val observedRenderTimes = mutableListOf<Float>()
        val view = IsoWorldView(
            world = World(1, 1) { _, _ -> TestTile() },
            textureFor = { _, stateTime ->
                observedRenderTimes += stateTime
                null
            }
        )

        try {
            assertEquals(0f, view.animationTime)

            view.render()
            assertEquals(0f, view.animationTime)

            view.update(
                realDelta = 0.25f,
                simulationDelta = 0f
            )
            assertEquals(0f, view.animationTime)

            view.update(
                realDelta = 0.25f,
                simulationDelta = 0.5f
            )
            assertEquals(0.5f, view.animationTime)

            view.render()
            view.render()
            assertEquals(0.5f, view.animationTime)

            view.resize(1024, 768)
            assertEquals(0.5f, view.animationTime)

            view.update(
                realDelta = 0.5f,
                simulationDelta = 1f
            )
            assertEquals(1.5f, view.animationTime)
            view.render()

            assertEquals(
                listOf(0f, 0.5f, 0.5f, 1.5f),
                observedRenderTimes
            )
        } finally {
            view.dispose()
        }
    }
}
