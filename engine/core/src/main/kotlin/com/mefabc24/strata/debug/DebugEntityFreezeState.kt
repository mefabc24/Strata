package com.mefabc24.strata.debug

import com.mefabc24.strata.world.WorldEntity
import java.util.Collections
import java.util.IdentityHashMap

/** Identity-based debug state for selectively suspending engine movement. */
internal class DebugEntityFreezeState {
    private val frozen = Collections.newSetFromMap(
        IdentityHashMap<WorldEntity, Boolean>()
    )

    fun isFrozen(entity: WorldEntity): Boolean = entity in frozen

    fun setFrozen(entity: WorldEntity, frozen: Boolean): Boolean {
        return if (frozen) this.frozen.add(entity) else this.frozen.remove(entity)
    }

    fun clear(): Int {
        val count = frozen.size
        frozen.clear()
        return count
    }

    fun retain(activeEntities: Set<WorldEntity>) {
        frozen.removeIf { entity -> activeEntities.none { it === entity } }
    }
}
