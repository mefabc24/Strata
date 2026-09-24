package com.mefabc24.strata.input

import com.badlogic.gdx.Input
import com.mefabc24.strata.iso.TilePickingMode
import com.mefabc24.strata.world.TilePosition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WorldInputProcessorTest {

    @Test
    fun `mouse down and drag use the same tile picker`() {
        val pickedScreens = mutableListOf<Pair<Float, Float>>()
        val pickedModes = mutableListOf<TilePickingMode>()
        val dispatchedTiles = mutableListOf<Pair<Int, Int>>()
        val processor = WorldInputProcessor(
            bindings = listOf(
                WorldInputBinding.Tile(
                    trigger = WorldInputTrigger.MouseDown(
                        Input.Buttons.LEFT
                    ),
                    mode = TilePickingMode.BASE_GRID
                ) { x, y ->
                    dispatchedTiles += x to y
                    true
                },
                WorldInputBinding.Tile(
                    trigger = WorldInputTrigger.MouseDrag(
                        Input.Buttons.LEFT
                    ),
                    mode = TilePickingMode.BASE_GRID
                ) { x, y ->
                    dispatchedTiles += x to y
                    true
                }
            ),
            pickTile = { x, y, mode ->
                pickedScreens += x to y
                pickedModes += mode
                TilePosition(x.toInt(), y.toInt())
            },
            pickObject = { _, _, _ -> null }
        )

        assertTrue(
            processor.touchDown(
                screenX = 2,
                screenY = 3,
                pointer = 0,
                button = Input.Buttons.LEFT
            )
        )
        assertTrue(
            processor.touchDragged(
                screenX = 4,
                screenY = 5,
                pointer = 0
            )
        )

        assertEquals(
            listOf(2f to 3f, 4f to 5f),
            pickedScreens
        )
        assertEquals(
            listOf(
                TilePickingMode.BASE_GRID,
                TilePickingMode.BASE_GRID
            ),
            pickedModes
        )
        assertEquals(
            listOf(2 to 3, 4 to 5),
            dispatchedTiles
        )
    }

    @Test
    fun `tile bindings use surface picking by default`() {
        var pickedMode: TilePickingMode? = null
        val processor = WorldInputProcessor(
            bindings = listOf(
                WorldInputBinding.Tile(
                    trigger = WorldInputTrigger.MouseDown(
                        Input.Buttons.LEFT
                    )
                ) { _, _ -> true }
            ),
            pickTile = { _, _, mode ->
                pickedMode = mode
                TilePosition(0, 0)
            },
            pickObject = { _, _, _ -> null }
        )

        processor.touchDown(0, 0, 0, Input.Buttons.LEFT)

        assertEquals(TilePickingMode.SURFACE, pickedMode)
    }

    @Test
    fun `mouse up is dispatched even when no tile is picked`() {
        var releases = 0

        val processor = WorldInputProcessor(
            bindings = listOf(
                WorldInputBinding.NoPicking(
                    trigger = WorldInputTrigger.MouseUp(
                        Input.Buttons.LEFT
                    )
                ) {
                    releases++
                    true
                }
            ),
            pickTile = { _, _, _ -> null },
            pickObject = { _, _, _ -> null }
        )

        processor.touchDown(
            screenX = 10,
            screenY = 10,
            pointer = 0,
            button = Input.Buttons.LEFT
        )

        assertTrue(
            processor.touchUp(
                screenX = -100,
                screenY = -100,
                pointer = 0,
                button = Input.Buttons.LEFT
            )
        )

        assertEquals(1, releases)

        // A second release must not trigger another event.
        assertFalse(
            processor.touchUp(
                screenX = -100,
                screenY = -100,
                pointer = 0,
                button = Input.Buttons.LEFT
            )
        )

        assertEquals(1, releases)
    }

    @Test
    fun `disabling input clears pressed mouse buttons`() {
        var releases = 0

        val processor = WorldInputProcessor(
            bindings = listOf(
                WorldInputBinding.NoPicking(
                    trigger = WorldInputTrigger.MouseUp(
                        Input.Buttons.LEFT
                    )
                ) {
                    releases++
                    true
                }
            ),
            pickTile = { _, _, _ -> null },
            pickObject = { _, _, _ -> null }
        )

        processor.touchDown(
            screenX = 10,
            screenY = 10,
            pointer = 0,
            button = Input.Buttons.LEFT
        )

        processor.enabled = false
        processor.enabled = true

        assertFalse(
            processor.touchUp(
                screenX = 20,
                screenY = 20,
                pointer = 0,
                button = Input.Buttons.LEFT
            )
        )

        assertEquals(0, releases)
    }
}
