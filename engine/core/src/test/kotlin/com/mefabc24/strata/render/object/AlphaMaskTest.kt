package com.mefabc24.strata.render.`object`

import com.badlogic.gdx.graphics.Pixmap
import com.mefabc24.strata.testing.TestGdxEnvironment
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AlphaMaskTest {

    @BeforeTest
    fun installTestEnvironment() {
        TestGdxEnvironment.install()
    }

    @Test
    fun `pixmap regions preserve frame boundaries and bottom left coordinates`() {
        val pixmap = Pixmap(4, 2, Pixmap.Format.RGBA8888)

        try {
            pixmap.setColor(1f, 1f, 1f, 1f)
            pixmap.drawPixel(0, 0)
            pixmap.drawPixel(3, 1)

            val first = AlphaMask.fromPixmap(pixmap, 0, 0, 2, 2)
            val second = AlphaMask.fromPixmap(pixmap, 2, 0, 2, 2)

            assertTrue(first.isSolid(0.25f, 0.75f))
            assertFalse(first.isSolid(0.75f, 0.25f))
            assertFalse(second.isSolid(0.25f, 0.75f))
            assertTrue(second.isSolid(0.75f, 0.25f))
        } finally {
            pixmap.dispose()
        }
    }

    @Test
    fun `pixmap region outside source is rejected`() {
        val pixmap = Pixmap(2, 2, Pixmap.Format.RGBA8888)

        try {
            kotlin.test.assertFailsWith<IllegalArgumentException> {
                AlphaMask.fromPixmap(pixmap, 1, 1, 2, 2)
            }
        } finally {
            pixmap.dispose()
        }
    }
}
