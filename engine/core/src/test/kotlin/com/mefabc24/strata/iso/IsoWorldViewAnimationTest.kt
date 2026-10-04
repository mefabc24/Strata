package com.mefabc24.strata.iso

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.mefabc24.strata.debug.DebugSettings
import com.mefabc24.strata.render.entity.EntityVisual
import com.mefabc24.strata.render.entity.ResolvedEntityVisual
import com.mefabc24.strata.testing.TestGdxEnvironment
import com.mefabc24.strata.world.Entity
import com.mefabc24.strata.world.EntityPosition
import com.mefabc24.strata.world.Tile
import com.mefabc24.strata.world.World
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class IsoWorldViewAnimationTest {

    private class TestTile : Tile
    private data object TestEntity : Entity

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

    @Test
    fun `waiting debug path entity uses representative visual until released`() {
        val world = World(1, 1) { _, _ -> TestTile() }
        val entity = world.addEntity(TestEntity, EntityPosition(0.5f, 0.5f))
        val normal = EntityVisual(TextureRegion())
        val representative = EntityVisual(TextureRegion())
        val settings = DebugSettings()
        val view = IsoWorldView(
            world = world,
            textureFor = { _, _ -> null },
            resolvedEntityVisualFor = { _, time ->
                ResolvedEntityVisual(normal, time, null)
            },
            resolvedRepresentativeEntityVisualFor = { _, time ->
                ResolvedEntityVisual(representative, time, null)
            },
            debugSettings = settings
        )

        try {
            assertSame(normal, view.resolvedEntityVisual(entity)?.visual)

            settings.worldState.pathfindingEntity = entity
            settings.worldState.pathfindingEntityWaiting = true
            assertSame(representative, view.resolvedEntityVisual(entity)?.visual)

            settings.worldState.pathfindingEntityWaiting = false
            assertSame(normal, view.resolvedEntityVisual(entity)?.visual)
        } finally {
            view.dispose()
        }
    }

    @Test
    fun `global visual freeze does not consume simulation delta`() {
        val settings = DebugSettings().apply {
            operations.simulation.enabled = true
            operations.simulation.freezeVisualAnimations = true
        }
        val view = IsoWorldView(
            world = World(1, 1) { _, _ -> TestTile() },
            textureFor = { _, _ -> null },
            debugSettings = settings
        )

        try {
            view.update(realDelta = 0.25f, simulationDelta = 1f)
            assertEquals(0f, view.animationTime)

            settings.operations.simulation.freezeVisualAnimations = false
            view.update(realDelta = 0.25f, simulationDelta = 0.5f)
            assertEquals(0.5f, view.animationTime)

            settings.operations.simulation.freezeVisualAnimations = true
            settings.operations.simulation.enabled = false
            view.update(realDelta = 0.25f, simulationDelta = 0.5f)
            assertEquals(1f, view.animationTime)
        } finally {
            view.dispose()
        }
    }
}
