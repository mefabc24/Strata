package com.mefabc24.strata.debug.ui

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.mefabc24.strata.testing.TestGdxEnvironment
import com.mefabc24.strata.ui.StrataSelectionGroup
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class DebugToolSelectionListTest {
    private val texture = TextureRegion()
    private val items = listOf(
        DebugToolSelectionItem("house", "Town House", texture),
        DebugToolSelectionItem("tree", "Oak Tree", texture),
        DebugToolSelectionItem("villa", "Villa", texture)
    )

    @BeforeTest
    fun installTestEnvironment() {
        TestGdxEnvironment.install()
    }

    @Test
    fun `search filters display names without changing registry order`() {
        assertEquals(
            listOf("house"),
            filterDebugToolSelectionItems(items, "  HOUSE ").map { it.value }
        )
        assertEquals(
            listOf("tree"),
            filterDebugToolSelectionItems(items, "oak").map { it.value }
        )
        assertEquals(items, filterDebugToolSelectionItems(items, " "))
        assertEquals(emptyList(), filterDebugToolSelectionItems(items, "missing"))
    }

    @Test
    fun `filtering does not clear a selection excluded from the results`() {
        val selection = StrataSelectionGroup(items.map { it.value }, "villa")

        filterDebugToolSelectionItems(items, "tree")

        assertEquals("villa", selection.selected)
    }

    @Test
    fun `selection list height shrinks and caps independently`() {
        assertEquals(42f, debugToolSelectionListHeight(0))
        assertEquals(56f, debugToolSelectionListHeight(1))
        assertEquals(227f, debugToolSelectionListHeight(4))
        assertEquals(228f, debugToolSelectionListHeight(40))
        assertFailsWith<IllegalArgumentException> { debugToolSelectionListHeight(-1) }
    }

    @Test
    fun `hovered selection list receives mouse wheel scrolling`() {
        val skin = DebugPanelSkin.create()
        val values = (1..10).toList()
        val list = DebugToolSelectionList(
            items = values.map {
                DebugToolSelectionItem(it, "Entry $it", texture)
            },
            selectionGroup = StrataSelectionGroup(values),
            skin = skin,
            searchHint = "Search..."
        )
        val stage = Stage()
        stage.addActor(list)
        list.setBounds(0f, 0f, 300f, 266f)
        list.validate()
        val scroll = list.children.first { it is ScrollPane } as ScrollPane
        scroll.validate()
        val point = scroll.localToStageCoordinates(
            com.badlogic.gdx.math.Vector2(10f, 10f)
        )
        val target = checkNotNull(stage.hit(point.x, point.y, true))
        target.fire(InputEvent().apply {
            type = InputEvent.Type.enter
            this.stage = stage
            stageX = point.x
            stageY = point.y
            pointer = -1
        })

        assertSame(scroll, stage.scrollFocus)
        val before = scroll.scrollY
        val screen = stage.stageToScreenCoordinates(point.cpy())
        stage.mouseMoved(screen.x.toInt(), screen.y.toInt())
        assertTrue(stage.scrolled(0f, 1f))
        assertNotEquals(before, scroll.scrollY)

        stage.dispose()
        skin.dispose()
    }
}
