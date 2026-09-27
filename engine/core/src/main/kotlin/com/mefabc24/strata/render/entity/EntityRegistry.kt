package com.mefabc24.strata.render.entity

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.mefabc24.strata.assets.StrataAssets
import com.mefabc24.strata.render.`object`.AlphaMask
import com.mefabc24.strata.render.`object`.alphaMaskFromClasspath
import com.mefabc24.strata.render.`object`.alphaMasksFromSpriteSheetClasspath
import com.mefabc24.strata.render.sprite.SpriteSource
import com.mefabc24.strata.render.sprite.PreparedVisualDefinition
import com.mefabc24.strata.render.sprite.ResolvedVisual
import com.mefabc24.strata.render.sprite.SpriteDefinitionBuilder
import com.mefabc24.strata.render.sprite.VisualDefinition
import com.mefabc24.strata.render.sprite.VisualPlayback
import com.mefabc24.strata.render.sprite.VisualStateId
import com.mefabc24.strata.render.sprite.spriteSource
import com.mefabc24.strata.render.sprite.alphaMasksFromAtlasClasspath
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
@Suppress("unused")
class EntityEntry internal constructor(
    val type: KClass<out Entity>,
    internal val definition: VisualDefinition<WorldEntity, EntitySpriteDefinition>,
    internal val settings: EntitySpriteSettings
) {
    internal val sources = definition.sources().flatMap(EntitySpriteDefinition::sources)
    private var preparedDefinition:
        PreparedVisualDefinition<WorldEntity, PreparedEntitySprites>? = null
    private var preparedSprites: List<PreparedEntitySprites> = emptyList()

    val visual: EntityVisual
        get() {
            check(
                definition is VisualDefinition.Single &&
                    definition.source is EntitySpriteDefinition.Single
            ) {
                "Entity type $type requires a runtime entity for state or direction."
            }
            val prepared = preparedSprites.singleOrNull()
                as? PreparedEntitySprites.Single
            return prepared?.visual ?: error("Entity type $type is not prepared.")
        }

    val isPrepared: Boolean
        get() = preparedDefinition != null

    internal fun prepare(
        definition: PreparedVisualDefinition<WorldEntity, PreparedEntitySprites>,
        sprites: List<PreparedEntitySprites>
    ) {
        preparedDefinition = definition
        preparedSprites = sprites
    }

    internal fun resolve(
        entity: WorldEntity,
        animationTime: Float
    ): ResolvedVisual<PreparedEntitySprites> {
        return preparedDefinition?.resolve(entity, animationTime)
            ?: error("Entity type $type is not prepared.")
    }
}

/** The active entity visual and the clock used for its current frame. */
class ResolvedEntityVisual internal constructor(
    val visual: EntityVisual,
    val stateTime: Float,
    val state: VisualStateId?
) {
    val frame: EntityVisualFrame
        get() = visual.frameAt(stateTime)
}

/** Scene-owned visual registration for game entity types. */
@Suppress("unused")
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
    },
    private val loadAtlasAlphaMasks: (
        List<SpriteSource>
    ) -> Map<SpriteSource, List<AlphaMask?>> = ::alphaMasksFromAtlasClasspath
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
        registerVisual(
            type = type,
            configure = configure
        ) { sprite(sprite) }
    }

    /**
     * Registers a static atlas region. The atlas path is used as supplied and
     * is not resolved relative to the entity directory.
     */
    fun <T : Entity> registerAtlas(
        type: KClass<T>,
        atlas: String,
        region: String,
        configure: EntitySpriteSettings.() -> Unit = {}
    ) = registerVisual(
        type,
        configure
    ) { atlas(atlas, region) }

    fun <T : Entity> registerAnimated(
        type: KClass<T>,
        frames: List<String>,
        frameDuration: Float,
        configure: EntitySpriteSettings.() -> Unit = {}
    ) {
        registerVisual(
            type = type,
            configure = configure
        ) { animated(frames, frameDuration) }
    }

    /** Registers indexed atlas regions as a looping animation. */
    fun <T : Entity> registerAnimatedAtlas(
        type: KClass<T>,
        atlas: String,
        region: String,
        frameDuration: Float,
        configure: EntitySpriteSettings.() -> Unit = {}
    ) = registerVisual(
        type,
        configure
    ) { animatedAtlas(atlas, region, frameDuration) }

    fun <T : Entity> registerAnimated(
        type: KClass<T>,
        spriteSheet: String,
        frameWidth: Int,
        frameHeight: Int,
        frameDuration: Float,
        frameCount: Int? = null,
        configure: EntitySpriteSettings.() -> Unit = {}
    ) {
        registerVisual(
            type = type,
            configure = configure
        ) {
            spriteSheet(
                spriteSheet,
                frameWidth,
                frameHeight,
                frameDuration,
                frameCount
            )
        }
    }

    inline fun <reified T : Entity> register(
        sprite: String,
        noinline configure: EntitySpriteSettings.() -> Unit = {}
    ) = register(T::class, sprite, configure)

    inline fun <reified T : Entity> registerAtlas(
        atlas: String,
        region: String,
        noinline configure: EntitySpriteSettings.() -> Unit = {}
    ) = registerAtlas(T::class, atlas, region, configure)

    inline fun <reified T : Entity> registerAnimated(
        frames: List<String>,
        frameDuration: Float,
        noinline configure: EntitySpriteSettings.() -> Unit = {}
    ) = registerAnimated(T::class, frames, frameDuration, configure)

    inline fun <reified T : Entity> registerAnimatedAtlas(
        atlas: String,
        region: String,
        frameDuration: Float,
        noinline configure: EntitySpriteSettings.() -> Unit = {}
    ) = registerAnimatedAtlas(
        T::class,
        atlas,
        region,
        frameDuration,
        configure
    )

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

    /** Registers one sprite source through the shared sprite-definition DSL. */
    fun <T : Entity> registerVisual(
        type: KClass<T>,
        configure: EntitySpriteSettings.() -> Unit = {},
        visual: SpriteDefinitionBuilder.() -> Unit
    ) {
        registerDefinition(
            type = type,
            definition = VisualDefinition.Single(
                EntitySpriteDefinition.Single(spriteSource(::resolvePath, visual))
            ),
            configure = configure
        )
    }

    inline fun <reified T : Entity> registerVisual(
        noinline configure: EntitySpriteSettings.() -> Unit = {},
        noinline visual: SpriteDefinitionBuilder.() -> Unit
    ) = registerVisual(T::class, configure, visual)

    /** Registers visuals selected only by the entity's engine-owned direction. */
    fun <T : Entity> registerDirectional(
        type: KClass<T>,
        configure: EntitySpriteSettings.() -> Unit = {},
        visual: EntitySpriteDefinitionBuilder.() -> Unit
    ) {
        registerDefinition(
            type,
            VisualDefinition.Single(
                EntitySpriteDefinitionBuilder(::resolvePath)
                    .apply(visual)
                    .buildEntity()
            ),
            configure
        )
    }

    inline fun <reified T : Entity> registerDirectional(
        noinline configure: EntitySpriteSettings.() -> Unit = {},
        noinline visual: EntitySpriteDefinitionBuilder.() -> Unit
    ) = registerDirectional(T::class, configure, visual)

    /**
     * Registers game-defined states for an entity type.
     *
     * All state assets are queued during setup. The resolver runs at runtime
     * and receives both the engine-owned entity and the typed game entity.
     * States use local playback by default, restarting when the state changes.
     */
    fun <T : Entity> registerStateful(
        type: KClass<T>,
        stateFor: (WorldEntity) -> VisualStateId,
        configure: EntitySpriteSettings.() -> Unit = {},
        states: EntityStatefulVisualBuilder.() -> Unit
    ) = registerStateful(
        type,
        { runtime, _ -> stateFor(runtime) },
        configure,
        states
    )

    fun <T : Entity> registerStateful(
        type: KClass<T>,
        stateFor: (WorldEntity, T) -> VisualStateId,
        configure: EntitySpriteSettings.() -> Unit = {},
        states: EntityStatefulVisualBuilder.() -> Unit
    ) {
        check(registrationOpen) {
            "Entity registry registration is already closed."
        }
        val definitions = EntityStatefulVisualBuilder(
            ::resolvePath,
            VisualPlayback.LOCAL
        ).apply(states).build()
        registerDefinition(
            type = type,
            definition = VisualDefinition.Stateful(
                states = definitions,
                stateFor = { runtime ->
                    stateFor(runtime, type.java.cast(runtime.entity))
                }
            ),
            configure = configure
        )
    }

    inline fun <reified T : Entity> registerStateful(
        noinline stateFor: (WorldEntity) -> VisualStateId,
        noinline configure: EntitySpriteSettings.() -> Unit = {},
        noinline states: EntityStatefulVisualBuilder.() -> Unit
    ) = registerStateful(T::class, stateFor, configure, states)

    inline fun <reified T : Entity> registerStateful(
        noinline stateFor: (WorldEntity, T) -> VisualStateId,
        noinline configure: EntitySpriteSettings.() -> Unit = {},
        noinline states: EntityStatefulVisualBuilder.() -> Unit
    ) = registerStateful(T::class, stateFor, configure, states)

    internal fun prepare() {
        val preparedAtlasMasks = loadAtlasAlphaMasks(
            registrations.values.filterNot(EntityEntry::isPrepared)
                .flatMap(EntityEntry::sources)
                .filter {
                    it is SpriteSource.AtlasRegion ||
                        it is SpriteSource.AtlasAnimation
                }
        )
        registrations.values.forEach { entry ->
            if (entry.isPrepared) return@forEach

            val settings = entry.settings
            val visualsBySource = entry.sources.associateWith { source ->
                val sprite = source.prepare(regionFor, atlasFor)
                val masks = alphaMasksFor(source, preparedAtlasMasks)
                require(masks.size == sprite.frameCount) {
                    "Entity animation alpha-mask count must match its frame count."
                }
                EntityVisual(
                    sprite = sprite,
                    alphaMasks = masks,
                    offsetX = settings.offsetX,
                    offsetY = settings.offsetY,
                    width = settings.width,
                    height = settings.height,
                    scale = settings.scale
                )
            }
            entry.prepare(
                definition = entry.definition.prepare { entitySprites ->
                    entitySprites.prepare(visualsBySource::getValue)
                },
                sprites = entry.definition.sources().map { entitySprites ->
                    entitySprites.prepare(visualsBySource::getValue)
                }
            )
        }
    }

    internal fun freeze() {
        registrationOpen = false
    }

    fun get(entity: WorldEntity): EntityVisual? {
        return registrations[entity.entity::class]?.visual
    }

    /** Resolves the active state and animation clock for [entity]. */
    fun resolve(
        entity: WorldEntity,
        animationTime: Float
    ): ResolvedEntityVisual? {
        val resolved = registrations[entity.entity::class]
            ?.resolve(entity, animationTime)
            ?: return null
        return ResolvedEntityVisual(
            visual = resolved.visual.resolve(entity.direction),
            stateTime = resolved.stateTime,
            state = resolved.state
        )
    }

    private fun <T : Entity> registerDefinition(
        type: KClass<T>,
        definition: VisualDefinition<WorldEntity, EntitySpriteDefinition>,
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
        val sources = definition.sources().flatMap(EntitySpriteDefinition::sources)
        sources.flatMap(SpriteSource::texturePaths).distinct().forEach(queueTexture)
        sources.flatMap(SpriteSource::atlasPaths).distinct().forEach(queueAtlas)
        registrations[type] = EntityEntry(type, definition, settings)
    }

    private fun alphaMasksFor(
        source: SpriteSource,
        preparedAtlasMasks: Map<SpriteSource, List<AlphaMask?>>
    ): List<AlphaMask?> {
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
            is SpriteSource.SpriteSheetRow -> {
                val key = SpriteSheetMaskKey(
                    source.path,
                    source.frameWidth,
                    source.frameHeight,
                    null
                )
                val allMasks = spriteSheetAlphaMasks.getOrPut(key) {
                    loadSpriteSheetAlphaMasks(
                        source.path,
                        source.frameWidth,
                        source.frameHeight,
                        null
                    )
                }
                val columns = regionFor(source.path).regionWidth / source.frameWidth
                val count = source.framesPerRow ?: columns
                val start = source.row * columns
                allMasks.subList(start, start + count)
            }
            is SpriteSource.AtlasRegion,
            is SpriteSource.AtlasAnimation -> {
                checkNotNull(preparedAtlasMasks[source]) {
                    "Atlas alpha masks were not prepared for $source."
                }
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
