package com.mefabc24.strata.audio

import com.badlogic.gdx.Audio
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.audio.Sound
import com.mefabc24.strata.assets.StrataAssets
import com.mefabc24.strata.testing.TestGdxEnvironment
import com.mefabc24.strata.testing.defaultValue
import com.mefabc24.strata.testing.proxy
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StrataAudioTest {
    private enum class Category : SoundCategoryId { EFFECT, UI }
    private enum class Effect : SoundId { CLICK, ALERT }

    @BeforeTest
    fun installTestEnvironment() {
        TestGdxEnvironment.install()
    }

    @Test
    fun `sound playback multiplies all volume levels and refreshes active instances`() =
        withFixture { fixture ->
            fixture.audio.masterVolume = 0.8f
            fixture.audio.soundVolume = 0.5f
            fixture.audio.setCategoryVolume(Category.EFFECT, 0.25f)

            val handle = fixture.audio.playSound(Effect.CLICK, volume = 0.5f)

            assertTrue(handle != null)
            assertEquals(listOf(0.05f), fixture.sound.playVolumes)
            assertEquals(0.25f, fixture.audio.getCategoryVolume(Category.EFFECT))
            assertEquals(1f, fixture.audio.getCategoryVolume(Category.UI))

            fixture.audio.masterVolume = 0.4f
            fixture.audio.soundVolume = 0.25f
            fixture.audio.setCategoryVolume(Category.EFFECT, 0.5f)

            assertEquals(
                listOf(1L to 0.025f, 1L to 0.0125f, 1L to 0.025f),
                fixture.sound.volumeChanges
            )
        }

    @Test
    fun `failed backend playback returns null and is not tracked`() =
        withFixture(soundPlayResults = mutableListOf(-1L)) { fixture ->
            assertNull(fixture.audio.playSound(Effect.CLICK))

            fixture.audio.stopAllSounds()

            assertTrue(fixture.sound.stoppedIds.isEmpty())
        }

    @Test
    fun `tracking limit stops and forgets the oldest playback`() =
        withFixture(maxTrackedSounds = 2) { fixture ->
            val first = requireNotNull(fixture.audio.playSound(Effect.CLICK))
            val second = requireNotNull(fixture.audio.playSound(Effect.CLICK))
            val third = requireNotNull(fixture.audio.playSound(Effect.CLICK))

            assertEquals(listOf(1L), fixture.sound.stoppedIds)
            assertFalse(fixture.audio.stopSound(first))
            assertTrue(fixture.audio.stopSound(second))
            assertTrue(fixture.audio.stopSound(third))
            assertFalse(fixture.audio.stopSound(third))
            assertEquals(listOf(1L, 2L, 3L), fixture.sound.stoppedIds)
        }

    @Test
    fun `stop all clears every tracked handle`() = withFixture { fixture ->
        val first = requireNotNull(fixture.audio.playSound(Effect.CLICK))
        val second = requireNotNull(fixture.audio.playSound(Effect.ALERT))

        fixture.audio.stopAllSounds()

        assertEquals(listOf(1L, 2L), fixture.sound.stoppedIds)
        assertFalse(fixture.audio.stopSound(first))
        assertFalse(fixture.audio.stopSound(second))
    }

    @Test
    fun `music replacement stops previous track and applies independent volume`() =
        withFixture { fixture ->
            fixture.audio.masterVolume = 0.8f
            fixture.audio.musicVolume = 0.5f

            fixture.audio.playMusic("audio/theme.ogg", volume = 0.25f, loop = false)

            assertEquals(listOf(false), fixture.music.loopingChanges)
            assertEquals(listOf(0.1f), fixture.music.volumeChanges)
            assertEquals(1, fixture.music.playCalls)

            fixture.audio.masterVolume = 0.4f
            fixture.audio.musicVolume = 0.25f
            assertEquals(listOf(0.1f, 0.05f, 0.025f), fixture.music.volumeChanges)

            fixture.audio.playMusic("audio/theme.ogg", volume = 1f, loop = true)

            assertEquals(1, fixture.music.stopCalls)
            assertEquals(listOf(false, true), fixture.music.loopingChanges)
            assertEquals(2, fixture.music.playCalls)
            assertEquals(0.1f, fixture.music.volumeChanges.last())
        }

    @Test
    fun `music pause resume and stop are safe with and without a track`() =
        withFixture { fixture ->
            fixture.audio.pauseMusic()
            fixture.audio.resumeMusic()
            fixture.audio.stopMusic()
            assertEquals(0, fixture.music.pauseCalls)

            fixture.audio.playMusic("audio/theme.ogg")
            fixture.audio.pauseMusic()
            fixture.audio.resumeMusic()
            fixture.audio.stopMusic()
            fixture.audio.stopMusic()

            assertEquals(1, fixture.music.pauseCalls)
            assertEquals(2, fixture.music.playCalls)
            assertEquals(1, fixture.music.stopCalls)
        }

    @Test
    fun `all public volume inputs reject non finite and out of range values`() =
        withFixture { fixture ->
            val invalid = listOf(-0.01f, 1.01f, Float.NaN, Float.POSITIVE_INFINITY)
            invalid.forEach { volume ->
                assertFailsWith<IllegalArgumentException> {
                    fixture.audio.masterVolume = volume
                }
                assertFailsWith<IllegalArgumentException> {
                    fixture.audio.soundVolume = volume
                }
                assertFailsWith<IllegalArgumentException> {
                    fixture.audio.musicVolume = volume
                }
                assertFailsWith<IllegalArgumentException> {
                    fixture.audio.setCategoryVolume(Category.EFFECT, volume)
                }
                assertFailsWith<IllegalArgumentException> {
                    fixture.audio.playSound(Effect.CLICK, volume)
                }
                assertFailsWith<IllegalArgumentException> {
                    fixture.audio.playMusic("audio/theme.ogg", volume)
                }
            }

            fixture.audio.masterVolume = 0f
            fixture.audio.masterVolume = 1f
        }

    @Test
    fun `maximum tracked sound count must be positive`() {
        val assets = StrataAssets()
        try {
            val registry = SoundRegistry(assets)
            assertFailsWith<IllegalArgumentException> {
                StrataAudio(assets, registry, maxTrackedSounds = 0)
            }
        } finally {
            assets.dispose()
        }
    }

    @Test
    fun `dispose stops playback once and rejects later operations`() {
        val fixture = fixture()
        val handle = requireNotNull(fixture.audio.playSound(Effect.CLICK))
        fixture.audio.playMusic("audio/theme.ogg")

        fixture.audio.dispose()
        fixture.audio.dispose()

        assertEquals(listOf(1L), fixture.sound.stoppedIds)
        assertEquals(1, fixture.music.stopCalls)
        assertFailsWith<IllegalStateException> { fixture.audio.playSound(Effect.CLICK) }
        assertFailsWith<IllegalStateException> { fixture.audio.stopSound(handle) }
        assertFailsWith<IllegalStateException> { fixture.audio.stopAllSounds() }
        assertFailsWith<IllegalStateException> { fixture.audio.playMusic("audio/theme.ogg") }
        assertFailsWith<IllegalStateException> { fixture.audio.pauseMusic() }
        assertFailsWith<IllegalStateException> { fixture.audio.resumeMusic() }
        assertFailsWith<IllegalStateException> { fixture.audio.stopMusic() }
        assertFailsWith<IllegalStateException> {
            fixture.audio.getCategoryVolume(Category.EFFECT)
        }
        fixture.assets.dispose()
    }

    private fun withFixture(
        maxTrackedSounds: Int = 128,
        soundPlayResults: MutableList<Long> = mutableListOf(),
        block: (AudioFixture) -> Unit
    ) {
        val fixture = fixture(maxTrackedSounds, soundPlayResults)
        try {
            block(fixture)
        } finally {
            fixture.audio.dispose()
            fixture.assets.dispose()
        }
    }

    private fun fixture(
        maxTrackedSounds: Int = 128,
        soundPlayResults: MutableList<Long> = mutableListOf()
    ): AudioFixture {
        val sound = RecordingSound(soundPlayResults)
        val music = RecordingMusic()
        Gdx.audio = proxy(Audio::class.java) { _, method, _ ->
            when (method.name) {
                "newSound" -> sound.proxy
                "newMusic" -> music.proxy
                else -> defaultValue(method.returnType)
            }
        }
        val assets = StrataAssets()
        val registry = SoundRegistry(assets)
        registry.register(Effect.CLICK, "audio/click.wav", Category.EFFECT)
        registry.register(Effect.ALERT, "audio/alert.wav", Category.UI)
        assets.queueMusic("audio/theme.ogg")
        assets.finishLoading()
        return AudioFixture(
            assets,
            StrataAudio(assets, registry, maxTrackedSounds),
            sound,
            music
        )
    }

    private data class AudioFixture(
        val assets: StrataAssets,
        val audio: StrataAudio,
        val sound: RecordingSound,
        val music: RecordingMusic
    )

    private class RecordingSound(
        private val playResults: MutableList<Long>
    ) {
        val playVolumes = mutableListOf<Float>()
        val volumeChanges = mutableListOf<Pair<Long, Float>>()
        val stoppedIds = mutableListOf<Long>()
        private var nextId = 1L

        val proxy: Sound = proxy(Sound::class.java) { _, method, arguments ->
            when (method.name) {
                "play" -> {
                    playVolumes += arguments?.firstOrNull() as? Float ?: 1f
                    if (playResults.isEmpty()) nextId++ else playResults.removeAt(0)
                }
                "setVolume" -> {
                    volumeChanges += arguments!![0] as Long to arguments[1] as Float
                    null
                }
                "stop" -> {
                    (arguments?.firstOrNull() as? Long)?.let(stoppedIds::add)
                    null
                }
                else -> defaultValue(method.returnType)
            }
        }
    }

    private class RecordingMusic {
        val loopingChanges = mutableListOf<Boolean>()
        val volumeChanges = mutableListOf<Float>()
        var playCalls = 0
        var pauseCalls = 0
        var stopCalls = 0

        val proxy: Music = proxy(Music::class.java) { _, method, arguments ->
            when (method.name) {
                "setLooping" -> { loopingChanges += arguments!![0] as Boolean; null }
                "setVolume" -> { volumeChanges += arguments!![0] as Float; null }
                "play" -> { playCalls++; null }
                "pause" -> { pauseCalls++; null }
                "stop" -> { stopCalls++; null }
                else -> defaultValue(method.returnType)
            }
        }
    }
}
