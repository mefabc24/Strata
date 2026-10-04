package com.mefabc24.sandbox

import com.mefabc24.strata.render.sprite.VisualStateId
import com.mefabc24.strata.world.Entity

class Wolf : Entity

enum class WolfState : VisualStateId {
    IDLE,
    WALK
}

class Boar : Entity

enum class BoarState : VisualStateId {
    IDLE,
    WALK
}