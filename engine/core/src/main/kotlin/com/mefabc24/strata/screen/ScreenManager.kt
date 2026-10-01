package com.mefabc24.strata.screen

import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.mefabc24.strata.input.StrataInput
import com.mefabc24.strata.ui.StrataUi
import com.mefabc24.strata.ui.StrataUiNavigation
import com.mefabc24.strata.ui.StrataUiTheme
import com.mefabc24.strata.world.WorldId
import com.mefabc24.strata.world.WorldManager

/** Stable identifier for a screen registered in one Strata runtime. */
@JvmInline
value class ScreenId(val value: String) {
    init {
        require(value.isNotBlank()) { "Screen ID must not be blank." }
    }

    override fun toString(): String = value
}

/** Values supplied for one screen activation. */
data class ScreenActivation(
    val parameters: Any? = null
)

/** Runtime operations available to screen lifecycle callbacks. */
class ScreenContext internal constructor(
    val id: ScreenId,
    val screens: ScreenManager,
    val worlds: WorldManager
) {
    var activation: ScreenActivation = ScreenActivation()
        internal set

    val parameters: Any?
        get() = activation.parameters

    fun navigate(id: ScreenId, parameters: Any? = null) =
        screens.navigate(id, parameters)

    fun navigate(id: String, parameters: Any? = null) =
        navigate(ScreenId(id), parameters)

    fun back(): Boolean = screens.back()

    fun showOverlay(
        id: ScreenId,
        parameters: Any? = null,
        blocksInput: Boolean = true
    ) = screens.showOverlay(id, parameters, blocksInput)

    fun dismissOverlay(): Boolean = screens.dismissOverlay()
}

internal data class ManagedUi(
    val ui: StrataUi,
    val dispose: () -> Unit
)

internal fun interface ManagedUiFactory {
    fun create(spec: ScreenUiSpec): ManagedUi
}

internal data class ScreenUiSpec(
    val skin: Skin?,
    val theme: StrataUiTheme,
    val configure: StrataUi.() -> Unit
)

/** Declarative definition used by [ScreenManager.register]. */
class ScreenDefinition internal constructor() {
    internal var worldId: WorldId? = null
    internal var uiSpec: ScreenUiSpec? = null
    internal var created: ScreenContext.() -> Unit = {}
    internal var activated: ScreenContext.() -> Unit = {}
    internal var deactivated: ScreenContext.() -> Unit = {}
    internal var disposed: ScreenContext.() -> Unit = {}

    /** Selects the registered world displayed while this screen is visible. */
    fun world(id: WorldId) {
        worldId = id
    }

    fun world(id: String) = world(WorldId(id))

    /**
     * Defines UI using Strata's engine-owned default theme resources.
     *
     * The UI is created on first activation and retained across navigation.
     */
    fun ui(
        theme: StrataUiTheme = StrataUiTheme.default(),
        configure: StrataUi.() -> Unit
    ) {
        check(uiSpec == null) { "A UI has already been defined for this screen." }
        uiSpec = ScreenUiSpec(null, theme, configure)
    }

    /**
     * Advanced escape hatch using a caller-owned Scene2D skin.
     *
     * Strata owns the stage but does not dispose [skin].
     */
    fun ui(
        skin: Skin,
        theme: StrataUiTheme = StrataUiTheme(),
        configure: StrataUi.() -> Unit
    ) {
        check(uiSpec == null) { "A UI has already been defined for this screen." }
        uiSpec = ScreenUiSpec(skin, theme, configure)
    }

    /** Called once, after the screen's optional UI is created. */
    fun onCreate(action: ScreenContext.() -> Unit) {
        created = action
    }

    /** Called whenever the screen enters the visible presentation stack. */
    fun onActivate(action: ScreenContext.() -> Unit) {
        activated = action
    }

    /** Called whenever the screen leaves the visible presentation stack. */
    fun onDeactivate(action: ScreenContext.() -> Unit) {
        deactivated = action
    }

    /** Called once when the screen is removed or the runtime is disposed. */
    fun onDispose(action: ScreenContext.() -> Unit) {
        disposed = action
    }
}

private data class ScreenEntry(
    val id: ScreenId,
    val definition: ScreenDefinition,
    val context: ScreenContext,
    var managedUi: ManagedUi? = null,
    var created: Boolean = false
)

private data class VisibleScreen(
    val entry: ScreenEntry,
    val activation: ScreenActivation,
    val overlay: Boolean,
    val blocksInput: Boolean
)

/**
 * Registers logical screens and coordinates navigation, overlays, and UI.
 *
 * A normal navigation replaces the visible stack and records the previous
 * base screen in history. An overlay remains above the existing stack until
 * dismissed. Visible UIs update and render from base to top; input routes in
 * the reverse order. Blocking overlays stop all input below them even where
 * their UI contains empty space.
 */
@Suppress("unused")
class ScreenManager internal constructor(
    private val worlds: WorldManager,
    private val input: StrataInput,
    private val uiFactory: ManagedUiFactory
) {
    private val entries = linkedMapOf<ScreenId, ScreenEntry>()
    private val visible = mutableListOf<VisibleScreen>()
    private val backStack = mutableListOf<ScreenId>()
    private var disposed = false

    /** Registered identifiers in declaration order. */
    val ids: Set<ScreenId>
        get() = entries.keys.toSet()

    /** Topmost visible screen. */
    val currentId: ScreenId?
        get() = visible.lastOrNull()?.entry?.id

    /** Visible screen identifiers from base to topmost overlay. */
    val activeScreenIds: List<ScreenId>
        get() = visible.map { it.entry.id }

    /** Navigation history from oldest to newest. */
    val history: List<ScreenId>
        get() = backStack.toList()

    val canGoBack: Boolean
        get() = visible.any { it.overlay } || backStack.isNotEmpty()

    fun register(id: ScreenId, configure: ScreenDefinition.() -> Unit) {
        checkActive()
        require(id !in entries) { "Screen '$id' is already registered." }
        val definition = ScreenDefinition().apply(configure)
        definition.worldId?.let { worldId ->
            require(worlds.contains(worldId)) {
                "Screen '$id' references unregistered world '$worldId'."
            }
        }
        entries[id] = ScreenEntry(
            id = id,
            definition = definition,
            context = ScreenContext(id, this, worlds)
        )
    }

    fun register(id: String, configure: ScreenDefinition.() -> Unit) =
        register(ScreenId(id), configure)

    operator fun contains(id: ScreenId): Boolean = id in entries

    fun navigate(id: ScreenId, parameters: Any? = null) {
        checkActive()
        val target = requireEntry(id)
        if (visible.size == 1 && visible.single().entry === target &&
            visible.single().activation.parameters == parameters
        ) return

        visible.firstOrNull()?.takeUnless { it.overlay }?.entry?.id?.let { previous ->
            if (previous != id) backStack += previous
        }
        replacePresentation(target, ScreenActivation(parameters))
    }

    fun navigate(id: String, parameters: Any? = null) =
        navigate(ScreenId(id), parameters)

    fun back(): Boolean {
        checkActive()
        if (visible.lastOrNull()?.overlay == true) return dismissOverlay()

        while (backStack.isNotEmpty()) {
            val id = backStack.removeAt(backStack.lastIndex)
            val target = entries[id] ?: continue
            replacePresentation(target, ScreenActivation())
            return true
        }
        return false
    }

    fun clearHistory() {
        checkActive()
        backStack.clear()
    }

    fun showOverlay(
        id: ScreenId,
        parameters: Any? = null,
        blocksInput: Boolean = true
    ) {
        checkActive()
        check(visible.isNotEmpty()) { "An overlay requires a visible base screen." }
        val entry = requireEntry(id)
        require(visible.none { it.entry === entry }) {
            "Screen '$id' is already visible."
        }
        val item = VisibleScreen(
            entry = entry,
            activation = ScreenActivation(parameters),
            overlay = true,
            blocksInput = blocksInput
        )
        ensureCreated(item)
        visible += item
        activate(item)
        refreshPresentation()
    }

    fun showOverlay(
        id: String,
        parameters: Any? = null,
        blocksInput: Boolean = true
    ) = showOverlay(ScreenId(id), parameters, blocksInput)

    fun dismissOverlay(): Boolean {
        checkActive()
        val item = visible.lastOrNull()?.takeIf { it.overlay } ?: return false
        visible.removeAt(visible.lastIndex)
        deactivate(item)
        refreshPresentation()
        return true
    }

    fun remove(id: ScreenId): Boolean {
        checkActive()
        val entry = entries[id] ?: return false
        val affected = visible.filter { it.entry === entry }
        if (affected.isNotEmpty()) {
            affected.asReversed().forEach(::deactivate)
            visible.removeAll { it.entry === entry }
        }
        backStack.removeAll { it == id }
        entries.remove(id)
        disposeEntry(entry)
        refreshPresentation()
        return true
    }

    fun remove(id: String): Boolean = remove(ScreenId(id))

    internal fun update(realDelta: Float) {
        visible.forEach { it.entry.managedUi?.ui?.update(realDelta) }
    }

    internal fun render() {
        visible.forEach { it.entry.managedUi?.ui?.render() }
    }

    internal fun resize(width: Int, height: Int) {
        entries.values.forEach { it.managedUi?.ui?.resize(width, height) }
    }

    internal fun dispose() {
        if (disposed) return
        disposed = true
        visible.asReversed().forEach(::deactivate)
        visible.clear()
        input.setScreenUiProcessors(emptyList())
        entries.values.toList().asReversed().forEach(::disposeEntry)
        entries.clear()
        backStack.clear()
    }

    private fun replacePresentation(
        target: ScreenEntry,
        activation: ScreenActivation
    ) {
        visible.asReversed().forEach(::deactivate)
        visible.clear()
        val item = VisibleScreen(target, activation, overlay = false, blocksInput = false)
        ensureCreated(item)
        visible += item
        activate(item)
        refreshPresentation()
    }

    private fun ensureCreated(item: VisibleScreen) {
        val entry = item.entry
        if (entry.created) return
        entry.context.activation = item.activation
        entry.managedUi = entry.definition.uiSpec?.let(uiFactory::create)?.also { managed ->
            managed.ui.navigation = object : StrataUiNavigation {
                override fun navigate(id: ScreenId, parameters: Any?) {
                    this@ScreenManager.navigate(id, parameters)
                }

                override fun back(): Boolean = this@ScreenManager.back()

                override fun showOverlay(
                    id: ScreenId,
                    parameters: Any?,
                    blocksInput: Boolean
                ) {
                    this@ScreenManager.showOverlay(id, parameters, blocksInput)
                }

                override fun dismissOverlay(): Boolean =
                    this@ScreenManager.dismissOverlay()
            }
        }
        entry.created = true
        entry.definition.created(entry.context)
    }

    private fun activate(item: VisibleScreen) {
        item.entry.context.activation = item.activation
        item.entry.definition.activated(item.entry.context)
    }

    private fun deactivate(item: VisibleScreen) {
        item.entry.context.activation = item.activation
        item.entry.definition.deactivated(item.entry.context)
    }

    private fun disposeEntry(entry: ScreenEntry) {
        if (entry.created) entry.definition.disposed(entry.context)
        entry.managedUi?.dispose?.invoke()
        entry.managedUi = null
    }

    private fun refreshPresentation() {
        val processors = mutableListOf<InputProcessor>()
        for (item in visible.asReversed()) {
            item.entry.managedUi?.ui?.inputProcessor?.let(processors::add)
            if (item.overlay && item.blocksInput) {
                processors += BlockingInputProcessor
                break
            }
        }
        input.setScreenUiProcessors(processors)

        val worldId = visible.asReversed()
            .firstNotNullOfOrNull { it.entry.definition.worldId }
        if (worldId == null) worlds.deactivate() else worlds.activate(worldId)
    }

    private fun requireEntry(id: ScreenId): ScreenEntry = entries[id]
        ?: error("Screen '$id' is not registered.")

    private fun checkActive() {
        check(!disposed) { "ScreenManager has already been disposed." }
    }
}

private object BlockingInputProcessor : InputProcessor {
    override fun keyDown(keycode: Int) = true
    override fun keyUp(keycode: Int) = true
    override fun keyTyped(character: Char) = true
    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int) = true
    override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int) = true
    override fun touchCancelled(screenX: Int, screenY: Int, pointer: Int, button: Int) = true
    override fun touchDragged(screenX: Int, screenY: Int, pointer: Int) = true
    override fun mouseMoved(screenX: Int, screenY: Int) = true
    override fun scrolled(amountX: Float, amountY: Float) = true
}
