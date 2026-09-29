package com.mefabc24.strata.scene

import com.mefabc24.strata.debug.DebugEntitySettings
import com.mefabc24.strata.debug.DebugGridSettings
import com.mefabc24.strata.debug.DebugObjectSettings

import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.graphics.Color
import com.mefabc24.strata.audio.SoundId
import com.mefabc24.strata.audio.SoundCategoryId
import com.mefabc24.strata.camera.CameraSettings
import com.mefabc24.strata.input.ControlsSettings
import com.mefabc24.strata.input.WorldInputBinding
import com.mefabc24.strata.input.WorldInputTrigger
import com.mefabc24.strata.iso.IsoWorldView
import com.mefabc24.strata.render.preview.PlacementPreview
import com.mefabc24.strata.render.RenderStats
import com.mefabc24.strata.render.RenderingSettings
import com.mefabc24.strata.terrain.TerrainId
import com.mefabc24.strata.testing.TestGdxEnvironment
import com.mefabc24.strata.world.Footprint
import com.mefabc24.strata.world.Entity
import com.mefabc24.strata.world.EntityPosition
import com.mefabc24.strata.world.Placeable
import com.mefabc24.strata.world.Tile
import com.mefabc24.strata.world.TilePosition
import com.mefabc24.strata.world.World
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class StrataSceneWorldTest {

    private enum class Terrain : TerrainId {
        GRASS,
        WATER
    }

    private enum class SoundCategory : SoundCategoryId {
        EFFECT
    }

    private enum class TestSound : SoundId {
        PLACE,
        REMOVE
    }

    private class TestTile : Tile

    private class TestPlaceable : Placeable {
        override val footprint = Footprint.square(1)
    }

    private class OtherPlaceable : Placeable {
        override val footprint = Footprint.square(1)
    }

    private class TestEntity : Entity

    private class OtherEntity : Entity

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
    fun `scene closes content registration after preparing setup entries`() {
        val scene = sceneWith(RecordingViewFactory()) {
            registrations {
                terrain {
                    register(
                        type = Terrain.GRASS,
                        sprite = TEST_TEXTURE
                    )
                }

                objects {
                    register<TestPlaceable>(
                        sprite = TEST_TEXTURE,
                        factory = ::TestPlaceable
                    )
                }

                entities {
                    register<TestEntity>(TEST_TEXTURE)
                }

                sounds {
                    register(
                        id = TestSound.PLACE,
                        path = "test.wav",
                        category = SoundCategory.EFFECT
                    )
                }
            }
        }

        assertEquals(
            listOf(Terrain.GRASS),
            scene.terrain.entries.map { it.type }
        )
        assertTrue(scene.terrain.entries.single().isPrepared)
        assertTrue(scene.objects.entries.single().isPrepared)
        assertTrue(scene.entities.entries.single().isPrepared)
        assertIs<TestPlaceable>(
            scene.objects.constructibleEntries.single().create()
        )
        assertEquals("test.wav", scene.sounds[TestSound.PLACE].path)

        val terrainFailure = assertFailsWith<IllegalStateException> {
            scene.terrain.register(Terrain.WATER, TEST_TEXTURE)
        }
        val objectFailure = assertFailsWith<IllegalStateException> {
            scene.objects.register<OtherPlaceable>(TEST_TEXTURE)
        }
        val soundFailure = assertFailsWith<IllegalStateException> {
            scene.sounds.register(
                id = TestSound.REMOVE,
                path = "other.wav",
                category = SoundCategory.EFFECT
            )
        }
        val entityFailure = assertFailsWith<IllegalStateException> {
            scene.entities.register<OtherEntity>(TEST_TEXTURE)
        }

        assertEquals(
            "Terrain registry registration is already closed.",
            terrainFailure.message
        )
        assertEquals(
            "Object registry registration is already closed.",
            objectFailure.message
        )
        assertEquals(
            "Sound registry registration is already closed.",
            soundFailure.message
        )
        assertEquals(
            "Entity registry registration is already closed.",
            entityFailure.message
        )

        scene.dispose()
    }

    @Test
    fun `world attachment freezes setup except runtime debug grid`() {
        val binding = WorldInputBinding.NoPicking(
            trigger = WorldInputTrigger.KeyDown(1)
        ) {
            true
        }

        val mutableBindings = mutableListOf(binding)
        lateinit var escapedCamera: CameraSettings
        lateinit var escapedRendering: RenderingSettings
        lateinit var escapedControls: ControlsSettings
        lateinit var escapedGrid: DebugGridSettings
        lateinit var escapedObjects: DebugObjectSettings
        lateinit var escapedEntities: DebugEntitySettings
        val factory = RecordingViewFactory()

        val scene = sceneWith(factory) {
            audio {
                masterVolume = 0.75f
            }

            debug {
                performance {
                    intervalSeconds = 3f
                }

                grid {
                    enabled = true
                    escapedGrid = this
                }

                objects {
                    escapedObjects = this
                }

                entities {
                    escapedEntities = this
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
                objects {
                    offsetX = 2f
                    offsetY = -3f
                }
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
        escapedRendering.objects.offsetX = 999f
        escapedRendering.objects.offsetY = 999f
        escapedControls.camera.moveUp = 101
        escapedGrid.enabled = false
        escapedObjects.enabled = true
        escapedEntities.showPath = false
        mutableBindings.clear()

        val world = world()
        scene.attachWorld(world) { Terrain.GRASS }

        val spec = factory.spec

        assertSame(world, scene.world)
        assertEquals(0.75f, scene.audio.masterVolume)
        assertEquals(3f, scene.debug.performance.intervalSeconds)
        assertSame(scene.debug.grid, spec.debugGridSettings)
        assertSame(scene.debug.objects, spec.debugObjectSettings)
        assertSame(scene.debug.entities, spec.debugEntitySettings)
        assertFalse(spec.debugGridSettings.enabled)
        assertTrue(spec.debugObjectSettings.enabled)
        assertFalse(spec.debugEntitySettings.showPath)
        assertEquals(123f, spec.cameraSettings.moveSpeed)
        assertEquals(0.25f, spec.cameraSettings.zoomEdgeAllowance)
        assertEquals(48f, spec.renderingSettings.tileGeometry.width)
        assertEquals(24f, spec.renderingSettings.tileGeometry.height)
        assertEquals(2f, spec.renderingSettings.objects.offsetX)
        assertEquals(-3f, spec.renderingSettings.objects.offsetY)
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
    fun `scene lighting configuration remains runtime mutable`() {
        val factory = RecordingViewFactory()
        val scene = sceneWith(factory) {
            lighting {
                enabled = true
                ambientIntensity = 0.4f
                ambientColor = Color(0.5f, 0.6f, 0.8f, 1f)
                addPointLight(
                    position = EntityPosition(2.5f, 3.5f),
                    radius = 4f,
                    intensity = 1f,
                    color = Color.ORANGE
                )
            }
        }

        scene.attachWorld(world()) { Terrain.GRASS }

        assertSame(scene.lighting, factory.spec.lighting)
        assertTrue(scene.lighting.enabled)
        assertEquals(0.4f, scene.lighting.ambientIntensity)
        assertEquals(1, scene.lighting.pointLights.size)

        scene.lighting.ambientIntensity = 0.2f
        scene.lighting.pointLights.single().radius = 6f

        assertEquals(0.2f, factory.spec.lighting.ambientIntensity)
        assertEquals(6f, factory.spec.lighting.pointLights.single().radius)
        assertFailsWith<IllegalStateException> {
            scene.lighting {
                enabled = false
            }
        }

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

        assertEquals(listOf(0.5f to 0.5f), factory.view.updateDeltas)
        assertEquals(1, factory.view.renderedPreviews.size)
        assertTrue(factory.view.renderedPreviews.single().isEmpty())
        assertEquals(listOf(900 to 700), factory.view.resizes)

        scene.dispose()
        scene.dispose()

        assertTrue(factory.view.disposed)
        assertNull(inputState.inputProcessor)
    }

    @Test
    fun `scene update advances entity movement automatically`() {
        val factory = RecordingViewFactory()
        val scene = sceneWith(factory)
        val world = world()
        val entity = world.addEntity(TestEntity(), EntityPosition(0.5f, 0.5f))
        entity.followPath(listOf(TilePosition(1, 0)), speed = 2f)
        scene.attachWorld(world) { Terrain.GRASS }

        scene.update(
            realDelta = 0.25f,
            simulationDelta = 0f
        )

        assertEquals(EntityPosition(0.5f, 0.5f), entity.position)
        assertTrue(entity.isMoving)

        scene.update(
            realDelta = 0.125f,
            simulationDelta = 0.25f
        )

        assertEquals(EntityPosition(1f, 0.5f), entity.position)
        assertEquals(
            listOf(0.25f to 0f, 0.125f to 0.25f),
            factory.view.updateDeltas
        )
        scene.dispose()
    }

    @Test
    fun `scene placement supplies the complete preview collection`() {
        val validColor = Color(0.2f, 0.3f, 0.4f, 0.5f)

        val factory = RecordingViewFactory()
        val scene = sceneWith(factory) {
            placement {
                preview {
                    objects {
                        this.validColor = validColor
                        invalidColor = Color(0.8f, 0.7f, 0.6f, 0.5f)
                    }
                }
                validator { _, position ->
                    position.x == 2
                }
            }
        }

        validColor.set(Color.RED)
        scene.attachWorld(world()) { Terrain.GRASS }

        val selectedFactory = ::TestPlaceable
        scene.placement.selectedFactory = selectedFactory

        factory.view.nextHoveredTile = TilePosition(1, 3)
        scene.update(0.25f)

        val invalidPreview = scene.placement.previews.single()
        assertEquals(1, invalidPreview.placedObject.x)
        assertEquals(3, invalidPreview.placedObject.y)
        assertFalse(invalidPreview.valid)
        assertEquals(Color(0.2f, 0.3f, 0.4f, 0.5f), invalidPreview.style.validColor)
        assertNull(scene.placement.placeAt(1, 3))

        factory.view.nextHoveredTile = TilePosition(2, 3)
        scene.update(0.25f)

        val validPreview = scene.placement.previews.single()
        assertTrue(validPreview.valid)

        scene.placement.previewAt(
            listOf(TilePosition(2, 1), TilePosition(2, 3))
        )
        val explicitPreviews = scene.placement.previews

        scene.render()
        assertEquals(2, explicitPreviews.size)
        assertSame(explicitPreviews, factory.view.renderedPreviews.last())

        scene.placement.enabled = false

        assertTrue(scene.placement.previews.isEmpty())
        assertSame(
            selectedFactory,
            scene.placement.selectedFactory
        )
        assertNotNull(scene.placement.selectedPlaceable)
        assertNull(scene.placement.placeAt(2, 3))

        scene.render()
        assertTrue(factory.view.renderedPreviews.last().isEmpty())

        scene.placement.enabled = true
        scene.update(0.25f)
        assertEquals(1, scene.placement.previews.size)
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
        configure: StrataScene.() -> Unit = {}
    ): StrataScene {
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
        val updateDeltas = mutableListOf<Pair<Float, Float>>()
        val renderedPreviews = mutableListOf<List<PlacementPreview>>()
        val resizes = mutableListOf<Pair<Int, Int>>()
        var disposed = false
            private set

        override fun update(
            realDelta: Float,
            simulationDelta: Float
        ) {
            updateDeltas += realDelta to simulationDelta
            hoveredTile = nextHoveredTile
        }

        override fun render(previews: List<PlacementPreview>) {
            renderedPreviews += previews
        }

        override fun resize(width: Int, height: Int) {
            resizes += width to height
        }

        override fun dispose() {
            disposed = true
        }
    }

    private companion object {
        const val TEST_TEXTURE = "com/badlogic/gdx/utils/lsans-15.png"
    }
}
