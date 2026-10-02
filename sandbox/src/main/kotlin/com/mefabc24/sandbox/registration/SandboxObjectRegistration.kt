package com.mefabc24.sandbox.registration

import com.mefabc24.strata.render.`object`.ObjectRegistry
import com.mefabc24.sandbox.*

internal fun ObjectRegistry.registerSandboxObjects() {
    registerAtlas(
        atlas = DEMO_ATLAS,
        region = "house",
        factory = ::House
    )

    register(
        sprite = "oak.png",
        factory = ::OakTree
    ) {
        offsetY = 3f
    }

    register(
        sprite = "villa.png",
        factory = ::Villa
    ) {
        offsetY = -16f
        offsetX = -6f
    }

    register(
        sprite = "pine.png",
        factory = ::Pine
    ) {
        offsetY = 3f
    }

    register(
        sprite = "trunk1.png",
        factory = ::Trunk1
    ) {
        offsetY = 3f
    }

    register(
        sprite = "trunk2.png",
        factory = ::Trunk2
    ) {
        offsetY = 3f
    }

    register(
        sprite = "trunk3.png",
        factory = ::Trunk3
    ) {
        offsetY = 3f
    }

    register(
        sprite = "trunk4.png",
        factory = ::Trunk4
    ) {
        offsetY = 3f
    }

    register(
        sprite = "flower1.png",
        factory = ::Flower1
    ) {
        offsetY = 3f
    }

    register(
        sprite = "flower2.png",
        factory = ::Flower2
    ) {
        offsetY = 3f
    }

    register(
        sprite = "rock_water1.png",
        factory = ::RockWater1
    ) {
        offsetY = -3f
    }

    register(
        sprite = "rock_water2.png",
        factory = ::RockWater2
    ) {
        offsetY = -3f
    }

    register(
        sprite = "rock_water3.png",
        factory = ::RockWater3
    ) {
        offsetY = -3f
    }

    register(
        sprite = "road1.png",
        factory = ::Road1
    ) {
        renderPriority = -1
        offsetY = -7f
    }

    register(
        sprite = "road2.png",
        factory = ::Road2
    ) {
        renderPriority = -1
        offsetY = -7f
    }

    register(
        sprite = "road-intersection.png",
        factory = ::RoadIntersection
    ) {
        renderPriority = -1
        offsetY = -7f
    }

    register(
        sprite = "well.png",
        factory = ::Well
    )
}

private const val DEMO_ATLAS = "sandbox.atlas"
