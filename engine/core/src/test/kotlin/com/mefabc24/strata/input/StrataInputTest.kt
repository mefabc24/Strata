package com.mefabc24.strata.input

import com.badlogic.gdx.InputAdapter
import com.mefabc24.strata.testing.TestGdxEnvironment
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class StrataInputTest {

    @Test
    fun `ui input works without a world processor`() {
        val calls = mutableListOf<String>()
        val input = StrataInput()

        input.addUiProcessor(
            processor("ui", calls, handled = true)
        )

        assertTrue(input.processor.touchDown(0, 0, 0, 0))
        assertEquals(listOf("ui"), calls)
    }

    @Test
    fun `empty router safely leaves input unhandled`() {
        val input = StrataInput()

        assertFalse(input.processor.touchDown(0, 0, 0, 0))
    }

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

    @Test
    fun `world processor attached after ui retains lower priority`() {
        val calls = mutableListOf<String>()
        val input = StrataInput()

        input.addUiProcessor(
            processor("ui", calls, handled = false)
        )
        input.setWorldProcessor(
            processor("world", calls, handled = true)
        )

        assertTrue(input.processor.touchDown(0, 0, 0, 0))
        assertEquals(listOf("ui", "world"), calls)
    }

    @Test
    fun `only one world processor can be registered`() {
        val input = StrataInput()
        val first = InputAdapter()

        input.setWorldProcessor(first)

        assertFailsWith<IllegalStateException> {
            input.setWorldProcessor(InputAdapter())
        }

        assertFalse(input.removeWorldProcessor(InputAdapter()))
        assertTrue(input.removeWorldProcessor(first))
        input.setWorldProcessor(InputAdapter())
    }

    @Test
    fun `installation and removal only change the owned global processor`() {
        val inputState = TestGdxEnvironment.install()
        val input = StrataInput()

        input.install()
        assertTrue(inputState.inputProcessor === input.processor)

        val replacement = InputAdapter()
        inputState.input.inputProcessor = replacement
        input.uninstall()
        assertTrue(inputState.inputProcessor === replacement)

        input.install()
        input.uninstall()
        assertEquals(null, inputState.inputProcessor)
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
