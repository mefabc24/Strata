package com.mefabc24.strata.render

import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.mefabc24.strata.iso.IsoProjection
import com.mefabc24.strata.iso.TileGeometry
import com.mefabc24.strata.render.entity.EntityVisual
import com.mefabc24.strata.render.entity.ResolvedEntityVisual
import com.mefabc24.strata.render.preview.EntityPreview
import com.mefabc24.strata.render.preview.EntityPreviewVisual
import com.mefabc24.strata.render.preview.PlacementPreviewStyle
import com.mefabc24.strata.testing.TestGdxEnvironment
import com.mefabc24.strata.world.Entity
import com.mefabc24.strata.world.EntityPosition
import com.mefabc24.strata.world.Tile
import com.mefabc24.strata.world.World
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class IsoWorldRendererEntityTest {

    @BeforeTest
    fun installTestEnvironment() {
        TestGdxEnvironment.install()
    }

    @Test
    fun `normal rendering draws visible entities and culls distant ones`() {
        val pixmap = Pixmap(8, 12, Pixmap.Format.RGBA8888)
        val texture = Texture(pixmap)
        pixmap.dispose()
        val renderer = IsoWorldRenderer(
            IsoProjection(TileGeometry(32f, 24f))
        )

        try {
            val world = World(3, 3) { _, _ -> TestTile }
            val entity = world.addEntity(TestEntity, EntityPosition(1.5f, 1.5f))
            val visual = EntityVisual(TextureRegion(texture))
            val camera = OrthographicCamera(100f, 100f).apply {
                position.set(0f, -24f, 0f)
                update()
            }

            renderer.render(
                world = world,
                camera = camera,
                textureFor = { _, _ -> null },
                entityVisualFor = { visual }
            )

            assertEquals(1, renderer.stats.entitiesChecked)
            assertEquals(1, renderer.stats.entitiesDrawn)
            assertEquals(1, renderer.stats.entitiesTotal)

            entity.teleport(EntityPosition(1000f, 1000f))
            renderer.render(
                world = world,
                camera = camera,
                textureFor = { _, _ -> null },
                entityVisualFor = { visual }
            )

            assertEquals(1, renderer.stats.entitiesChecked)
            assertEquals(0, renderer.stats.entitiesDrawn)
            assertEquals(1, renderer.stats.entitiesTotal)
        } finally {
            renderer.dispose()
            texture.dispose()
        }
    }

    @Test
    fun `renderer uses runtime-resolved entity visual and state clock`() {
        val pixmap = Pixmap(8, 12, Pixmap.Format.RGBA8888)
        val texture = Texture(pixmap)
        pixmap.dispose()
        val renderer = IsoWorldRenderer(IsoProjection(TileGeometry(32f, 24f)))

        try {
            val world = World(1, 1) { _, _ -> TestTile }
            val entity = world.addEntity(TestEntity, EntityPosition(0.5f, 0.5f))
            val visual = EntityVisual(TextureRegion(texture))
            val camera = OrthographicCamera(100f, 100f).apply {
                position.set(0f, -12f, 0f)
                update()
            }
            var resolvedRuntime = entity
            var resolvedTime = -1f

            renderer.render(
                world = world,
                camera = camera,
                textureFor = { _, _ -> null },
                entityVisualFor = { error("Legacy visual callback was used.") },
                resolvedEntityVisualFor = { runtime, animationTime ->
                    resolvedRuntime = runtime
                    resolvedTime = animationTime
                    ResolvedEntityVisual(visual, stateTime = 0f, state = null)
                },
                animationTime = 3.5f
            )

            assertEquals(entity, resolvedRuntime)
            assertEquals(3.5f, resolvedTime)
            assertEquals(1, renderer.stats.entitiesDrawn)
        } finally {
            renderer.dispose()
            texture.dispose()
        }
    }

    @Test
    fun `entity preview renders after world content without entering entity stats`() {
        val pixmap = Pixmap(8, 12, Pixmap.Format.RGBA8888)
        val texture = Texture(pixmap)
        pixmap.dispose()
        val renderer = IsoWorldRenderer(IsoProjection(TileGeometry(32f, 24f)))

        try {
            val world = World(1, 1) { _, _ -> TestTile }
            val visual = EntityVisual(TextureRegion(texture))
            val preview = EntityPreview(
                position = EntityPosition(0.5f, 0.5f),
                visual = EntityPreviewVisual.Representative(visual),
                valid = false,
                style = PlacementPreviewStyle(
                    validColor = com.badlogic.gdx.graphics.Color.GREEN,
                    invalidColor = com.badlogic.gdx.graphics.Color.RED
                )
            )
            val camera = OrthographicCamera(100f, 100f).apply {
                position.set(0f, -12f, 0f)
                update()
            }

            renderer.render(
                world = world,
                camera = camera,
                textureFor = { _, _ -> null },
                entityPreviews = listOf(preview)
            )

            assertEquals(1, renderer.stats.previewsDrawn)
            assertEquals(0, renderer.stats.entitiesChecked)
            assertEquals(0, renderer.stats.entitiesDrawn)
            assertEquals(0, renderer.stats.entitiesTotal)
            assertEquals(0, world.entityCount)
        } finally {
            renderer.dispose()
            texture.dispose()
        }
    }

    @Test
    fun `move preview resolves the existing entity visual at render time`() {
        val pixmap = Pixmap(8, 12, Pixmap.Format.RGBA8888)
        val texture = Texture(pixmap)
        pixmap.dispose()
        val renderer = IsoWorldRenderer(IsoProjection(TileGeometry(32f, 24f)))

        try {
            val world = World(1, 1) { _, _ -> TestTile }
            val entity = world.addEntity(TestEntity, EntityPosition(0.5f, 0.5f))
            val visual = EntityVisual(TextureRegion(texture))
            val preview = EntityPreview(
                position = EntityPosition(0.5f, 0.5f),
                visual = EntityPreviewVisual.Existing(entity),
                valid = true,
                style = PlacementPreviewStyle()
            )
            val camera = OrthographicCamera(100f, 100f).apply {
                position.set(0f, -12f, 0f)
                update()
            }
            var resolveCalls = 0

            renderer.render(
                world = world,
                camera = camera,
                textureFor = { _, _ -> null },
                entityPreviews = listOf(preview),
                resolvedEntityVisualFor = { runtime, _ ->
                    assertEquals(entity, runtime)
                    resolveCalls++
                    ResolvedEntityVisual(visual, stateTime = 0f, state = null)
                }
            )

            assertEquals(2, resolveCalls)
            assertEquals(1, renderer.stats.entitiesDrawn)
            assertEquals(1, renderer.stats.previewsDrawn)
        } finally {
            renderer.dispose()
            texture.dispose()
        }
    }

    private data object TestEntity : Entity
    private data object TestTile : Tile
}
