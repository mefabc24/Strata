package com.mefabc24.strata.assets

import com.mefabc24.strata.testing.TestGdxEnvironment
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFailsWith

class StrataAssetsTest {
    @BeforeTest
    fun installTestEnvironment() {
        TestGdxEnvironment.install()
    }

    @Test
    fun `atlas queueing validates paths and deduplicates references`() {
        val assets = StrataAssets()
        try {
            assertFailsWith<IllegalArgumentException> { assets.queueAtlas(" ") }
            assets.queueAtlas("atlas/world.atlas")
            assets.queueAtlas("atlas/world.atlas")
        } finally {
            assets.dispose()
        }
    }

    @Test
    fun `one path cannot be queued as atlas and texture`() {
        val assets = StrataAssets()
        try {
            assets.queueAtlas("atlas/world.atlas")
            assertFailsWith<IllegalArgumentException> {
                assets.queueTexture("atlas/world.atlas")
            }
        } finally {
            assets.dispose()
        }
    }

    @Test
    fun `atlas retrieval requires a queued loaded atlas`() {
        val assets = StrataAssets()
        try {
            assertFailsWith<IllegalStateException> {
                assets.atlas("atlas/world.atlas")
            }
            assets.queueAtlas("atlas/world.atlas")
            assertFailsWith<IllegalStateException> {
                assets.atlas("atlas/world.atlas")
            }
        } finally {
            assets.dispose()
        }
    }
}
