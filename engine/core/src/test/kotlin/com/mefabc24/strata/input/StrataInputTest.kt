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
    fun `debug tiers route before game ui and world`() {
        val calls = mutableListOf<String>()
        val input = StrataInput()
        input.addDebugUiProcessor(processor("debug-ui", calls, false))
        input.addUiProcessor(processor("game-ui", calls, false))
        input.setDebugWorldProcessor(processor("debug-tool", calls, false))
        input.setWorldProcessor(processor("game-world", calls, true))
        assertTrue(input.processor.touchDown(0, 0, 0, 0))
        assertEquals(
            listOf("debug-ui", "game-ui", "debug-tool", "game-world"),
            calls
        )
    }

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

    @Test
    fun `screen processors route after debug ui and before compatibility ui`() {
        val calls = mutableListOf<String>()
        val input = StrataInput(processor("world", calls, handled = true))
        input.addDebugUiProcessor(processor("debug", calls, handled = false))
        input.setScreenUiProcessors(
            listOf(
                processor("top-screen", calls, handled = false),
                processor("base-screen", calls, handled = false)
            )
        )
        input.addUiProcessor(processor("compatibility", calls, handled = false))

        assertTrue(input.processor.touchDown(0, 0, 0, 0))

        assertEquals(
            listOf("debug", "top-screen", "base-screen", "compatibility", "world"),
            calls
        )
    }

    @Test
    fun `screen processor list rejects identity duplicates`() {
        val input = StrataInput()
        val processor = InputAdapter()

        assertFailsWith<IllegalStateException> {
            input.setScreenUiProcessors(listOf(processor, InputAdapter(), processor))
        }
    }

    @Test
    fun `debug ui processors reject duplicates and can be removed`() {
        val calls = mutableListOf<String>()
        val input = StrataInput(processor("world", calls, handled = true))
        val debug = processor("debug", calls, handled = true)
        input.addDebugUiProcessor(debug)

        assertFailsWith<IllegalStateException> { input.addDebugUiProcessor(debug) }
        assertTrue(input.removeDebugUiProcessor(debug))
        assertFalse(input.removeDebugUiProcessor(debug))
        assertTrue(input.processor.touchDown(0, 0, 0, 0))

        assertEquals(listOf("world"), calls)
    }

    @Test
    fun `debug world processor has single ownership and identity removal`() {
        val input = StrataInput()
        val debugTool = InputAdapter()
        input.setDebugWorldProcessor(debugTool)

        assertFailsWith<IllegalStateException> {
            input.setDebugWorldProcessor(InputAdapter())
        }
        assertFalse(input.removeDebugWorldProcessor(InputAdapter()))
        assertTrue(input.removeDebugWorldProcessor(debugTool))
        input.setDebugWorldProcessor(InputAdapter())
    }

    @Test
    fun `world replacement immediately changes the routed target`() {
        val calls = mutableListOf<String>()
        val input = StrataInput(processor("first", calls, handled = true))

        input.replaceWorldProcessor(processor("second", calls, handled = true))
        assertTrue(input.processor.touchDown(0, 0, 0, 0))
        input.replaceWorldProcessor(null)
        assertFalse(input.processor.touchDown(0, 0, 0, 0))

        assertEquals(listOf("second"), calls)
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
