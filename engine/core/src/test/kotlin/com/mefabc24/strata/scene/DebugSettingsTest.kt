package com.mefabc24.strata.scene

import com.badlogic.gdx.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DebugSettingsTest {

    @Test
    fun `world diagnostics are disabled by default`() {
        val settings = DebugSettings()

        assertFalse(settings.objects.enabled)
        assertFalse(settings.entities.enabled)
        assertTrue(settings.objects.showOccupiedTiles)
        assertTrue(settings.objects.showOriginTile)
        assertTrue(settings.entities.showCurrentTile)
        assertTrue(settings.entities.showPosition)
        assertTrue(settings.entities.showPath)
    }

    @Test
    fun `object and entity blocks remain mutable at runtime`() {
        val settings = DebugSettings().apply {
            objects {
                enabled = true
                showSpriteBounds = true
            }
            entities {
                enabled = true
                showPath = false
                showDirection = true
            }
        }

        assertTrue(settings.objects.enabled)
        assertTrue(settings.objects.showSpriteBounds)
        assertTrue(settings.entities.enabled)
        assertFalse(settings.entities.showPath)
        assertTrue(settings.entities.showDirection)

        settings.objects.enabled = false
        settings.entities.showPath = true

        assertFalse(settings.objects.enabled)
        assertTrue(settings.entities.showPath)
    }

    @Test
    fun `world diagnostic colors are copied on assignment and access`() {
        val supplied = Color(1f, 0.5f, 0.25f, 0.75f)
        val settings = DebugSettings().apply {
            objects.occupiedTileColor = supplied
            objects.occupiedTileFillColor = supplied
            entities.pathColor = supplied
            entities.currentTileFillColor = supplied
        }

        supplied.a = 0.1f
        settings.objects.occupiedTileColor.a = 0.2f
        settings.objects.occupiedTileFillColor?.a = 0.3f
        settings.entities.pathColor.a = 0.4f
        settings.entities.currentTileFillColor?.a = 0.5f

        assertEquals(0.75f, settings.objects.occupiedTileColor.a)
        assertEquals(0.75f, settings.objects.occupiedTileFillColor?.a)
        assertEquals(0.75f, settings.entities.pathColor.a)
        assertEquals(0.75f, settings.entities.currentTileFillColor?.a)
    }

    @Test
    fun `debug grid is disabled by default`() {
        assertFalse(DebugSettings().grid.enabled)
        assertEquals(DebugGridExtent.WORLD, DebugSettings().grid.extent)
    }

    @Test
    fun `grid block configures debug grid`() {
        val settings = DebugSettings().apply {
            grid {
                enabled = true
                extent = DebugGridExtent.VISIBLE
            }
        }

        assertTrue(settings.grid.enabled)
        assertEquals(DebugGridExtent.VISIBLE, settings.grid.extent)
        assertEquals(DebugGridExtent.VISIBLE, settings.grid.copy().extent)
    }

    @Test
    fun `grid colors are copied on assignment and access`() {
        val supplied = Color(1f, 0.5f, 0.25f, 0.75f)
        val settings = DebugGridSettings().apply {
            color = supplied
            backgroundColor = supplied
        }

        supplied.a = 0.1f
        settings.color.a = 0.2f
        settings.backgroundColor?.a = 0.3f

        assertEquals(0.75f, settings.color.a)
        assertEquals(0.75f, settings.backgroundColor?.a)
    }

    @Test
    fun `grid line width rejects invalid values`() {
        val settings = DebugGridSettings()

        for (invalid in listOf(
            0f,
            -1f,
            Float.NaN,
            Float.POSITIVE_INFINITY,
            Float.NEGATIVE_INFINITY
        )) {
            assertFailsWith<IllegalArgumentException> {
                settings.lineWidth = invalid
            }
        }
    }

    @Test
    fun `world diagnostic line widths reject invalid values`() {
        val invalidValues = listOf(
            0f,
            -1f,
            Float.NaN,
            Float.POSITIVE_INFINITY,
            Float.NEGATIVE_INFINITY
        )

        for (invalid in invalidValues) {
            assertFailsWith<IllegalArgumentException> {
                DebugObjectSettings().lineWidth = invalid
            }
            assertFailsWith<IllegalArgumentException> {
                DebugEntitySettings().lineWidth = invalid
            }
        }
    }
}
