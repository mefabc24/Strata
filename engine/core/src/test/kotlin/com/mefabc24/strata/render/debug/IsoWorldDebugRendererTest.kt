package com.mefabc24.strata.render.debug

import com.mefabc24.strata.iso.IsoProjection
import com.mefabc24.strata.iso.TileGeometry
import com.mefabc24.strata.world.Entity
import com.mefabc24.strata.world.EntityDirection
import com.mefabc24.strata.world.EntityPosition
import com.mefabc24.strata.world.Footprint
import com.mefabc24.strata.world.Placeable
import com.mefabc24.strata.world.PlacedObject
import com.mefabc24.strata.world.TileOffset
import com.mefabc24.strata.world.TilePosition
import com.mefabc24.strata.world.WorldEntity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class IsoWorldDebugRendererTest {

    @Test
    fun `object debug data uses occupied cells and placement origin`() {
        val placeable = object : Placeable {
            override val footprint = Footprint.custom(
                TileOffset(0, 0),
                TileOffset(1, 0),
                TileOffset(1, 1),
                origin = TileOffset(1, 0)
            )
        }
        val placed = PlacedObject(placeable, x = 4, y = 3)

        assertEquals(
            setOf(
                TilePosition(3, 3),
                TilePosition(4, 3),
                TilePosition(4, 4)
            ),
            placed.occupiedTiles()
        )
        assertEquals(TilePosition(4, 3), objectDebugOrigin(placed))
    }

    @Test
    fun `entity path starts at continuous position without consuming movement`() {
        val entity = WorldEntity(
            TestEntity,
            EntityPosition(1.25f, 2.75f)
        )
        entity.followPath(
            listOf(TilePosition(2, 2), TilePosition(3, 2)),
            speed = 1f
        )
        val before = entity.remainingWaypoints
        val segments = mutableListOf<Pair<EntityPosition, EntityPosition>>()

        forEachEntityDebugPathSegment(entity) { from, to ->
            segments += from to to
        }

        assertEquals(entity.position, segments.first().first)
        assertEquals(before, segments.map { it.second })
        assertEquals(before, entity.remainingWaypoints)
        assertTrue(entity.isMoving)
    }

    @Test
    fun `continuous entity data projects independently of current tile`() {
        val entity = WorldEntity(
            TestEntity,
            EntityPosition(1.25f, 2.75f)
        )
        val projection = IsoProjection(TileGeometry(width = 32f, height = 24f))

        assertEquals(TilePosition(1, 2), entity.currentTile)
        assertEquals(
            projection.tileToWorld(1.25f, 2.75f),
            projection.tileToWorld(entity.position.x, entity.position.y)
        )
    }

    @Test
    fun `path disappears after movement completes`() {
        val entity = WorldEntity(
            TestEntity,
            EntityPosition.centerOf(TilePosition(0, 0))
        )
        entity.followPath(listOf(TilePosition(1, 0)), speed = 1f)

        entity.updateMovement(1f)
        var segmentCount = 0
        forEachEntityDebugPathSegment(entity) { _, _ -> segmentCount++ }

        assertEquals(0, segmentCount)
        assertTrue(entity.remainingWaypoints.isEmpty())
    }

    @Test
    fun `direction target follows world entity facing`() {
        val entity = WorldEntity(
            TestEntity,
            EntityPosition(2f, 3f),
            EntityDirection.NORTH_WEST
        )

        assertEquals(
            EntityPosition(1.55f, 3f),
            entityDebugDirectionTarget(entity)
        )
    }

    private data object TestEntity : Entity
}
