package com.mefabc24.sandbox

import com.mefabc24.strata.world.Footprint
import com.mefabc24.strata.world.Placeable

class OakTree : Placeable {
    override val footprint = Footprint.square(1)
}

class House : Placeable {
    override val footprint = Footprint.square(2)
}

class Villa : Placeable {
    override val footprint = Footprint.square(4)
}

class Pine : Placeable {
    override val footprint = Footprint.square(1)
}

class Trunk1 : Placeable {
    override val footprint = Footprint.square(1)
}

class Trunk2 : Placeable {
    override val footprint = Footprint.square(1)
}

class Trunk3 : Placeable {
    override val footprint = Footprint.square(1)
}

class Trunk4 : Placeable {
    override val footprint = Footprint.square(1)
}

class Flower1 : Placeable {
    override val footprint = Footprint.square(1)
}

class Flower2 : Placeable {
    override val footprint = Footprint.square(1)
}

class RockWater1 : Placeable {
    override val footprint = Footprint.square(1)
}

class RockWater2 : Placeable {
    override val footprint = Footprint.square(1)
}

class RockWater3 : Placeable {
    override val footprint = Footprint.square(1)
}

class Road1 : Placeable {
    override val footprint = Footprint.square(1)
}

class Road2 : Placeable {
    override val footprint = Footprint.square(1)
}

class RoadIntersection : Placeable {
    override val footprint = Footprint.square(1)
}

class Well : Placeable {
    override val footprint = Footprint.square(1)
}