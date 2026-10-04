package com.mefabc24.strata.debug

import com.badlogic.gdx.Gdx

/** Persistence and startup behavior for the developer-defined DEFAULT visual preset. */
class DebugPresetSettings internal constructor() {
    /** Whether a saved DEFAULT replaces game-defined visual settings during scene setup. */
    var applySavedDefaultOnStartup: Boolean = true

    internal var storageNamespace: String? = null
        private set

    /** Stores DEFAULT in the libGDX preferences namespace owned by the game. */
    fun storage(preferencesName: String) {
        require(preferencesName.isNotBlank()) {
            "Debug preset preferences name must not be blank."
        }
        storageNamespace = preferencesName
    }

    internal fun loadDefault(): DebugVisualConfiguration? {
        val name = storageNamespace ?: return null
        val storedJson = Gdx.app.getPreferences(name).getString(DEFAULT_PRESET_KEY, "")
        if (storedJson.isBlank()) return null

        return runCatching {
            DebugVisualConfigurationCodec.decode(storedJson)
        }.onFailure { error ->
            Gdx.app.error(
                "Strata Debug",
                "Failed to load saved DEFAULT debug preset from '$name'; " +
                    "using the game-defined visual configuration.",
                error
            )
        }.getOrNull()
    }

    internal fun saveDefault(configuration: DebugVisualConfiguration) {
        val name = storageNamespace ?: return
        Gdx.app.getPreferences(name)
            .putString(DEFAULT_PRESET_KEY, DebugVisualConfigurationCodec.encode(configuration))
            .flush()
    }

    private companion object {
        const val DEFAULT_PRESET_KEY = "default-visual-configuration"
    }
}
