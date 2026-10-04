package com.mefabc24.strata.ui

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.mefabc24.strata.testing.TestGdxEnvironment
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class StrataScrollFocusTest {
    @BeforeTest
    fun installTestEnvironment() {
        TestGdxEnvironment.install()
    }

    @Test
    fun `nested pane keeps focus when enter event bubbles to outer pane`() {
        val fixture = nestedPanes()
        try {
            fixture.fireHover(InputEvent.Type.enter, fixture.innerPoint, null)

            assertSame(fixture.inner, fixture.stage.scrollFocus)
        } finally {
            fixture.stage.dispose()
        }
    }

    @Test
    fun `leaving inner pane restores outer pane then leaving ui clears focus`() {
        val fixture = nestedPanes()
        try {
            val innerHit = fixture.hit(fixture.innerPoint)
            val outerHit = fixture.hit(fixture.outerPoint)
            fixture.fireHover(InputEvent.Type.enter, fixture.innerPoint, null)
            fixture.fireHover(
                InputEvent.Type.exit,
                fixture.outerPoint,
                outerHit,
                innerHit
            )

            assertSame(fixture.outer, fixture.stage.scrollFocus)

            fixture.fireHover(
                InputEvent.Type.exit,
                fixture.outsidePoint,
                fixture.stage.root,
                outerHit
            )

            assertNull(fixture.stage.scrollFocus)
        } finally {
            fixture.stage.dispose()
        }
    }

    @Test
    fun `wheel event repairs stale outer focus before scrolling`() {
        val fixture = nestedPanes()
        try {
            fixture.fireHover(InputEvent.Type.enter, fixture.innerPoint, null)
            fixture.stage.setScrollFocus(fixture.outer)
            fixture.inner.scrollY = 0f
            val outerBefore = fixture.outer.scrollY

            val event = InputEvent().apply {
                type = InputEvent.Type.scrolled
                stage = fixture.stage
                stageX = fixture.innerPoint.x
                stageY = fixture.innerPoint.y
                scrollAmountY = 1f
            }
            fixture.outer.fire(event)

            assertTrue(event.isHandled)
            assertSame(fixture.inner, fixture.stage.scrollFocus)
            assertTrue(fixture.inner.scrollY > 0f)
            assertTrue(fixture.outer.scrollY == outerBefore)
        } finally {
            fixture.stage.dispose()
        }
    }

    @Test
    fun `wheel event returns scrolling to outer pane after pointer leaves inner pane`() {
        val fixture = nestedPanes()
        try {
            fixture.fireHover(InputEvent.Type.enter, fixture.innerPoint, null)
            fixture.outer.scrollY = 0f
            val innerBefore = fixture.inner.scrollY

            val event = fixture.scrollEvent(fixture.outerPoint)
            fixture.inner.fire(event)

            assertTrue(event.isHandled)
            assertSame(fixture.outer, fixture.stage.scrollFocus)
            assertTrue(fixture.outer.scrollY > 0f)
            assertTrue(fixture.inner.scrollY == innerBefore)
        } finally {
            fixture.stage.dispose()
        }
    }

    @Test
    fun `hover policy leaves unregistered scroll panes in control`() {
        val fixture = nestedPanes()
        try {
            fixture.fireHover(InputEvent.Type.enter, fixture.innerPoint, null)
            val content = Table().apply {
                add(Actor()).size(100f, 400f)
            }
            val independent = ScrollPane(content, ScrollPane.ScrollPaneStyle()).apply {
                setScrollingDisabled(true, false)
                setBounds(300f, 20f, 120f, 100f)
            }
            fixture.stage.addActor(independent)
            independent.validate()
            val point = independent.localToStageCoordinates(Vector2(20f, 20f))
            fixture.stage.setScrollFocus(independent)

            val event = fixture.scrollEvent(point)
            independent.fire(event)

            assertTrue(event.isHandled)
            assertSame(independent, fixture.stage.scrollFocus)
            assertTrue(independent.scrollY > 0f)
        } finally {
            fixture.stage.dispose()
        }
    }

    private fun nestedPanes(): NestedPaneFixture {
        val style = ScrollPane.ScrollPaneStyle()
        val innerContent = Table().apply {
            add(Actor()).size(100f, 200f)
            row()
            add(Actor()).size(100f, 200f)
        }
        val inner = ScrollPane(innerContent, style).apply {
            setScrollingDisabled(true, false)
            setOverscroll(false, false)
            useHoverScrollFocus()
        }
        val outerContent = Table().apply {
            add(inner).size(120f, 100f)
            row()
            add(Actor()).size(120f, 300f)
        }
        val outer = ScrollPane(outerContent, style).apply {
            setScrollingDisabled(true, false)
            setOverscroll(false, false)
            useHoverScrollFocus()
            setBounds(20f, 20f, 220f, 220f)
        }
        val stage = Stage()
        stage.addActor(outer)
        outer.validate()
        inner.validate()

        val innerPoint = inner.localToStageCoordinates(
            Vector2(inner.width / 2f, inner.height / 2f)
        )
        val outerPoint = outer.localToStageCoordinates(Vector2(210f, 210f))
        val outsidePoint = Vector2(400f, 400f)
        val fixture = NestedPaneFixture(
            stage,
            outer,
            inner,
            innerPoint,
            outerPoint,
            outsidePoint
        )
        assertTrue(fixture.hit(innerPoint).isInside(inner))
        assertTrue(!fixture.hit(outerPoint).isInside(inner))
        return fixture
    }

    private data class NestedPaneFixture(
        val stage: Stage,
        val outer: ScrollPane,
        val inner: ScrollPane,
        val innerPoint: Vector2,
        val outerPoint: Vector2,
        val outsidePoint: Vector2
    ) {
        fun hit(point: Vector2): Actor = checkNotNull(
            stage.hit(point.x, point.y, true)
        )

        fun fireHover(
            type: InputEvent.Type,
            point: Vector2,
            related: Actor?,
            eventTarget: Actor? = null
        ) {
            val target = eventTarget ?: stage.hit(point.x, point.y, true) ?: stage.root
            target.fire(InputEvent().apply {
                this.type = type
                this.stage = this@NestedPaneFixture.stage
                stageX = point.x
                stageY = point.y
                pointer = -1
                relatedActor = related
            })
        }

        fun scrollEvent(point: Vector2) = InputEvent().apply {
            type = InputEvent.Type.scrolled
            stage = this@NestedPaneFixture.stage
            stageX = point.x
            stageY = point.y
            scrollAmountY = 1f
        }
    }

    private fun Actor.isInside(ancestor: Actor): Boolean =
        this === ancestor || isDescendantOf(ancestor)
}
