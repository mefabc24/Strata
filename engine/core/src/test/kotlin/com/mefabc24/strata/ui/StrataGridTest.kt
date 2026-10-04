package com.mefabc24.strata.ui

import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.utils.Align
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class StrataGridTest {

    @Test
    fun `grid rejects non-positive column counts`() {
        val skin = Skin()
        val context = context(skin)

        try {
            for (invalid in listOf(0, -1, Int.MIN_VALUE)) {
                assertFailsWith<IllegalArgumentException> {
                    grid(context, columns = invalid)
                }
            }
        } finally {
            context.dispose()
            skin.dispose()
        }
    }

    @Test
    fun `grid retains layout spacing validation`() {
        val skin = Skin()
        val context = context(skin)

        try {
            assertFailsWith<IllegalArgumentException> {
                grid(
                    context = context,
                    columns = 2,
                    spacing = Float.NaN
                )
            }
        } finally {
            context.dispose()
            skin.dispose()
        }
    }

    @Test
    fun `grid preserves arbitrary actors in insertion order`() {
        val skin = Skin()
        val context = context(skin)
        val grid = grid(context, columns = 3)
        val actors = List(7) { Actor() }

        try {
            actors.forEach(grid::actor)

            assertEquals(
                actors,
                grid.children.toList()
            )
        } finally {
            context.dispose()
            skin.dispose()
        }
    }

    @Test
    fun `grid keeps fewer items than columns on one row`() {
        assertEquals(
            1 to 2,
            dimensions(columns = 3, itemCount = 2)
        )
    }

    @Test
    fun `grid places exactly one full row`() {
        assertEquals(
            1 to 3,
            dimensions(columns = 3, itemCount = 3)
        )
    }

    @Test
    fun `grid places multiple full rows`() {
        assertEquals(
            2 to 3,
            dimensions(columns = 3, itemCount = 6)
        )
    }

    @Test
    fun `grid leaves an incomplete final row`() {
        assertEquals(
            3 to 3,
            dimensions(columns = 3, itemCount = 7)
        )
    }

    @Test
    fun `grid actors retain scene2d cell customization`() {
        val skin = Skin()
        val context = context(skin)
        val grid = grid(context, columns = 2)

        try {
            val actor = grid.actor(Actor()).cell {
                width(41f)
                height(23f)
            }

            val cell = grid.getCell(actor)

            assertEquals(41f, cell.prefWidth)
            assertEquals(23f, cell.prefHeight)
        } finally {
            context.dispose()
            skin.dispose()
        }
    }

    @Test
    fun `grid nests inside existing layout scopes`() {
        val skin = Skin()
        val context = context(skin)
        val outer = StrataColumn(
            context = context,
            spacing = 0f,
            padding = StrataInsets.NONE,
            alignment = Align.topLeft
        )

        lateinit var nestedGrid: StrataGrid

        try {
            val panel = outer.panel(styleName = null) {
                nestedGrid = grid(columns = 2) {
                    actor(Actor())
                    actor(Actor())
                    actor(Actor())
                }
            }

            assertSame(outer, panel.parent)
            assertSame(panel, nestedGrid.parent)
            assertEquals(3, nestedGrid.children.size)
        } finally {
            context.dispose()
            skin.dispose()
        }
    }

    private fun dimensions(
        columns: Int,
        itemCount: Int
    ): Pair<Int, Int> {
        val skin = Skin()
        val context = context(skin)
        val grid = grid(context, columns)

        return try {
            repeat(itemCount) {
                grid.actor(Actor())
            }

            grid.pack()
            grid.rows to grid.columns
        } finally {
            context.dispose()
            skin.dispose()
        }
    }

    private fun context(skin: Skin) = StrataUiContext(
        skin = skin,
        theme = StrataUiTheme(spacing = 0f)
    )

    private fun grid(
        context: StrataUiContext,
        columns: Int,
        spacing: Float = 0f
    ) = StrataGrid(
        context = context,
        columnCount = columns,
        spacing = spacing,
        padding = StrataInsets.NONE,
        alignment = Align.topLeft
    )
}
