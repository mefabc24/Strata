# Sandbox assets

Edit terrain artwork in `src/main/assets-src/tiles/`, then regenerate its
committed runtime atlas with:

```text
./gradlew :sandbox:packTextures
```

Generated metadata and page PNGs live in `src/main/resources/atlas/`. Use
`:sandbox:cleanPackedTextures` to remove only the generated `tiles` atlas files.
The separate `sandbox.atlas` remains a small hand-written object/entity demo.
