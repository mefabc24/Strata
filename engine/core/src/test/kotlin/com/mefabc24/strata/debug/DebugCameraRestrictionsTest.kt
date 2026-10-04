package com.mefabc24.strata.debug

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DebugCameraRestrictionsTest {

    @Test
    fun `camera restrictions are disabled while any override is active`() {
        assertFalse(
            cameraRestrictionsDisabled(
                freeCameraToolActive = false,
                persistentOverride = false
            )
        )

        assertTrue(
            cameraRestrictionsDisabled(
                freeCameraToolActive = true,
                persistentOverride = false
            )
        )

        assertTrue(
            cameraRestrictionsDisabled(
                freeCameraToolActive = false,
                persistentOverride = true
            )
        )

        assertTrue(
            cameraRestrictionsDisabled(
                freeCameraToolActive = true,
                persistentOverride = true
            )
        )
    }
}