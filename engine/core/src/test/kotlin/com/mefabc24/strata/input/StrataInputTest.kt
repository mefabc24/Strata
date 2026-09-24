package com.mefabc24.strata.input

import com.badlogic.gdx.InputAdapter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class StrataInputTest {

    @Test
    fun `ui input is routed before world input`() {
        val calls = mutableListOf<String>()

        val world = processor("world", calls, handled = true)
        val ui = processor("ui", calls, handled = true)

        val input = StrataInput(world)
        input.addUiProcessor(ui)

        assertTrue(
            input.processor.touchDown(0, 0, 0, 0)
        )
        assertEquals(listOf("ui"), calls)
    }

    @Test
    fun `unhandled ui input continues to the world`() {
        val calls = mutableListOf<String>()

        val world = processor("world", calls, handled = true)
        val ui = processor("ui", calls, handled = false)

        val input = StrataInput(world)
        input.addUiProcessor(ui)

        assertTrue(
            input.processor.touchDown(0, 0, 0, 0)
        )
        assertEquals(listOf("ui", "world"), calls)
    }

    @Test
    fun `removed ui processor no longer receives input`() {
        val calls = mutableListOf<String>()

        val world = processor("world", calls, handled = true)
        val ui = processor("ui", calls, handled = true)

        val input = StrataInput(world)
        input.addUiProcessor(ui)

        assertTrue(input.removeUiProcessor(ui))
        assertFalse(input.removeUiProcessor(ui))
        assertTrue(
            input.processor.touchDown(0, 0, 0, 0)
        )

        assertEquals(listOf("world"), calls)
    }

    @Test
    fun `same ui processor cannot be registered twice`() {
        val input = StrataInput(InputAdapter())
        val ui = InputAdapter()

        input.addUiProcessor(ui)

        assertFailsWith<IllegalStateException> {
            input.addUiProcessor(ui)
        }
    }

    private fun processor(
        name: String,
        calls: MutableList<String>,
        handled: Boolean
    ) = object : InputAdapter() {
        override fun touchDown(
            screenX: Int,
            screenY: Int,
            pointer: Int,
            button: Int
        ): Boolean {
            calls += name
            return handled
        }
    }
}
