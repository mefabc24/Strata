package com.mefabc24.strata.debug.tools

import com.mefabc24.strata.render.entity.EntityEntry
import com.mefabc24.strata.world.EntityPosition
import com.mefabc24.strata.world.TilePosition
import com.mefabc24.strata.world.World
import com.mefabc24.strata.world.WorldEntity

/** Creates entities through registry factories without defining game behavior. */
class DebugEntitySpawner(
    private val world: World,
    entries: List<EntityEntry>,
    private val isActive: () -> Boolean,
    private val onSpawned: (WorldEntity) -> Unit = {}
) {
    val entries: List<EntityEntry> = entries.toList().also { values ->
        require(values.all(EntityEntry::isSpawnable)) {
            "Debug spawn entries must have registered factories."
        }
    }

    var selectedEntry: EntityEntry? = this.entries.firstOrNull()
        set(value) {
            require(value == null || value in entries) {
                "Selected entity is not in the spawnable registry entries."
            }
            field = value
        }

    fun spawn(position: TilePosition): WorldEntity? {
        if (!isActive() || world.getTile(position) == null) return null
        val entry = selectedEntry ?: return null
        return world.addEntity(
            entity = entry.create(),
            position = EntityPosition.centerOf(position)
        ).also(onSpawned)
    }
}
