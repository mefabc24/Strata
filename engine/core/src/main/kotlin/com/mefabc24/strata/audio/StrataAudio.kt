package com.mefabc24.strata.audio

import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.utils.Disposable
import com.mefabc24.strata.assets.StrataAssets

/**
 * Identifies a sound playback managed by StrataAudio.
 */
class SoundHandle internal constructor()

/**
 * Plays game audio with game-defined sound categories.
 *
 * StrataAudio controls playback but does not own the assets.
 */
class StrataAudio<C : Enum<C>>(
    private val assets: StrataAssets,
    private val maxTrackedSounds: Int = 128
) : Disposable {

    private data class PlayingSound<C>(
        val sound: Sound,
        val id: Long,
        val category: C,
        val volume: Float
    )

    private val categoryVolumes = mutableMapOf<C, Float>()

    private val playingSounds =
        linkedMapOf<SoundHandle, PlayingSound<C>>()

    private var currentMusic: Music? = null
    private var currentMusicVolume = 1f

    private var disposed = false

    init {
        require(maxTrackedSounds > 0) {
            "Maximum tracked sounds must be positive."
        }
    }

    var masterVolume = 1f
        set(value) {
            validateVolume(value)
            field = value

            refreshSoundVolumes()
            refreshMusicVolume()
        }

    var soundVolume = 1f
        set(value) {
            validateVolume(value)
            field = value

            refreshSoundVolumes()
        }

    var musicVolume = 1f
        set(value) {
            validateVolume(value)
            field = value

            refreshMusicVolume()
        }

    /**
     * Sets the volume of a game-defined sound category.
     */
    fun setCategoryVolume(category: C, volume: Float) {
        checkActive()
        validateVolume(volume)

        categoryVolumes[category] = volume
        refreshSoundVolumes()
    }

    /**
     * Returns the configured category volume or 1 by default.
     */
    fun getCategoryVolume(category: C): Float {
        checkActive()
        return categoryVolumes[category] ?: 1f
    }

    /**
     * Plays a short sound effect.
     *
     * Returns null if the audio backend could not start playback.
     */
    fun playSound(
        path: String,
        category: C,
        volume: Float = 1f
    ): SoundHandle? {
        checkActive()
        validateVolume(volume)

        val sound = assets.sound(path)

        val id = sound.play(
            effectiveSoundVolume(category, volume)
        )

        if (id == -1L) return null

        // Keep the tracked playback count bounded.
        if (playingSounds.size >= maxTrackedSounds) {
            val oldest = playingSounds.entries.first()

            oldest.value.sound.stop(oldest.value.id)
            playingSounds.remove(oldest.key)
        }

        val handle = SoundHandle()

        playingSounds[handle] = PlayingSound(
            sound = sound,
            id = id,
            category = category,
            volume = volume
        )

        return handle
    }

    /**
     * Stops a sound associated with a known handle.
     *
     * Returns false if the handle is no longer tracked.
     */
    fun stopSound(handle: SoundHandle): Boolean {
        checkActive()

        val playing = playingSounds.remove(handle)
            ?: return false

        playing.sound.stop(playing.id)

        return true
    }

    /**
     * Stops all tracked sound effects.
     */
    fun stopAllSounds() {
        checkActive()

        playingSounds.values.forEach { playing ->
            playing.sound.stop(playing.id)
        }

        playingSounds.clear()
    }

    /**
     * Starts a music track, replacing the previous track.
     */
    fun playMusic(
        path: String,
        volume: Float = 1f,
        loop: Boolean = true
    ) {
        checkActive()
        validateVolume(volume)

        val music = assets.music(path)

        currentMusic?.stop()

        currentMusic = music
        currentMusicVolume = volume

        music.isLooping = loop
        refreshMusicVolume()
        music.play()
    }

    fun pauseMusic() {
        checkActive()
        currentMusic?.pause()
    }

    fun resumeMusic() {
        checkActive()
        currentMusic?.play()
    }

    fun stopMusic() {
        checkActive()

        currentMusic?.stop()
        currentMusic = null
    }

    /**
     * Recalculates the volume of all tracked sound instances.
     */
    private fun refreshSoundVolumes() {
        for (playing in playingSounds.values) {
            playing.sound.setVolume(
                playing.id,
                effectiveSoundVolume(
                    playing.category,
                    playing.volume
                )
            )
        }
    }

    private fun refreshMusicVolume() {
        currentMusic?.volume =
            masterVolume * musicVolume * currentMusicVolume
    }

    private fun effectiveSoundVolume(
        category: C,
        volume: Float
    ): Float {
        return masterVolume *
                soundVolume *
                getCategoryVolume(category) *
                volume
    }

    private fun validateVolume(volume: Float) {
        require(volume in 0f..1f) {
            "Volume must be between 0 and 1."
        }
    }

    private fun checkActive() {
        check(!disposed) {
            "StrataAudio has already been disposed."
        }
    }

    /**
     * Stops playback without disposing the managed audio assets.
     */
    override fun dispose() {
        if (disposed) return

        stopAllSounds()
        currentMusic?.stop()

        currentMusic = null
        playingSounds.clear()
        categoryVolumes.clear()

        disposed = true
    }
}