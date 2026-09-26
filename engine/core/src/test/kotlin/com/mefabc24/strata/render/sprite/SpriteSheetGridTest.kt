package com.mefabc24.strata.render.sprite

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SpriteSheetGridTest {

    @Test
    fun `cells are split in row major order`() {
        assertEquals(
            listOf(
                SpriteSheetCell(0, 0, 16, 12),
                SpriteSheetCell(16, 0, 16, 12),
                SpriteSheetCell(32, 0, 16, 12),
                SpriteSheetCell(0, 12, 16, 12),
                SpriteSheetCell(16, 12, 16, 12)
            ),
            SpriteSheetGrid.cells(
                sheetWidth = 48,
                sheetHeight = 24,
                frameWidth = 16,
                frameHeight = 12,
                frameCount = 5
            )
        )
    }

    @Test
    fun `omitted frame count uses every available cell`() {
        assertEquals(
            6,
            SpriteSheetGrid.cells(
                sheetWidth = 48,
                sheetHeight = 24,
                frameWidth = 16,
                frameHeight = 12
            ).size
        )
    }

    @Test
    fun `invalid dimensions and frame counts are rejected`() {
        assertFailsWith<IllegalArgumentException> {
            SpriteSheetGrid.cells(32, 32, 0, 16)
        }
        assertFailsWith<IllegalArgumentException> {
            SpriteSheetGrid.cells(32, 32, 16, -1)
        }
        assertFailsWith<IllegalArgumentException> {
            SpriteSheetGrid.cells(30, 32, 16, 16)
        }
        assertFailsWith<IllegalArgumentException> {
            SpriteSheetGrid.cells(32, 32, 16, 16, frameCount = 0)
        }
        assertFailsWith<IllegalArgumentException> {
            SpriteSheetGrid.cells(32, 32, 16, 16, frameCount = 5)
        }
    }
}
