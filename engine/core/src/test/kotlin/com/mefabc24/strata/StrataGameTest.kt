package com.mefabc24.strata

import com.mefabc24.strata.testing.TestGdxEnvironment
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class StrataGameTest {

    @BeforeTest
    fun installTestEnvironment() {
        TestGdxEnvironment.install()
    }

    @Test
    fun `strata runtime is created during game lifecycle`() {
        val events = mutableListOf<String>()
        val game = TestGame(events)

        assertEquals(
            listOf("subclass initialized"),
            events
        )

        assertFailsWith<IllegalStateException> {
            game.runtime.scene
        }

        game.resize(100, 100)

        assertEquals(
            listOf("subclass initialized"),
            events
        )

        game.create()

        assertEquals(
            listOf(
                "subclass initialized",
                "ready"
            ),
            events
        )

        // The Strata scene now exists.
        game.runtime.scene

        assertFailsWith<IllegalStateException> {
            game.create()
        }

        game.dispose()

        assertFailsWith<IllegalStateException> {
            game.runtime.scene
        }
    }

    @Test
    fun `game delegates lifecycle around strata runtime`() {
        val events = mutableListOf<String>()
        val game = TestGame(events)

        game.create()
        events.clear()

        game.update(0.5f)
        game.render()
        game.resize(900, 700)
        game.dispose()
        game.dispose()

        assertEquals(
            listOf(
                "game update 0.5",
                "game resize 900x700",
                "game dispose"
            ),
            events
        )

        assertFailsWith<IllegalStateException> {
            game.runtime.scene
        }
    }

    private class TestGame(
        private val events: MutableList<String>
    ) : StrataGame() {

        override val strata =
            Strata().configure {
                scene(
                    terrainDirectory = "",
                    objectDirectory = ""
                ) {
                    // No scene content is required for this lifecycle test.
                }
            }

        val runtime: Strata
            get() = strata

        init {
            events += "subclass initialized"
        }

        override fun onReady() {
            events += "ready"
        }

        override fun updateGame(delta: Float) {
            events += "game update $delta"
        }

        override fun resizeGame(
            width: Int,
            height: Int
        ) {
            events += "game resize ${width}x$height"
        }

        override fun disposeGame() {
            events += "game dispose"
        }
    }
}