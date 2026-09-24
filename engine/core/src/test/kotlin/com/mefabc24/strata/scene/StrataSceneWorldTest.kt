package com.mefabc24.strata.scene

import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.graphics.Color
import com.mefabc24.strata.camera.CameraSettings
import com.mefabc24.strata.input.ControlsSettings
import com.mefabc24.strata.input.WorldInputBinding
import com.mefabc24.strata.input.WorldInputTrigger
import com.mefabc24.strata.iso.IsoWorldView
import com.mefabc24.strata.render.PlacementPreview
import com.mefabc24.strata.render.PlacementPreviewStyle
import com.mefabc24.strata.render.RenderStats
import com.mefabc24.strata.render.RenderingSettings
import com.mefabc24.strata.testing.TestGdxEnvironment
import com.mefabc24.strata.world.Footprint
import com.mefabc24.strata.world.Placeable
import com.mefabc24.strata.world.Tile
import com.mefabc24.strata.world.TilePosition
import com.mefabc24.strata.world.World
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class StrataSceneWorldTest {

    private enum class Terrain {
        GRASS
    }

    private enum class SoundCategory {
        EFFECT
    }

    private class TestTile : Tile

    private class TestPlaceable : Placeable {
        override val footprint = Footprint.square(1)
    }

    @BeforeTest
    fun installTestEnvironment() {
        TestGdxEnvironment.install()
    }

    @Test
    fun `world access fails clearly before attachment`() {
        val scene = sceneWith(RecordingViewFactory())

        assertFailsWith<IllegalStateException> {
            scene.world
        }

        assertFailsWith<IllegalStateException> {
            scene.view
        }

        assertFailsWith<IllegalStateException> {
            scene.placement
        }

        scene.dispose()
    }

    @Test
    fun `runtime operations are rejected during scene setup`() {
        val testWorld = world()

        val failure = assertFailsWith<IllegalStateException> {
            sceneWith(RecordingViewFactory()) {
                attachWorld(testWorld) { Terrain.GRASS }
            }
        }

        assertEquals(
            "Scene runtime operations are unavailable during setup.",
            failure.message
        )
    }

    @Test
    fun `world attachment uses frozen scene configuration`() {
        val binding = WorldInputBinding.NoPicking(
            trigger = WorldInputTrigger.KeyDown(1)
        ) {
            true
        }

        val mutableBindings = mutableListOf(binding)
        lateinit var escapedCamera: CameraSettings
        lateinit var escapedRendering: RenderingSettings
        lateinit var escapedControls: ControlsSettings
        val factory = RecordingViewFactory()

        val scene = sceneWith(factory) {
            audio {
                masterVolume = 0.75f
            }

            debug {
                performance {
                    intervalSeconds = 3f
                }
            }

            camera {
                moveSpeed = 123f
                zoomEdgeAllowance = 0.25f
                escapedCamera = this
            }

            rendering {
                tileGeometry.width = 48f
                tileGeometry.height = 24f
                escapedRendering = this
            }

            controls {
                camera.moveUp = 99
                gameplay.bindings = mutableBindings
                escapedControls = this
            }
        }

        escapedCamera.moveSpeed = 999f
        escapedRendering.tileGeometry.width = 999f
        escapedControls.camera.moveUp = 101
        mutableBindings.clear()

        val world = world()
        scene.attachWorld(world) { Terrain.GRASS }

        val spec = factory.spec

        assertSame(world, scene.world)
        assertEquals(0.75f, scene.audio.masterVolume)
        assertEquals(3f, scene.debug.performance.intervalSeconds)
        assertEquals(123f, spec.cameraSettings.moveSpeed)
        assertEquals(0.25f, spec.cameraSettings.zoomEdgeAllowance)
        assertEquals(48f, spec.renderingSettings.tileGeometry.width)
        assertEquals(24f, spec.renderingSettings.tileGeometry.height)
        assertEquals(99, spec.controlsSettings.camera.moveUp)
        assertEquals(listOf(binding), spec.controlsSettings.gameplay.bindings)

        assertFailsWith<IllegalStateException> {
            scene.camera {
                moveSpeed = 1f
            }
        }

        assertFailsWith<IllegalStateException> {
            scene.attachWorld(world()) { Terrain.GRASS }
        }

        assertEquals(1, factory.createCalls)
        scene.dispose()
    }

    @Test
    fun `world view lifecycle is coordinated without a ui`() {
        val inputState = TestGdxEnvironment.install()
        val factory = RecordingViewFactory()
        val scene = sceneWith(factory)

        scene.attachWorld(world()) { Terrain.GRASS }

        assertTrue(inputState.inputProcessor === scene.input.processor)

        factory.view.nextHoveredTile = TilePosition(2, 3)
        scene.update(0.5f)
        scene.render()
        scene.resize(900, 700)

        assertEquals(listOf(0.5f), factory.view.updateDeltas)
        assertEquals(1, factory.view.renderedPreviews.size)
        assertNull(factory.view.renderedPreviews.single())
        assertEquals(listOf(900 to 700), factory.view.resizes)

        scene.dispose()
        scene.dispose()

        assertTrue(factory.view.disposed)
        assertNull(inputState.inputProcessor)
    }

    @Test
    fun `scene placement follows the updated hover and supplies rendering preview`() {
        val previewStyle = PlacementPreviewStyle(
            validColor = Color(0.2f, 0.3f, 0.4f, 0.5f),
            invalidColor = Color(0.8f, 0.7f, 0.6f, 0.5f)
        )

        val factory = RecordingViewFactory()
        val scene = sceneWith(factory) {
            placement {
                this.previewStyle = previewStyle
                validator { _, position ->
                    position.x == 2
                }
            }
        }

        previewStyle.validColor.set(Color.RED)
        scene.attachWorld(world()) { Terrain.GRASS }

        val selected = TestPlaceable()
        scene.placement.selectedPlaceable = selected

        factory.view.nextHoveredTile = TilePosition(1, 3)
        scene.update(0.25f)

        val invalidPreview = assertNotNull(scene.placement.preview)
        assertEquals(1, invalidPreview.placedObject.x)
        assertEquals(3, invalidPreview.placedObject.y)
        assertFalse(invalidPreview.valid)
        assertEquals(Color(0.2f, 0.3f, 0.4f, 0.5f), invalidPreview.style.validColor)
        assertNull(scene.placement.placeAt(1, 3))

        factory.view.nextHoveredTile = TilePosition(2, 3)
        scene.update(0.25f)

        val validPreview = assertNotNull(scene.placement.preview)
        assertTrue(validPreview.valid)

        scene.render()
        assertSame(validPreview, factory.view.renderedPreviews.last())

        scene.placement.enabled = false

        assertNull(scene.placement.preview)
        assertSame(selected, scene.placement.selectedPlaceable)
        assertNull(scene.placement.placeAt(2, 3))

        scene.render()
        assertNull(factory.view.renderedPreviews.last())

        scene.placement.enabled = true
        scene.update(0.25f)
        assertNotNull(scene.placement.preview)
        assertNotNull(scene.placement.placeAt(2, 3))

        scene.dispose()
    }

    @Test
    fun `world without placement keeps placement unavailable`() {
        val scene = sceneWith(RecordingViewFactory())
        scene.attachWorld(world()) { Terrain.GRASS }

        assertFailsWith<IllegalStateException> {
            scene.placement
        }

        scene.dispose()
    }

    private fun sceneWith(
        viewFactory: RecordingViewFactory,
        configure: StrataScene<Terrain, SoundCategory>.() -> Unit = {}
    ): StrataScene<Terrain, SoundCategory> {
        return StrataScene(
            terrainDirectory = "",
            objectDirectory = "",
            uiFactory = StrataUiFactory { _, _ ->
                error("UI creation was not expected in this test.")
            },
            worldViewFactory = viewFactory,
            configure = configure
        )
    }

    private fun world(): World {
        return World(5, 5) { _, _ ->
            TestTile()
        }
    }

    private class RecordingViewFactory : SceneWorldViewFactory {
        lateinit var spec: SceneWorldViewSpec
            private set

        val view = RecordingWorldView()
        var createCalls = 0
            private set

        override fun create(spec: SceneWorldViewSpec): SceneWorldView {
            createCalls++
            this.spec = spec
            return view
        }
    }

    private class RecordingWorldView : SceneWorldView {
        override val publicView: IsoWorldView? = null
        override val inputProcessor: InputProcessor = InputAdapter()
        override val renderStats = RenderStats()

        override var hoveredTile: TilePosition? = null
            private set

        var nextHoveredTile: TilePosition? = null
        val updateDeltas = mutableListOf<Float>()
        val renderedPreviews = mutableListOf<PlacementPreview?>()
        val resizes = mutableListOf<Pair<Int, Int>>()
        var disposed = false
            private set

        override fun update(delta: Float) {
            updateDeltas += delta
            hoveredTile = nextHoveredTile
        }

        override fun render(
            raisedTile: TilePosition?,
            raiseOffsetY: Float,
            preview: PlacementPreview?
        ) {
            renderedPreviews += preview
        }

        override fun resize(width: Int, height: Int) {
            resizes += width to height
        }

        override fun dispose() {
            disposed = true
        }
    }
}
