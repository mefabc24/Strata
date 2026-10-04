package com.mefabc24.strata.render

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RenderingSettingsTest {

    @Test
    fun `global object offsets default to zero`() {
        val settings = RenderingSettings()

        assertEquals(0f, settings.objects.offsetX)
        assertEquals(0f, settings.objects.offsetY)
    }

    @Test
    fun `global object offsets accept finite positive and negative values`() {
        val settings = RenderingSettings().apply {
            objects {
                offsetX = 3f
                offsetY = -4f
            }
        }

        settings.validate()

        assertEquals(3f, settings.objects.offsetX)
        assertEquals(-4f, settings.objects.offsetY)
    }

    @Test
    fun `global object offsets reject non finite values`() {
        for (invalid in listOf(
            Float.NaN,
            Float.POSITIVE_INFINITY,
            Float.NEGATIVE_INFINITY
        )) {
            assertFailsWith<IllegalArgumentException> {
                RenderingSettings().apply {
                    objects.offsetX = invalid
                }.validate()
            }

            assertFailsWith<IllegalArgumentException> {
                RenderingSettings().apply {
                    objects.offsetY = invalid
                }.validate()
            }
        }
    }

    @Test
    fun `copy owns an independent object settings snapshot`() {
        val settings = RenderingSettings().apply {
            objects {
                offsetX = 2f
                offsetY = -1f
            }
        }
        val copy = settings.copy()

        settings.objects.offsetX = 99f
        settings.objects.offsetY = 99f

        assertEquals(2f, copy.objects.offsetX)
        assertEquals(-1f, copy.objects.offsetY)
    }
}
