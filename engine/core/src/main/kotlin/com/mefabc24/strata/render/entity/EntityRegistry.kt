package com.mefabc24.strata.render.entity

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.mefabc24.strata.assets.StrataAssets
import com.mefabc24.strata.render.`object`.AlphaMask
import com.mefabc24.strata.render.`object`.alphaMaskFromClasspath
import com.mefabc24.strata.render.`object`.alphaMasksFromSpriteSheetClasspath
import com.mefabc24.strata.render.sprite.SpriteSheetGrid
import com.mefabc24.strata.render.sprite.SpriteSource
import com.mefabc24.strata.world.Entity
import com.mefabc24.strata.world.WorldEntity
import kotlin.reflect.KClass

/** Configures one entity type's bottom-center anchored sprite. */
class EntitySpriteSettings {
    var offsetX: Float = 0f
    var offsetY: Float = 0f
    var width: Float? = null
    var height: Float? = null
    var scale: Float = 1f

    internal fun validate() {
        require(offsetX.isFinite() && offsetY.isFinite()) {
            "Entity sprite offsets must be finite."
        }
        require(width == null || (width!!.isFinite() && width!! > 0f)) {
            "Entity sprite width must be finite and positive."
        }
        require(height == null || (height!!.isFinite() && height!! > 0f)) {
            "Entity sprite height must be finite and positive."
        }
        require(scale.isFinite() && scale > 0f) {
            "Entity sprite scale must be finite and positive."
        }
    }
}

/** One registered entity type and its prepared visual metadata. */
class EntityEntry internal constructor(
    val type: KClass<out Entity>,
    internal val source: SpriteSource,
    internal val settings: EntitySpriteSettings
) {
    val spritePath: String = source.assetPaths.first()

    private var preparedVisual: EntityVisual? = null

    val visual: EntityVisual
        get() = preparedVisual
            ?: error("Entity type $type is not prepared.")

    val isPrepared: Boolean
        get() = preparedVisual != null

    internal fun prepare(visual: EntityVisual) {
        preparedVisual = visual
    }
}

/** Scene-owned visual registration for game entity types. */
class EntityRegistry internal constructor(
    directory: String,
    private val queueTexture: (String) -> Unit,
    private val regionFor: (String) -> TextureRegion,
    private val loadAlphaMask: (String) -> AlphaMask?,
    private val loadSpriteSheetAlphaMasks: (
        path: String,
        frameWidth: Int,
        frameHeight: Int,
        frameCount: Int?
    ) -> List<AlphaMask?> = ::alphaMasksFromSpriteSheetClasspath,
    private val queueAtlas: (String) -> Unit = {},
    private val atlasFor: (String) -> TextureAtlas = {
        error("Atlas resolver is not configured.")
    }
) {
    constructor(
        directory: String,
        assets: StrataAssets
    ) : this(
        directory = directory,
        queueTexture = assets::queueTexture,
        regionFor = assets::region,
        loadAlphaMask = ::alphaMaskFromClasspath,
        loadSpriteSheetAlphaMasks = ::alphaMasksFromSpriteSheetClasspath,
        queueAtlas = assets::queueAtlas,
        atlasFor = assets::atlas
    )

    private val baseDirectory = directory.trimEnd('/')
    private val registrations =
        linkedMapOf<KClass<out Entity>, EntityEntry>()
    private val alphaMasks = mutableMapOf<String, AlphaMask?>()
    private val spriteSheetAlphaMasks =
        mutableMapOf<SpriteSheetMaskKey, List<AlphaMask?>>()
    private var registrationOpen = true

    val entries: List<EntityEntry>
        get() = registrations.values.toList()

    fun <T : Entity> register(
        type: KClass<T>,
        sprite: String,
        configure: EntitySpriteSettings.() -> Unit = {}
    ) {
        require(sprite.isNotBlank()) {
            "Sprite path must not be blank."
        }
        registerSource(
            type = type,
            source = SpriteSource.Static(resolvePath(sprite)),
            configure = configure
        )
    }

    fun <T : Entity> registerAnimated(
        type: KClass<T>,
        frames: List<String>,
        frameDuration: Float,
        configure: EntitySpriteSettings.() -> Unit = {}
    ) {
        require(frames.isNotEmpty() && frames.all { it.isNotBlank() }) {
            "Animation frame paths must not be empty or blank."
        }
        registerSource(
            type = type,
            source = SpriteSource.AnimatedFiles(
                paths = frames.map(::resolvePath),
                frameDuration = frameDuration
            ),
            configure = configure
        )
    }

    fun <T : Entity> registerAnimated(
        type: KClass<T>,
        spriteSheet: String,
        frameWidth: Int,
        frameHeight: Int,
        frameDuration: Float,
        frameCount: Int? = null,
        configure: EntitySpriteSettings.() -> Unit = {}
    ) {
        require(spriteSheet.isNotBlank()) {
            "Sprite sheet path must not be blank."
        }
        registerSource(
            type = type,
            source = SpriteSource.SpriteSheet(
                path = resolvePath(spriteSheet),
                frameWidth = frameWidth,
                frameHeight = frameHeight,
                frameDuration = frameDuration,
                frameCount = frameCount
            ),
            configure = configure
        )
    }

    inline fun <reified T : Entity> register(
        sprite: String,
        noinline configure: EntitySpriteSettings.() -> Unit = {}
    ) = register(T::class, sprite, configure)

    inline fun <reified T : Entity> registerAnimated(
        frames: List<String>,
        frameDuration: Float,
        noinline configure: EntitySpriteSettings.() -> Unit = {}
    ) = registerAnimated(T::class, frames, frameDuration, configure)

    inline fun <reified T : Entity> registerAnimated(
        spriteSheet: String,
        frameWidth: Int,
        frameHeight: Int,
        frameDuration: Float,
        frameCount: Int? = null,
        noinline configure: EntitySpriteSettings.() -> Unit = {}
    ) = registerAnimated(
        T::class,
        spriteSheet,
        frameWidth,
        frameHeight,
        frameDuration,
        frameCount,
        configure
    )

    internal fun prepare() {
        registrations.values.forEach { entry ->
            if (entry.isPrepared) return@forEach

            val sprite = entry.source.prepare(regionFor, atlasFor)
            val masks = alphaMasksFor(entry.source)
            require(masks.size == sprite.frameCount) {
                "Entity animation alpha-mask count must match its frame count."
            }

            val settings = entry.settings
            entry.prepare(
                EntityVisual(
                    sprite = sprite,
                    alphaMasks = masks,
                    offsetX = settings.offsetX,
                    offsetY = settings.offsetY,
                    width = settings.width,
                    height = settings.height,
                    scale = settings.scale
                )
            )
        }
    }

    internal fun freeze() {
        registrationOpen = false
    }

    fun get(entity: WorldEntity): EntityVisual? {
        return registrations[entity.entity::class]?.visual
    }

    private fun <T : Entity> registerSource(
        type: KClass<T>,
        source: SpriteSource,
        configure: EntitySpriteSettings.() -> Unit
    ) {
        check(registrationOpen) {
            "Entity registry registration is already closed."
        }
        require(type !in registrations) {
            "Entity type $type is already registered."
        }

        val settings = EntitySpriteSettings().apply(configure)
        settings.validate()
        source.queue(queueTexture, queueAtlas)
        registrations[type] = EntityEntry(type, source, settings)
    }

    private fun alphaMasksFor(source: SpriteSource): List<AlphaMask?> {
        return when (source) {
            is SpriteSource.Static -> listOf(alphaMaskFor(source.path))
            is SpriteSource.AnimatedFiles -> source.assetPaths.map(::alphaMaskFor)
            is SpriteSource.SpriteSheet -> {
                val key = SpriteSheetMaskKey(
                    source.path,
                    source.frameWidth,
                    source.frameHeight,
                    source.frameCount
                )
                spriteSheetAlphaMasks.getOrPut(key) {
                    loadSpriteSheetAlphaMasks(
                        source.path,
                        source.frameWidth,
                        source.frameHeight,
                        source.frameCount
                    )
                }
            }
            is SpriteSource.AtlasRegion,
            is SpriteSource.AtlasAnimation -> {
                List(source.prepare(regionFor, atlasFor).frameCount) { null }
            }
        }
    }

    private fun alphaMaskFor(path: String): AlphaMask? {
        if (path in alphaMasks) return alphaMasks[path]
        return loadAlphaMask(path).also { alphaMasks[path] = it }
    }

    private fun resolvePath(path: String): String {
        return if (baseDirectory.isEmpty()) path else "$baseDirectory/$path"
    }

    private data class SpriteSheetMaskKey(
        val path: String,
        val frameWidth: Int,
        val frameHeight: Int,
        val frameCount: Int?
    )
}
