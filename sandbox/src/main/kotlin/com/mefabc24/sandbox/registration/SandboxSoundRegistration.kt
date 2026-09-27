package com.mefabc24.sandbox.registration

import com.mefabc24.sandbox.BuildingSound
import com.mefabc24.sandbox.SoundCategory
import com.mefabc24.strata.audio.SoundRegistry

internal fun SoundRegistry.registerSandboxSounds() {
    register(
        id = BuildingSound.PLACE,
        path = "audio/pop.wav",
        category = SoundCategory.BUILDING
    )
}