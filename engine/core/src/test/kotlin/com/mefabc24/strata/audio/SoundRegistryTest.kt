package com.mefabc24.strata.audio

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SoundRegistryTest {

    private enum class TestCategory {
        BUILDING,
        NPC
    }

    private enum class BuildingSound : SoundId {
        PLACE,
        DEMOLISH
    }

    private enum class NpcSound : SoundId {
        PLACE
    }

    @Test
    fun `registering a sound stores its definition`() {
        val registry = SoundRegistry<TestCategory> { }

        registry.register(
            id = BuildingSound.PLACE,
            path = "audio/place.wav",
            category = TestCategory.BUILDING
        )

        assertEquals(
            SoundDefinition(
                path = "audio/place.wav",
                category = TestCategory.BUILDING
            ),
            registry[BuildingSound.PLACE]
        )
    }

    @Test
    fun `frozen registry rejects registration and keeps definitions readable`() {
        val registry = SoundRegistry<TestCategory> { }

        registry.register(
            id = BuildingSound.PLACE,
            path = "audio/place.wav",
            category = TestCategory.BUILDING
        )
        registry.freeze()

        val failure = assertFailsWith<IllegalStateException> {
            registry.register(
                id = BuildingSound.DEMOLISH,
                path = "audio/demolish.wav",
                category = TestCategory.BUILDING
            )
        }

        assertEquals(
            "Sound registry registration is already closed.",
            failure.message
        )

        assertEquals(
            SoundDefinition(
                path = "audio/place.wav",
                category = TestCategory.BUILDING
            ),
            registry[BuildingSound.PLACE]
        )
    }

    @Test
    fun `registering a sound queues its asset`() {
        val queuedPaths = mutableListOf<String>()

        val registry = SoundRegistry<TestCategory> { path ->
            queuedPaths.add(path)
        }

        registry.register(
            id = BuildingSound.PLACE,
            path = "audio/place.wav",
            category = TestCategory.BUILDING
        )

        assertEquals(
            listOf("audio/place.wav"),
            queuedPaths
        )
    }

    @Test
    fun `different sound enums can be registered together`() {
        val registry = SoundRegistry<TestCategory> { }

        registry.register(
            id = BuildingSound.PLACE,
            path = "audio/build.wav",
            category = TestCategory.BUILDING
        )

        registry.register(
            id = NpcSound.PLACE,
            path = "audio/npc.wav",
            category = TestCategory.NPC
        )

        assertEquals(
            "audio/build.wav",
            registry[BuildingSound.PLACE].path
        )

        assertEquals(
            "audio/npc.wav",
            registry[NpcSound.PLACE].path
        )
    }

    @Test
    fun `registering the same sound twice is rejected`() {
        val queuedPaths = mutableListOf<String>()

        val registry = SoundRegistry<TestCategory> { path ->
            queuedPaths.add(path)
        }

        registry.register(
            id = BuildingSound.PLACE,
            path = "audio/original.wav",
            category = TestCategory.BUILDING
        )

        assertFailsWith<IllegalArgumentException> {
            registry.register(
                id = BuildingSound.PLACE,
                path = "audio/replacement.wav",
                category = TestCategory.NPC
            )
        }

        assertEquals(
            SoundDefinition(
                path = "audio/original.wav",
                category = TestCategory.BUILDING
            ),
            registry[BuildingSound.PLACE]
        )

        assertEquals(
            listOf("audio/original.wav"),
            queuedPaths
        )
    }

    @Test
    fun `blank sound paths are rejected`() {
        val queuedPaths = mutableListOf<String>()

        val registry = SoundRegistry<TestCategory> { path ->
            queuedPaths.add(path)
        }

        assertFailsWith<IllegalArgumentException> {
            registry.register(
                id = BuildingSound.PLACE,
                path = "   ",
                category = TestCategory.BUILDING
            )
        }

        assertTrue(queuedPaths.isEmpty())

        assertFailsWith<IllegalStateException> {
            registry[BuildingSound.PLACE]
        }
    }

    @Test
    fun `accessing an unregistered sound fails`() {
        val registry = SoundRegistry<TestCategory> { }

        assertFailsWith<IllegalStateException> {
            registry[BuildingSound.DEMOLISH]
        }
    }

    @Test
    fun `failed asset queuing does not register the sound`() {
        val registry = SoundRegistry<TestCategory> {
            error("Asset loading failed.")
        }

        assertFailsWith<IllegalStateException> {
            registry.register(
                id = BuildingSound.PLACE,
                path = "audio/place.wav",
                category = TestCategory.BUILDING
            )
        }

        assertFailsWith<IllegalStateException> {
            registry[BuildingSound.PLACE]
        }
    }
}
