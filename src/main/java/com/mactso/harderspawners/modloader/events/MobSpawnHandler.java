package com.mactso.harderspawners.modloader.events;

import com.mactso.harderspawners.common.logic.ProcessMobSpawns;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;

/**
 * Handles mob spawn processing in Fabric.
 * 
 * NOTE: Fabric does not provide the original SpawnType directly.
 * We'll later capture it using a mixin on Mob#finalizeSpawn and
 * call processMobSpawnWithType().
 */
public class MobSpawnHandler {

    /**
     * This method should be called from the mixin once the spawn type is known.
     * @param mob The mob that was spawned
     * @param spawnType The MobSpawnType from the original finalizeSpawn call
     */
    public static void processMobSpawnWithType(Mob mob, MobSpawnType spawnType) {
        if (mob == null || spawnType == null) return;
        ProcessMobSpawns.processMobSpawn(mob, spawnType);
    }

    /**
     * Temporary registration for Fabric before the mixin is in place.
     * Assumes NATURAL spawn type.
     */
    public static void registerTemporary() {
        // Fabric callback; will pass NATURAL for now
        net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents.ENTITY_LOAD.register((entity, world) -> {
            if (!(entity instanceof Mob mob)) return;
            processMobSpawnWithType(mob, MobSpawnType.NATURAL);
        });
    }
}