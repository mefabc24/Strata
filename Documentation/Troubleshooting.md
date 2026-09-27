# Troubleshooting

## An asset cannot be loaded

**Symptom:** Scene creation fails while loading or preparing a registered visual or sound.

**Likely cause:** The classpath path is missing, has different capitalization, or was resolved under the wrong registry base directory.

**Fix:** Verify the built resources, not only the source tree. File visuals are prefixed by `terrainDirectory`, `objectDirectory`, or `entityDirectory`; sound and atlas paths are used as supplied.

## A sprite sheet fails during scene creation

**Symptom:** Preparation rejects frame dimensions/count, or the animation uses unintended cells.

**Likely cause:** Sheet width/height is not divisible by `frameWidth`/`frameHeight`, `frameCount` exceeds available cells, or dimensions are in logical units instead of pixels.

**Fix:** Use positive pixel cell dimensions that divide the full image. Omit `frameCount` to use every cell, or keep it within the row-major cell count.

## A directional sheet fails validation

**Symptom:** Registration or preparation rejects direction rows or frames per direction.

**Likely cause:** `directionRows` omits a direction, repeats a row, uses a negative/out-of-range row, or `framesPerDirection` differs from the sheet's column count.

**Fix:** Map all four `EntityDirection` values to distinct valid rows. Make the image width exactly `frameWidth × framesPerDirection`.

## An atlas visual fails or picks incorrectly

**Symptom:** A region is rejected as rotated/trimmed, an animation region is missing/unindexed, or alpha picking does not align.

**Likely cause:** The atlas packer rotated/stripped the region, the static name has multiple matches, or animated regions do not use ascending unique indexes.

**Fix:** Pack world visuals with rotation and whitespace stripping disabled. Use `engine:tools` for compatible defaults. Give static regions one match and animations indexed matches.

## Duplicate registration fails

**Symptom:** Scene setup stops when registering a terrain ID, object/entity class, visual state, direction, or sound ID.

**Likely cause:** Registries require unique keys, and state/direction builders also reject duplicates.

**Fix:** Register each key once. Consolidate helpers or split distinct game types/IDs instead of overwriting a registration.

## Registration after setup fails

**Symptom:** A registry reports that registration is closed.

**Likely cause:** Terrain, object, entity, and sound registries freeze immediately after the scene setup block.

**Fix:** Queue all content inside `scene { registrations { ... } }`. Runtime code may read registry entries and create world instances, but it cannot add content registrations.

## Scene runtime access fails during configuration

**Symptom:** Accessing `strata.scene`, `world`, `view`, `placement`, or `ui` fails while constructing the configured game.

**Likely cause:** The scene block runs during `Strata.create`, before runtime layers are available, and the facade cannot expose a scene before creation.

**Fix:** Use the scene block receiver only for setup. Attach the world, select placement factories, create UI, and read runtime state in `onReady()` or later. Capture providers in input bindings instead of evaluating `lateinit` controllers during setup.

## No world or view is attached

**Symptom:** `strata.world` or `strata.view` access fails, or nothing is rendered.

**Likely cause:** `attachWorld` was not called after scene creation.

**Fix:** Create the game-owned `World` in `onReady()` and call `strata.attachWorld(world, terrainFor)`. A scene accepts only one world attachment.

## UI access fails

**Symptom:** `strata.ui` access fails or no UI appears.

**Likely cause:** `createUi` was not called, was called during scene setup, or failed because a required skin style is missing.

**Fix:** Call `strata.createUi` once after scene creation. Ensure the `Skin` contains the theme's typed styles, and retain/dispose the skin in game code.

## A stateful visual fails through a single-visual property

**Symptom:** Reading `TerrainEntry.sprite`, `ObjectEntry.visual`, or `EntityEntry.visual` fails for a registered type.

**Likely cause:** Those conveniences require one non-stateful visual; the entity property also rejects directional definitions.

**Fix:** Let the world renderer use runtime resolution. For object catalogs, configure/use `ObjectEntry.selectionVisual`. Use registry `resolve` only when code has the required runtime instance and animation time.

## A state resolver fails at runtime

**Symptom:** Rendering reports that a resolved visual state is unregistered.

**Likely cause:** `stateFor` returned a `VisualStateId` that was not declared in the builder.

**Fix:** Register every possible game state during setup and keep resolver/state transitions within that closed set.

## No path is found

**Symptom:** `findPath` returns `null`.

**Likely cause:** `canEnter` rejects start or goal, every four-neighbor route is blocked, or game rules accidentally treat traversable tiles as blocked.

**Fix:** Test start/goal predicates first and inspect the four edge-connected neighbors. The current pathfinder does not use diagonals or infer terrain/object rules.

## An entity does not move

**Symptom:** `followPath` was called but position stays fixed or immediately stops.

**Likely cause:** The path is empty/already reached, the game bypasses `StrataGame`'s update lifecycle, or speed/path input is invalid.

**Fix:** Supply a non-empty route with a positive finite speed and run the normal `StrataGame` update loop. Inspect `isMoving`, `movementSpeed`, and `remainingWaypoints`; enable entity path debugging.

## Movement ignores a new obstacle

**Symptom:** An entity follows a route through a tile that became blocked after pathfinding.

**Likely cause:** Route following does not re-run `canEnter` or validate waypoints against the world.

**Fix:** Let game AI observe world changes, call `cancelMovement`, find a new path, and replace the route.

## Placement does nothing

**Symptom:** `placeAt` returns `null` and no preview appears.

**Likely cause:** Placement is disabled, `selectedFactory` is `null`, `placement {}` was omitted, or occupancy/custom validation rejects the origin.

**Fix:** Configure placement before scene creation, attach a world, assign a factory, and enable the controller. Check `world.canPlace` and the custom validator separately.

## A drag preview remains on screen

**Symptom:** Hover previews no longer update after a custom drag, or old explicit previews remain.

**Likely cause:** Game-side drag policy called `previewAt` without later calling `clearPreviewPositions`.

**Fix:** Clear explicit preview mode in every finish/cancel/tool-change path. The Sandbox controller does this in `finally`/cancel logic.

## A drag gets stuck at the world edge

**Symptom:** Mouse-up or drag actions stop when the pointer leaves the finite world.

**Likely cause:** The action uses `WorldInputBinding.Tile`, which dispatches only for in-world tiles.

**Fix:** Use `Tile` to begin and `Grid` for drag/up. Use `NoPicking` for cleanup that needs no coordinate.

## An object occupies unexpected tiles

**Symptom:** A multi-tile object is shifted relative to the chosen origin.

**Likely cause:** The tool treated placement `(x, y)` as the minimum footprint corner instead of the declared `footprint.origin`.

**Fix:** Use `PlacedObject(placeable, x, y).occupiedTiles()` and the formula `placement + offset - origin`. Check the rectangle origin mapping in [Objects](Objects.md#footprints-and-origins).

## A sprite is offset or scaled unexpectedly

**Symptom:** The visual does not align with logical terrain/occupancy.

**Likely cause:** Object global and per-type offsets are additive, object default width derives from its footprint, entity default width uses texture pixels as world units, or terrain art is scaled to tile width.

**Fix:** Enable sprite-bounds debugging, then set explicit visual `width`/`height` or adjust offsets. Do not change the footprint to repair a purely visual alignment issue.

## An object or entity cannot be sprite-picked

**Symptom:** Clicking inside the apparent rectangle produces no target.

**Likely cause:** The clicked pixel has alpha below 16, the type has no registered visual, bounds are offset from the art, or a frontmost visual consumes the hit.

**Fix:** Enable sprite-bound diagnostics and inspect source transparency. For objects, use `SPRITE_OR_FOOTPRINT` when occupied tiles should be a fallback. Entities currently have alpha or no picking, with no footprint fallback.

## Music playback says the asset was not queued/loaded

**Symptom:** `playMusic(path)` fails even though the file exists.

**Likely cause:** Music has no registration DSL and was never queued before the scene loading pass.

**Fix:** Call `assets.queueMusic(path)` inside the scene setup block, then play it from `onReady()` or later with the identical classpath path.

## UI clicks also affect the world

**Symptom:** Clicking decorative UI triggers a world binding behind it.

**Likely cause:** The actor does not consume input, it sits in empty root space, or its panel has `blocksInput = false`.

**Fix:** Put interactive areas inside a blocking `StrataPanel` or add a Scene2D listener that consumes the event. UI processors already have priority over world input.

## `maxZoom` seems ignored

**Symptom:** The camera zoom-out limit differs from `CameraSettings.maxZoom`.

**Likely cause:** `ZoomMode.WORLD_BASED` derives the limit from world bounds, viewport, and `worldFill`.

**Fix:** Use `ZoomMode.FIXED` when `maxZoom` must be the explicit limit, or tune `worldFill` for world-based fitting.

