package com.mefabc24.sandbox.registration

import com.mefabc24.sandbox.DebugWalker
import com.mefabc24.strata.render.entity.EntityRegistry

internal fun EntityRegistry.registerSandboxEntities() {
    registerAtlas<DebugWalker>(
        atlas = DEMO_ATLAS,
        region = "debug-walker"
    )
}

private const val DEMO_ATLAS = "sandbox.atlas"