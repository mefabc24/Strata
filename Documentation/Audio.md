# Audio

Strata separates sound registration from playback. The game defines IDs/categories and registers short sound assets during scene setup. `StrataAudio` plays them later and applies global, channel, category, and per-play volumes.

## Game-owned IDs and registration

```kotlin
enum class GameSound : SoundId {
    PLACE,
    DEMOLISH
}

enum class GameSoundCategory : SoundCategoryId {
    UI,
    BUILDING,
    AMBIENCE
}
```

```kotlin
registrations {
    sounds {
        register(
            id = GameSound.PLACE,
            path = "audio/place.wav",
            category = GameSoundCategory.BUILDING
        )
    }
}
```

Registration queues the classpath asset. IDs must be unique, paths non-blank, and registration must happen before the scene registry freezes. `strata.sounds[id]` returns the immutable `SoundDefinition`; it does not play the sound.

## Playback and volume

```kotlin
val handle = strata.audio.playSound(
    id = GameSound.PLACE,
    volume = 0.8f
)

if (handle != null) {
    strata.audio.stopSound(handle)
}
```

`playSound` returns `null` if the audio backend cannot start playback. Instance volume and all configured volume values must be in `0f..1f`. Effective sound volume is:

```text
masterVolume × soundVolume × categoryVolume × instanceVolume
```

Category volume defaults to `1f` when unset. Changing master, sound, or category volume updates every playback that Strata still tracks. `stopAllSounds()` stops and clears tracked effects. The default tracked limit is 128; starting another effect stops and forgets the oldest tracked playback.

```kotlin
strata.audio.masterVolume = 0.9f
strata.audio.soundVolume = 0.8f
strata.audio.setCategoryVolume(GameSoundCategory.BUILDING, 0.6f)
val categoryVolume = strata.audio.getCategoryVolume(GameSoundCategory.BUILDING)
```

Audio is a live runtime object, so these values may be changed after setup.

## Music

Music paths are not part of `SoundRegistry`. Queue a music asset during the scene block so the scene loading pass includes it:

```kotlin
scene("tiles", "objects", "entities") {
    assets.queueMusic("audio/ambient.mp3")
    // Registrations and other setup...
}
```

Then control it after creation:

```kotlin
strata.audio.playMusic(
    path = "audio/ambient.mp3",
    volume = 0.7f,
    loop = true
)

strata.audio.pauseMusic()
strata.audio.resumeMusic()
strata.audio.stopMusic()
```

Starting music stops the previous track. Its effective volume is `masterVolume × musicVolume × per-track volume`; sound categories do not apply. The scene stops playback and disposes loaded audio assets during disposal.

See [Assets](Assets.md) and [Configuration](Configuration.md).

