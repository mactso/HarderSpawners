package com.mactso.harderspawners.modloader.events;

import com.mactso.harderspawners.common.logic.ProcessMobSpawns;

import net.minecraft.world.entity.Mob;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;


public class MobSpawnHandler {

	@SubscribeEvent
	public void handleMobSpawns(FinalizeSpawnEvent event) {
		// FinalizeSpawnEvent gives us the Mob and the SpawnType

		if (event.getEntity() instanceof Mob mob) {
			ProcessMobSpawns.processMobSpawn(mob, event.getSpawnType());
		}
	}
}