package com.mefabc24.strata.placement

import com.badlogic.gdx.graphics.Color
import com.mefabc24.strata.render.preview.PlacementPreviewStyle
import com.mefabc24.strata.world.Placeable
import com.mefabc24.strata.world.TilePosition
import com.mefabc24.strata.world.World

typealias PlacementValidator = (
    placeable: Placeable,
    position: TilePosition
) -> Boolean

enum class PlacementPreviewBoundsPolicy {

    /** Shows a preview only when every footprint tile is inside the world. */
    ALL_TILES_INSIDE,

    /** Shows a preview when the placement origin is inside the world. */
    ORIGIN_INSIDE,

    /** Shows a preview when at least one footprint tile is inside the world. */
    ANY_TILE_INSIDE,

    /** Shows the preview even when its footprint is outside the world. */
    ALWAYS
}

/** Configures object placement and move previews. */
class PlacementObjectPreviewSettings {
    var enabled: Boolean = true

    var validColor: Color = DEFAULT_VALID_COLOR.cpy()
        set(value) {
            field = value.cpy()
        }

    var invalidColor: Color = DEFAULT_INVALID_COLOR.cpy()
        set(value) {
            field = value.cpy()
        }

    var boundsPolicy: PlacementPreviewBoundsPolicy =
        PlacementPreviewBoundsPolicy.ALL_TILES_INSIDE

    internal fun copy(): PlacementObjectPreviewSettings {
        return PlacementObjectPreviewSettings().also { copy ->
            copy.enabled = enabled
            copy.validColor = validColor
            copy.invalidColor = invalidColor
            copy.boundsPolicy = boundsPolicy
        }
    }

    internal fun style(): PlacementPreviewStyle {
        return PlacementPreviewStyle(validColor.cpy(), invalidColor.cpy())
    }
}

/** Configures entity spawn and move previews. */
class PlacementEntityPreviewSettings {
    var enabled: Boolean = true

    var validColor: Color = DEFAULT_VALID_COLOR.cpy()
        set(value) {
            field = value.cpy()
        }

    var invalidColor: Color = DEFAULT_INVALID_COLOR.cpy()
        set(value) {
            field = value.cpy()
        }

    internal fun copy(): PlacementEntityPreviewSettings {
        return PlacementEntityPreviewSettings().also { copy ->
            copy.enabled = enabled
            copy.validColor = validColor
            copy.invalidColor = invalidColor
        }
    }

    internal fun style(): PlacementPreviewStyle {
        return PlacementPreviewStyle(validColor.cpy(), invalidColor.cpy())
    }
}

/** Scene-wide visual settings for object and entity placement previews. */
class PlacementPreviewSettings {
    val objects = PlacementObjectPreviewSettings()
    val entities = PlacementEntityPreviewSettings()

    fun objects(configure: PlacementObjectPreviewSettings.() -> Unit) {
        objects.apply(configure)
    }

    fun entities(configure: PlacementEntityPreviewSettings.() -> Unit) {
        entities.apply(configure)
    }

    internal fun copy(): PlacementPreviewSettings {
        return PlacementPreviewSettings().also { copy ->
            copy.objects.enabled = objects.enabled
            copy.objects.validColor = objects.validColor
            copy.objects.invalidColor = objects.invalidColor
            copy.objects.boundsPolicy = objects.boundsPolicy
            copy.entities.enabled = entities.enabled
            copy.entities.validColor = entities.validColor
            copy.entities.invalidColor = entities.invalidColor
        }
    }
}

/**
 * Configures the optional placement controller owned by a scene.
 *
 * Settings are captured when scene setup completes. The validator supplements
 * the world's geometric placement rules and cannot override them.
 */
class PlacementSettings {
    val preview = PlacementPreviewSettings()

    private var placementValidator: PlacementValidator = { _, _ -> true }

    fun preview(configure: PlacementPreviewSettings.() -> Unit) {
        preview.apply(configure)
    }

    fun validator(validate: PlacementValidator) {
        placementValidator = validate
    }

    internal fun createController(world: World): PlacementController {
        return PlacementController(
            world = world,
            style = preview.objects.style(),
            previewBoundsPolicy = preview.objects.boundsPolicy,
            placementValidator = placementValidator,
            previewEnabled = preview.objects.enabled,
            entityPreviewSettings = preview.entities.copy()
        )
    }

    internal fun copy(): PlacementSettings {
        return PlacementSettings().also { copy ->
            val previewCopy = preview.copy()
            copy.preview.objects.enabled = previewCopy.objects.enabled
            copy.preview.objects.validColor = previewCopy.objects.validColor
            copy.preview.objects.invalidColor = previewCopy.objects.invalidColor
            copy.preview.objects.boundsPolicy = previewCopy.objects.boundsPolicy
            copy.preview.entities.enabled = previewCopy.entities.enabled
            copy.preview.entities.validColor = previewCopy.entities.validColor
            copy.preview.entities.invalidColor = previewCopy.entities.invalidColor
            copy.placementValidator = placementValidator
        }
    }
}

private val DEFAULT_VALID_COLOR = Color(0.5f, 1f, 0.5f, 0.65f)
private val DEFAULT_INVALID_COLOR = Color(1f, 0.4f, 0.4f, 0.65f)
