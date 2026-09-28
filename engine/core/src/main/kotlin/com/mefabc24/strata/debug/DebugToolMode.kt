package com.mefabc24.strata.debug

/** Mutually exclusive engine debug world tools. */
enum class DebugToolMode(val displayName: String) {
    NONE("None"),
    INSPECT("Inspect"),
    BUILD("Build"),
    PAINT("Paint"),
    SPAWN("Spawn"),
    PATHFINDING("Path")
}
