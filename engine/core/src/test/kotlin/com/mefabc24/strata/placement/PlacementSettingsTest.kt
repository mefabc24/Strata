package com.mefabc24.strata.placement

import com.badlogic.gdx.graphics.Color
import com.mefabc24.strata.world.Footprint
import com.mefabc24.strata.world.Placeable
import com.mefabc24.strata.world.Tile
import com.mefabc24.strata.world.TilePosition
import com.mefabc24.strata.world.World
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PlacementSettingsTest {
    @Test
    fun `preview settings preserve existing defaults`() {
        val preview = PlacementSettings().preview

        assertTrue(preview.objects.enabled)
        assertEquals(Color(0.5f, 1f, 0.5f, 0.65f), preview.objects.validColor)
        assertEquals(Color(1f, 0.4f, 0.4f, 0.65f), preview.objects.invalidColor)
        assertEquals(
            PlacementPreviewBoundsPolicy.ALL_TILES_INSIDE,
            preview.objects.boundsPolicy
        )
        assertTrue(preview.entities.enabled)
        assertEquals(Color(0.5f, 1f, 0.5f, 0.65f), preview.entities.validColor)
        assertEquals(Color(1f, 0.4f, 0.4f, 0.65f), preview.entities.invalidColor)
    }

    @Test
    fun `assigned colors and copied settings are snapshots`() {
        val objectColor = Color(0.1f, 0.2f, 0.3f, 0.4f)
        val entityColor = Color(0.6f, 0.7f, 0.8f, 0.9f)
        val settings = PlacementSettings().apply {
            preview {
                objects { validColor = objectColor }
                entities { invalidColor = entityColor }
            }
        }

        objectColor.set(Color.RED)
        entityColor.set(Color.BLUE)
        val copy = settings.copy()
        settings.preview.objects.validColor.set(Color.GREEN)
        settings.preview.entities.invalidColor.set(Color.YELLOW)

        assertEquals(Color(0.1f, 0.2f, 0.3f, 0.4f), copy.preview.objects.validColor)
        assertEquals(Color(0.6f, 0.7f, 0.8f, 0.9f), copy.preview.entities.invalidColor)
        assertNotSame(settings.preview.objects.validColor, copy.preview.objects.validColor)
        assertNotSame(settings.preview.entities.invalidColor, copy.preview.entities.invalidColor)
    }

    @Test
    fun `disabled object previews do not disable placement`() {
        val world = World(2, 2) { _, _ -> TestTile }
        val controller = PlacementSettings().apply {
            preview { objects { enabled = false } }
        }.createController(world)
        controller.selectedFactory = ::TestPlaceable

        controller.update(TilePosition(1, 1))

        assertTrue(controller.previews.isEmpty())
        assertNotNull(controller.placeAt(1, 1))
    }

    private data object TestTile : Tile
    private class TestPlaceable : Placeable {
        override val footprint = Footprint.square(1)
    }
}
