# Strata texture atlas tools

`engine/tools` converts a directory tree of PNG sources into a standard libGDX
TextureAtlas. Its defaults keep world visuals compatible with Strata: rotation
and whitespace trimming are disabled, and texture filtering is `Nearest`.

Kotlin usage:

```kotlin
TextureAtlasPacker.pack(
    AtlasPackingConfig(sourceDirectory, outputDirectory, "tiles")
)
```

CLI usage:

```text
com.mefabc24.strata.tools.atlas.MainKt <sourceDir> <outputDir> <atlasName>
```

Cleanup CLI usage:

```text
com.mefabc24.strata.tools.atlas.CleanMainKt <outputDir> <atlasName>
```

Repacking removes only the existing `<atlasName>.atlas` and matching numbered
PNG pages before generating the new atlas.
