package com.mefabc24.sandbox

import com.mefabc24.strata.world.Entity
import com.mefabc24.strata.world.EntityPosition
import com.mefabc24.strata.world.TilePosition
import com.mefabc24.strata.world.World
import com.mefabc24.strata.world.WorldEntity

data class SandboxSpawnEntry(
    val name: String,
    val create: () -> Entity
) {
    init {
        require(name.isNotBlank()) {
            "A Sandbox spawn entry must have a name."
        }
    }
}

/** Creates selected Sandbox entities without defining their behavior. */
class SandboxEntitySpawner(
    private val world: World,
    private val tools: SandboxToolController,
    entries: List<SandboxSpawnEntry>
) {
    val entries: List<SandboxSpawnEntry> = entries.toList().also { snapshot ->
        require(snapshot.map(SandboxSpawnEntry::name).distinct().size == snapshot.size) {
            "Sandbox spawn entry names must be unique."
        }
    }

    var selectedEntry: SandboxSpawnEntry? = this.entries.firstOrNull()
        set(value) {
            require(value == null || value in entries) {
                "Selected spawn entry is not in the Sandbox spawn catalog."
            }
            field = value
        }

    fun spawn(position: TilePosition): WorldEntity? {
        if (tools.mode != SandboxMode.SPAWN) return null
        if (world.getTile(position) == null) return null

        val entry = selectedEntry ?: return null
        return world.addEntity(
            entity = entry.create(),
            position = EntityPosition.centerOf(position)
        )
    }
}

fun sandboxSpawnEntries(): List<SandboxSpawnEntry> {
    return listOf(
        SandboxSpawnEntry(
            name = "Wolf",
            create = ::Wolf
        )
    )
}
