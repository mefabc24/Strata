package com.mefabc24.strata.debug.inspector

import com.mefabc24.strata.world.Footprint
import com.mefabc24.strata.world.EntityPosition
import com.mefabc24.strata.world.TileOffset
import com.mefabc24.strata.world.TilePosition
import java.util.Locale

/** Formats a footprint without relying on its identity-based default string. */
fun formatFootprint(footprint: Footprint): String {
    val offsets = footprint.offsets.sortedWith(
        compareBy<TileOffset>({ it.y }, { it.x })
    )
    val minX = offsets.minOf(TileOffset::x)
    val maxX = offsets.maxOf(TileOffset::x)
    val minY = offsets.minOf(TileOffset::y)
    val maxY = offsets.maxOf(TileOffset::y)
    val width = maxX - minX + 1
    val height = maxY - minY + 1
    val rectangular = offsets.size == width * height &&
        (minY..maxY).all { y ->
            (minX..maxX).all { x -> TileOffset(x, y) in footprint.offsets }
        }
    return if (rectangular) {
        "$width x $height"
    } else {
        offsets.joinToString(prefix = "[", postfix = "]") { "(${it.x},${it.y})" }
    }
}

fun formatTilePosition(position: TilePosition): String =
    "(${position.x}, ${position.y})"

fun formatTilePositions(positions: Iterable<TilePosition>): String =
    positions.joinToString(prefix = "[", postfix = "]", transform = ::formatTilePosition)

fun formatEntityPosition(position: EntityPosition): String = String.format(
    Locale.ROOT,
    "(%.2f, %.2f)",
    position.x,
    position.y
)
