# Assets

Strata loads assets from the runtime classpath through a scene-owned `AssetManager`. Put files in `src/main/resources` in a conventional JVM project. Registrations queue their assets during scene setup; scene creation then blocks in `finishLoading()` and prepares all terrain, object, and entity visuals before `onReady()`.

## Base directories and paths

Given:

```kotlin
scene(
    terrainDirectory = "tiles",
    objectDirectory = "objects",
    entityDirectory = "entities"
) { /* ... */ }
```

a file path registered as `"grass.png"` resolves to `tiles/grass.png`, and `"wolf/wolf-idle.png"` resolves to `entities/wolf/wolf-idle.png`. Leading directories are not inferred from the type.

Atlas paths are different: `atlas = "atlas/tiles.atlas"` is used exactly as supplied. Region names are looked up inside that atlas.

```text
src/main/resources/
├── atlas/
│   ├── tiles.atlas
│   └── tiles.png
├── audio/
│   └── place.wav
├── entities/
│   └── wolf/
│       ├── wolf-idle.png
│       └── wolf-run.png
├── objects/
│   └── house.png
└── tiles/
    └── grass.png
```

## Supported visual sources

The common public sprite-definition DSL supports these source forms:

```kotlin
registerVisual<MyObject> {
    sprite("house.png")
}

registerVisual<MyObject> {
    animated(
        frames = listOf("lamp-0.png", "lamp-1.png"),
        frameDuration = 0.15f
    )
}

registerVisual<MyObject> {
    spriteSheet(
        path = "mill.png",
        frameWidth = 64,
        frameHeight = 96,
        frameDuration = 0.1f,
        frameCount = 6
    )
}

registerVisual<MyObject> {
    atlas("world.atlas", "house")
}

registerVisual<MyObject> {
    animatedAtlas(
        atlas = "world.atlas",
        region = "water",
        frameDuration = 0.2f
    )
}
```

Convenience methods named `register`, `registerAnimated`, `registerAtlas`, and `registerAnimatedAtlas` expose the same forms on terrain, object, and entity registries. Only entity directional builders expose a sprite-sheet-row source, through `directionalSpriteSheet`; the row source type itself is internal API.

One visual/state/direction definition must contain exactly one source. Animations loop. `frameDuration` is seconds per frame and must be finite and positive.

### File sequences

Frame paths are ordered exactly as supplied. Every prepared frame must have identical pixel dimensions. For objects and entities, Strata also creates a matching alpha mask for each file.

### Sprite sheets

`frameWidth` and `frameHeight` are pixel dimensions. Both full sheet dimensions must divide evenly by the frame dimensions. Frames are read left-to-right, then top-to-bottom. With no `frameCount`, every cell is used; otherwise the positive count may not exceed the available cells.

Directional sheets require distinct, non-negative rows for either the four diagonal base directions or all eight `EntityDirection` values. Each row must contain exactly `framesPerDirection` columns when that argument is supplied. See [Visuals and Animation](Visuals-and-Animation.md).

### Texture atlases

A static atlas visual requires exactly one region with the requested name. An animated atlas visual uses libGDX indexed regions in ascending, unique index order. The regions must be indexed.

World visual regions must be packed without rotation or whitespace stripping. Rendering bounds and alpha masks assume that packed and original dimensions match. `engine:tools` supplies `TextureAtlasPacker` with compatible defaults; see [the atlas tool README](../engine/tools/README.md). In this repository, `:sandbox:packTextures` packs `sandbox/src/main/assets-src/tiles` into `sandbox/src/main/resources/atlas`.

## Object and entity alpha masks

Object and entity registrations read source pixels during preparation. Sprite-alpha picking first checks the rendered rectangle, then accepts pixels whose alpha is at least 16 out of 255. A transparent area therefore lets picking fall through to a visually lower object/entity. Masks track the active animation frame, state, and direction.

File, sheet, and atlas sources all receive masks. Atlas metadata and page images must remain available on the classpath. Because atlas regions cannot be rotated or trimmed, the mask coordinates line up with the rendered region.

Terrain does not use alpha masks for tile picking: terrain interaction uses the logical isometric top face.

## Sounds and music

Sound registration paths are scene-level classpath paths; there is no sound base directory:

```kotlin
register(
    id = GameSound.PLACE,
    path = "audio/place.wav",
    category = GameSoundCategory.BUILDING
)
```

Music has no registration DSL. Queue it explicitly during scene setup so the scene's loading pass includes it:

```kotlin
scene("tiles", "objects", "entities") {
    assets.queueMusic("audio/ambient.mp3")
    // Other setup blocks...
}
```

After creation, call `strata.audio.playMusic("audio/ambient.mp3")`. See [Audio](Audio.md).

## Loading and ownership

`StrataAssets` deduplicates the same path/type. Reusing a path for a different asset type fails. Lookups require the requested asset to have been queued and loaded. The normal scene workflow is synchronous: there is no loading-screen hook between registration and preparation in `StrataScene`.

The scene owns and disposes its loaded textures, atlases, sounds, and music. Do not dispose registry textures or visuals yourself.
