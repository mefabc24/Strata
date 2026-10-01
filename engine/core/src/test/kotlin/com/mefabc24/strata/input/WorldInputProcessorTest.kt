package com.mefabc24.strata.input

import com.badlogic.gdx.Input
import com.mefabc24.strata.world.TilePosition
import com.mefabc24.strata.world.Entity
import com.mefabc24.strata.world.EntityPosition
import com.mefabc24.strata.world.World
import com.mefabc24.strata.world.Tile
import com.mefabc24.strata.world.Footprint
import com.mefabc24.strata.world.Placeable
import com.mefabc24.strata.iso.ObjectPickingMode
import com.mefabc24.strata.testing.TestGdxEnvironment
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WorldInputProcessorTest {

    @Test
    fun `grid binding dispatches logical coordinates outside the world`() {
        var dispatched: TilePosition? = null
        val processor = WorldInputProcessor(
            bindings = listOf(
                WorldInputBinding.Grid(
                    trigger = WorldInputTrigger.MouseDown(Input.Buttons.LEFT)
                ) { x, y ->
                    dispatched = TilePosition(x, y)
                    true
                }
            ),
            pickTile = { _, _ -> null },
            pickObject = { _, _, _ -> null },
            pickGrid = { _, _ -> TilePosition(-2, 14) }
        )

        assertTrue(processor.touchDown(4, 5, 0, Input.Buttons.LEFT))
        assertEquals(TilePosition(-2, 14), dispatched)
    }

    @Test
    fun `tile binding does not dispatch a logical coordinate outside world`() {
        var dispatched = false
        val processor = WorldInputProcessor(
            bindings = listOf(
                WorldInputBinding.Tile(
                    trigger = WorldInputTrigger.MouseDown(Input.Buttons.LEFT)
                ) { _, _ ->
                    dispatched = true
                    true
                }
            ),
            pickTile = { _, _ -> null },
            pickObject = { _, _, _ -> null },
            pickGrid = { _, _ -> TilePosition(52, 7) }
        )

        assertFalse(processor.touchDown(4, 5, 0, Input.Buttons.LEFT))
        assertFalse(dispatched)
    }

    @Test
    fun `matching grid bindings share one pick`() {
        var picks = 0
        val processor = WorldInputProcessor(
            bindings = listOf(
                WorldInputBinding.Grid(
                    trigger = WorldInputTrigger.MouseDown(Input.Buttons.LEFT)
                ) { _, _ -> false },
                WorldInputBinding.Grid(
                    trigger = WorldInputTrigger.MouseDown(Input.Buttons.LEFT)
                ) { _, _ -> true }
            ),
            pickTile = { _, _ -> null },
            pickObject = { _, _, _ -> null },
            pickGrid = { _, _ ->
                picks++
                TilePosition(2, 3)
            }
        )

        assertTrue(processor.touchDown(4, 5, 0, Input.Buttons.LEFT))
        assertEquals(1, picks)
    }

    @Test
    fun `pointer observer can inspect a click without consuming later bindings`() {
        val events = mutableListOf<String>()
        val processor = WorldInputProcessor(
            bindings = listOf(
                WorldInputBinding.Pointer(
                    trigger = WorldInputTrigger.MouseDown(Input.Buttons.LEFT)
                ) { x, y ->
                    events += "pointer:$x,$y"
                    false
                },
                WorldInputBinding.Tile(
                    trigger = WorldInputTrigger.MouseDown(Input.Buttons.LEFT)
                ) { x, y ->
                    events += "tile:$x,$y"
                    true
                }
            ),
            pickTile = { _, _ -> TilePosition(2, 3) },
            pickObject = { _, _, _ -> null }
        )

        assertTrue(processor.touchDown(10, 20, 0, Input.Buttons.LEFT))
        assertEquals(listOf("pointer:10.0,20.0", "tile:2,3"), events)
    }

    @Test
    fun `entity binding dispatches the picked runtime entity`() {
        val world = World(1, 1) { _, _ -> TestTile }
        val entity = world.addEntity(TestEntity, EntityPosition(0.5f, 0.5f))
        var dispatched = false
        val processor = WorldInputProcessor(
            bindings = listOf(
                WorldInputBinding.Entity(
                    trigger = WorldInputTrigger.MouseDown(Input.Buttons.LEFT)
                ) { picked ->
                    dispatched = picked === entity
                    true
                }
            ),
            pickTile = { _, _ -> null },
            pickObject = { _, _, _ -> null },
            pickEntity = { _, _, _ -> entity }
        )

        assertTrue(processor.touchDown(4, 5, 0, Input.Buttons.LEFT))
        assertTrue(dispatched)
    }

    @Test
    fun `mouse down and drag use the same tile picker`() {
        val pickedScreens = mutableListOf<Pair<Float, Float>>()
        val dispatchedTiles = mutableListOf<Pair<Int, Int>>()
        val processor = WorldInputProcessor(
            bindings = listOf(
                WorldInputBinding.Tile(
                    trigger = WorldInputTrigger.MouseDown(
                        Input.Buttons.LEFT
                    )
                ) { x, y ->
                    dispatchedTiles += x to y
                    true
                },
                WorldInputBinding.Tile(
                    trigger = WorldInputTrigger.MouseDrag(
                        Input.Buttons.LEFT
                    )
                ) { x, y ->
                    dispatchedTiles += x to y
                    true
                }
            ),
            pickTile = { x, y ->
                pickedScreens += x to y
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
            listOf(2 to 3, 4 to 5),
            dispatchedTiles
        )
    }

    @Test
    fun `tile binding uses the configured flat tile picker`() {
        var picks = 0
        val processor = WorldInputProcessor(
            bindings = listOf(
                WorldInputBinding.Tile(
                    trigger = WorldInputTrigger.MouseDown(
                        Input.Buttons.LEFT
                    )
                ) { _, _ -> true }
            ),
            pickTile = { _, _ ->
                picks++
                TilePosition(0, 0)
            },
            pickObject = { _, _, _ -> null }
        )

        processor.touchDown(0, 0, 0, Input.Buttons.LEFT)

        assertEquals(1, picks)
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
            pickTile = { _, _ -> null },
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
            pickTile = { _, _ -> null },
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

    @Test
    fun `key binding dispatches only matching enabled keys`() {
        TestGdxEnvironment.install()
        val calls = mutableListOf<Int>()
        var bindingEnabled = false
        val processor = WorldInputProcessor(
            bindings = listOf(
                WorldInputBinding.NoPicking(
                    trigger = WorldInputTrigger.KeyDown(Input.Keys.SPACE),
                    enabled = { bindingEnabled }
                ) {
                    calls += Input.Keys.SPACE
                    true
                }
            ),
            pickTile = { _, _ -> null },
            pickObject = { _, _, _ -> null }
        )

        assertFalse(processor.keyDown(Input.Keys.ENTER))
        assertFalse(processor.keyDown(Input.Keys.SPACE))
        bindingEnabled = true
        assertTrue(processor.keyDown(Input.Keys.SPACE))
        processor.enabled = false
        assertFalse(processor.keyDown(Input.Keys.SPACE))

        assertEquals(listOf(Input.Keys.SPACE), calls)
    }

    @Test
    fun `consuming binding prevents later matching actions`() {
        val calls = mutableListOf<String>()
        val trigger = WorldInputTrigger.MouseDown(Input.Buttons.LEFT)
        val processor = WorldInputProcessor(
            bindings = listOf(
                WorldInputBinding.NoPicking(trigger) { calls += "first"; true },
                WorldInputBinding.NoPicking(trigger) { calls += "second"; true }
            ),
            pickTile = { _, _ -> null },
            pickObject = { _, _, _ -> null }
        )

        assertTrue(processor.touchDown(0, 0, 0, Input.Buttons.LEFT))
        assertEquals(listOf("first"), calls)
    }

    @Test
    fun `object binding forwards picking mode and picked identity`() {
        val world = World(1, 1) { _, _ -> TestTile }
        val placed = requireNotNull(world.place(
            object : Placeable { override val footprint = Footprint.square(1) },
            0,
            0
        ))
        var receivedMode: ObjectPickingMode? = null
        var receivedObject = false
        val processor = WorldInputProcessor(
            bindings = listOf(
                WorldInputBinding.Object(
                    trigger = WorldInputTrigger.MouseDown(Input.Buttons.RIGHT),
                    mode = ObjectPickingMode.FOOTPRINT
                ) {
                    receivedObject = it === placed
                    true
                }
            ),
            pickTile = { _, _ -> null },
            pickObject = { _, _, mode ->
                receivedMode = mode
                placed
            }
        )

        assertTrue(processor.touchDown(2, 3, 0, Input.Buttons.RIGHT))
        assertEquals(ObjectPickingMode.FOOTPRINT, receivedMode)
        assertTrue(receivedObject)
    }

    @Test
    fun `missing picked object leaves binding unhandled`() {
        var actionCalled = false
        val processor = WorldInputProcessor(
            bindings = listOf(
                WorldInputBinding.Object(
                    WorldInputTrigger.MouseDown(Input.Buttons.LEFT)
                ) {
                    actionCalled = true
                    true
                }
            ),
            pickTile = { _, _ -> null },
            pickObject = { _, _, _ -> null }
        )

        assertFalse(processor.touchDown(0, 0, 0, Input.Buttons.LEFT))
        assertFalse(actionCalled)
    }

    @Test
    fun `pressed buttons are isolated by pointer and button`() {
        val drags = mutableListOf<Int>()
        val processor = WorldInputProcessor(
            bindings = listOf(
                WorldInputBinding.NoPicking(
                    WorldInputTrigger.MouseDrag(Input.Buttons.LEFT)
                ) { drags += Input.Buttons.LEFT; true },
                WorldInputBinding.NoPicking(
                    WorldInputTrigger.MouseDrag(Input.Buttons.RIGHT)
                ) { drags += Input.Buttons.RIGHT; true }
            ),
            pickTile = { _, _ -> null },
            pickObject = { _, _, _ -> null }
        )
        processor.touchDown(0, 0, 1, Input.Buttons.LEFT)
        processor.touchDown(0, 0, 2, Input.Buttons.RIGHT)

        assertTrue(processor.touchDragged(0, 0, 1))
        assertTrue(processor.touchDragged(0, 0, 2))
        assertFalse(processor.touchDragged(0, 0, 3))

        assertEquals(listOf(Input.Buttons.LEFT, Input.Buttons.RIGHT), drags)
    }

    private data object TestEntity : Entity
    private data object TestTile : Tile
}
