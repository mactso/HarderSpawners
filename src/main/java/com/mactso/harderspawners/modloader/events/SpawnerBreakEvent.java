package com.mactso.harderspawners.modloader.events;

import com.mactso.harderspawners.common.logic.ExtraLifetimeItemDisplays;
import com.mactso.harderspawners.common.logic.SpawnerStunLogic;
import com.mactso.harderspawners.common.managers.SpawnerPositionManager;
import com.mactso.harderspawners.common.utility.MyUtilities;
import com.mactso.harderspawners.modloader.config.MyConfig;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;

public class SpawnerBreakEvent {

	public static void register() {

		PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {

			if (!(world instanceof ServerLevel serverLevel))
				return true; // allow break by default

			if (!(player instanceof ServerPlayer sp))
				return true; // allow break by default

			if (!(blockEntity instanceof SpawnerBlockEntity sbe))
				return true;
			
			if (sbe.isRemoved())
				return true;
			
			MyUtilities.debugMsg(1,pos,"Breaking Spawner");
			
			if (sp.isCreative() || MyConfig.getSpawnerMinutesStunned() == 0 ) {
				ExtraLifetimeItemDisplays.removeDisplay(serverLevel, sbe);
				SpawnerPositionManager.forgetSpawner(serverLevel, pos);
				return true;
			}

			// Cancel break if stunned
			boolean stunned = SpawnerStunLogic.processSpawnerStun(sp, serverLevel, sbe);
			if (stunned) 
				return false; // cancel the spawner break.
			

			return true; 

		});
	}
}
