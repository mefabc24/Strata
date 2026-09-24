package com.mefabc24.strata.iso

/** Defines which terrain plane is used when resolving a tile coordinate. */
enum class TilePickingMode {
    /** Picks the visible terrain surface, including its current elevation. */
    SURFACE,

    /** Picks the permanent elevation-zero grid and ignores terrain elevation. */
    BASE_GRID
}
