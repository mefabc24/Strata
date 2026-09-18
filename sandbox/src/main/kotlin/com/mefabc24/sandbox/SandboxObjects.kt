package com.mefabc24.sandbox

import com.mefabc24.strata.world.Footprint
import com.mefabc24.strata.world.Placeable

class OakTree : Placeable {
    override val footprint = Footprint.square(1)
}

class House : Placeable {
    override val footprint = Footprint.square(2)
}