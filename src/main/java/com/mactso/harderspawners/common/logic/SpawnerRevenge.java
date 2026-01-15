package com.mactso.harderspawners.common.logic;

import com.mactso.harderspawners.common.utility.MyUtilities;
import com.mactso.harderspawners.common.utility.SharedUtilityMethods;
import com.mactso.harderspawners.modloader.config.MyConfig;
import com.mactso.harderspawners.modloader.events.SpawnerBreakHandler;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;

public class SpawnerRevenge {

	private static final int THREE_SECONDS = 60;
	
	// plays sound when spawner is breaking with blockbreakingspeed event.
	public static void doSpawnerRevenge(final BlockPos pos, ServerPlayer serverPlayer, SpawnerBlockEntity sbe) {
	
		float volume = 0.8f;
		if (SharedUtilityMethods.isSpawnerStunned(sbe)) {
			serverPlayer.serverLevel().playSound(null, pos, SoundEvents.ALLAY_DEATH, SoundSource.AMBIENT, 0.25f, 0.25f);
			return;
		}
	
		serverPlayer.level().playSound(null, pos, SoundEvents.ENDERMAN_AMBIENT, SoundSource.AMBIENT, volume, 0.25f);
		Holder<MobEffect> effect = MobEffects.POISON;
		int revengeLevel = MyConfig.getSpawnerRevengeLevel();
		if (revengeLevel >= 5) {
			effect = MobEffects.WITHER;
		}
		int amplifier = revengeLevel - 1;
		MyUtilities.updateEffect(serverPlayer, amplifier, effect, SpawnerRevenge.THREE_SECONDS);
	}

	public static void doServerSideRevenge(Player player, final BlockPos pos) {
	
		if (MyConfig.getSpawnerRevengeLevel() == 0)
			return;
	
		if (player.level().isClientSide())
			return;
	
		ServerPlayer serverPlayer = (ServerPlayer) player;
		ServerLevel serverlevel = (ServerLevel) serverPlayer.level();

		BlockEntity be = serverlevel.getBlockEntity(pos);
		if (be == null) 
			return;
		if (!(be instanceof SpawnerBlockEntity sbe))  // should always be true by this point
			return;
	
		long gameTime = serverlevel.getGameTime();
	
		if (SpawnerBreakHandler.nextActionTime < gameTime) {
			RandomSource rand = serverPlayer.serverLevel().getRandom();
			SpawnerBreakHandler.nextActionTime = gameTime + 13 + rand.nextInt(5);
			SpecialEffects.doSpawnerBreakingEffects(pos, serverPlayer, serverlevel, sbe, rand);
			SharedUtilityMethods.doDestroyLightingNearSpawner(sbe);
			doSpawnerRevenge(pos, serverPlayer, sbe);
		}
	
	}



}
