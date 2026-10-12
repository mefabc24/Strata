package com.mefabc24.strata.iso

import com.mefabc24.strata.testing.TestGdxEnvironment
import com.mefabc24.strata.world.Tile
import com.mefabc24.strata.world.World
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class IsoWorldViewViewportTest {
    private class TestTile : Tile

    @BeforeTest
    fun installTestEnvironment() {
        TestGdxEnvironment.install()
    }

    @Test
    fun `minimized viewport suspends picking and clears hover until restored`() {
        val view = view()

        try {
            view.update(0f)
            assertTrue(view.pickingAvailable)
            assertNotNull(view.hoveredGridPosition)

            view.resize(0, 0)

            assertFalse(view.pickingAvailable)
            assertNull(view.hoveredGridPosition)
            assertNull(view.hoveredTile)
            assertNull(view.pickTile(0f, 0f))
            assertFailsWith<IllegalStateException> {
                view.pickGrid(0f, 0f)
            }

            view.update(0.25f)
            assertNull(view.hoveredGridPosition)

            view.resize(640, 480)
            view.update(0f)

            assertTrue(view.pickingAvailable)
            assertNotNull(view.hoveredGridPosition)
            view.pickGrid(0f, 0f)
        } finally {
            view.dispose()
        }
    }

    @Test
    fun `restoring a viewport preserves an explicitly disabled world input state`() {
        val view = view()

        try {
            view.worldInputEnabled = false
            view.resize(0, 0)
            view.resize(800, 600)

            assertFalse(view.worldInputEnabled)
            assertTrue(view.pickingAvailable)
        } finally {
            view.dispose()
        }
    }

    private fun view() = IsoWorldView(
        world = World(2, 2) { _, _ -> TestTile() },
        textureFor = { _, _ -> null }
    )
}
