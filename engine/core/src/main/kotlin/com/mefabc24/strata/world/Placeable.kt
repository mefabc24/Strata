package com.mefabc24.strata.world

/**
 * An object that can occupy one or more tiles in a world.
 */
interface Placeable {
    val footprint: Footprint
}