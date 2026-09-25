package com.mefabc24.strata.render

import com.mefabc24.strata.iso.IsoProjection

/** Result of comparing two isometric world-space sort volumes. */
internal enum class IsoSpatialRelation {
    BEHIND,
    IN_FRONT,
    AMBIGUOUS
}

/**
 * Closed world-axis bounds used by the isometric painter ordering.
 *
 * Zero-thickness planes are valid. Two bounds are definitely ordered when
 * one ends where the other begins on at least one axis and they are not both
 * the same zero-thickness plane on that axis.
 */
internal data class IsoSortVolume(
    val minX: Int,
    val maxX: Int,
    val minY: Int,
    val maxY: Int,
    val minZ: Int,
    val maxZ: Int
) {
    init {
        require(minX <= maxX && minY <= maxY && minZ <= maxZ) {
            "Isometric sort volume bounds must be ordered."
        }
    }

    fun relationTo(other: IsoSortVolume): IsoSpatialRelation {
        val thisBehind = isDefinitelyBehind(other)
        val otherBehind = other.isDefinitelyBehind(this)

        return when {
            thisBehind && !otherBehind -> IsoSpatialRelation.BEHIND
            otherBehind && !thisBehind -> IsoSpatialRelation.IN_FRONT
            else -> IsoSpatialRelation.AMBIGUOUS
        }
    }

    fun projectedFrontY(projection: IsoProjection): Float {
        return -(maxX + maxY) * projection.tileHeight / 2f +
                minZ * projection.elevationStep
    }

    private fun isDefinitelyBehind(other: IsoSortVolume): Boolean {
        return axisEndsBefore(minX, maxX, other.minX, other.maxX) ||
                axisEndsBefore(minY, maxY, other.minY, other.maxY) ||
                axisEndsBefore(minZ, maxZ, other.minZ, other.maxZ)
    }

    private fun axisEndsBefore(
        min: Int,
        max: Int,
        otherMin: Int,
        otherMax: Int
    ): Boolean {
        if (max < otherMin) return true
        if (max != otherMin) return false

        return min < max || otherMin < otherMax
    }
}

/** Common sort contract for every normal world render primitive. */
internal interface IsoSortable {
    val sortVolume: IsoSortVolume
    val sortKind: Int
    val stableSortKey: String
}
