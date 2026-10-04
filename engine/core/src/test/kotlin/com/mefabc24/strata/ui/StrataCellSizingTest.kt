package com.mefabc24.strata.ui

import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Widget
import com.badlogic.gdx.utils.Align
import com.mefabc24.strata.testing.TestGdxEnvironment
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StrataCellSizingTest {

    @BeforeTest
    fun installTestEnvironment() {
        TestGdxEnvironment.install()
    }

    @Test
    fun `hug tracks the actor preferred size`() = withLayout { layout ->
        val actor = PreferredWidget(80f, 20f)

        layout.actor(actor).cell {
            growX()
            fillX()
            hugX()
        }
        layout.pack()

        assertEquals(80f, layout.prefWidth)
        assertEquals(80f, actor.width)

        actor.preferredWidth = 125f
        actor.invalidateHierarchy()
        layout.pack()

        assertEquals(125f, layout.prefWidth)
        assertEquals(125f, actor.width)
    }

    @Test
    fun `fill available consumes additional width`() = withLayout { layout ->
        val actor = PreferredWidget(40f, 20f)

        layout.actor(actor).cell {
            fillAvailableX()
        }
        layout.setSize(120f, 20f)
        layout.validate()

        assertEquals(120f, actor.width)
    }

    @Test
    fun `fixed width remains explicit`() = withLayout { layout ->
        val actor = PreferredWidget(90f, 20f)

        layout.actor(actor).cell {
            fixedWidth(52f)
        }
        layout.pack()

        assertEquals(52f, layout.prefWidth)
        assertEquals(52f, actor.width)
    }

    @Test
    fun `minimum width is respected while larger content wins`() = withLayout { layout ->
        val actor = PreferredWidget(70f, 20f)

        layout.actor(actor).cell {
            minWidth(100f)
            hugX()
        }
        assertEquals(100f, layout.prefWidth)

        actor.preferredWidth = 140f
        actor.invalidateHierarchy()

        assertEquals(140f, layout.prefWidth)
    }

    @Test
    fun `uniform grid uses its widest preferred child`() = withGrid(
        columns = 3,
        spacing = 4f
    ) { grid ->
        listOf(36f, 48f, 104f).forEach { width ->
            grid.actor(PreferredWidget(width, 20f)).cell {
                fillAvailableX()
                uniformX()
            }
        }
        grid.pack()

        assertEquals(104f * 3f + 4f * 2f, grid.prefWidth)
        grid.children.forEach { child ->
            assertEquals(104f, child.width)
            assertTrue(child.x >= 0f)
            assertTrue(child.x + child.width <= grid.width)
        }
    }

    @Test
    fun `padding and sibling spacing contribute predictably`() {
        val skin = Skin()
        val context = context(skin)
        val row = StrataRow(
            context = context,
            spacing = 7f,
            padding = StrataInsets(
                top = 2f,
                left = 11f,
                bottom = 3f,
                right = 13f
            ),
            alignment = Align.left
        )

        try {
            row.actor(PreferredWidget(30f, 10f))
            row.actor(PreferredWidget(40f, 10f))

            assertEquals(30f + 40f + 7f + 11f + 13f, row.prefWidth)
            assertEquals(10f + 2f + 3f, row.prefHeight)
        } finally {
            context.dispose()
            skin.dispose()
        }
    }

    @Test
    fun `nested row column and grid preserve preferred sizes`() =
        withLayout { outer ->
            outer.row(spacing = 5f) {
                actor(PreferredWidget(20f, 10f))
                column(spacing = 3f) {
                    actor(PreferredWidget(30f, 12f))
                    grid(columns = 2, spacing = 4f) {
                        actor(PreferredWidget(14f, 8f))
                        actor(PreferredWidget(18f, 8f))
                    }
                }
            }

            assertEquals(20f + 5f + 36f, outer.prefWidth)
            assertEquals(12f + 3f + 8f, outer.prefHeight)
        }

    @Test
    fun `raw cell customization remains available`() = withLayout { layout ->
        val actor = layout.actor(PreferredWidget(80f, 30f)).cell {
            width(41f)
            height(23f)
        }

        layout.pack()

        assertEquals(41f, actor.width)
        assertEquals(23f, actor.height)
    }

    @Test
    fun `terrain regression lets panel grow beyond its minimum`() {
        val skin = Skin()
        val context = context(skin)
        val panel = StrataPanel(
            context = context,
            styleName = null,
            spacing = 0f,
            paddingOverride = StrataInsets.all(12f),
            blocksInput = true
        )

        try {
            val grid = panel.grid(columns = 3, spacing = 4f) {
                listOf(54f, 50f, 112f).forEach { width ->
                    actor(PreferredWidget(width, 30f)).cell {
                        fillAvailableX()
                        uniformX()
                    }
                }
            }.cell {
                fillAvailableX()
            }

            val root = StrataColumn(
                context = context,
                spacing = 0f,
                padding = StrataInsets.NONE,
                alignment = Align.topLeft
            )
            root.actor(panel).cell {
                minWidth(280f)
                hugX()
            }
            root.pack()
            panel.validate()
            grid.validate()

            val expectedGridWidth = 112f * 3f + 4f * 2f
            assertEquals(expectedGridWidth, grid.prefWidth)
            assertEquals(expectedGridWidth + 24f, panel.prefWidth)
            assertTrue(panel.width > 280f)

            grid.children.forEach { child ->
                assertTrue(child.x >= 0f)
                assertTrue(child.x + child.width <= grid.width)
            }
        } finally {
            context.dispose()
            skin.dispose()
        }
    }

    private inline fun withLayout(
        block: (StrataColumn) -> Unit
    ) {
        val skin = Skin()
        val context = context(skin)
        val layout = StrataColumn(
            context = context,
            spacing = 0f,
            padding = StrataInsets.NONE,
            alignment = Align.topLeft
        )

        try {
            block(layout)
        } finally {
            context.dispose()
            skin.dispose()
        }
    }

    private inline fun withGrid(
        columns: Int,
        spacing: Float,
        block: (StrataGrid) -> Unit
    ) = withLayout { outer ->
        val grid = outer.grid(
            columns = columns,
            spacing = spacing,
            configure = {}
        )
        block(grid)
    }

    private fun context(skin: Skin) = StrataUiContext(
        skin = skin,
        theme = StrataUiTheme(spacing = 0f)
    )

    private class PreferredWidget(
        var preferredWidth: Float,
        private val preferredHeight: Float
    ) : Widget() {
        override fun getPrefWidth(): Float = preferredWidth

        override fun getPrefHeight(): Float = preferredHeight
    }
}
