package com.mactso.harderspawners.events;

import com.mactso.harderspawners.config.MyConfig;
import com.mactso.harderspawners.util.SharedUtilityMethods;
import com.mactso.harderspawners.util.Utility;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SpawnerBlock;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class SpawnerBreakEvent {
	static int spamLimiter = 0;
	static boolean SHOW_PARTICLES = true;
	
	@SubscribeEvent
	public void blockBreakSpeed(PlayerEvent.BreakSpeed event) {

		final int THREE_SECONDS = 60;

		if (event.getState().getBlock() == null || event.getPosition().isEmpty()) {
			return;
		}
		if (!(event.getState().getBlock() instanceof SpawnerBlock)) {
			return;
		}
		if (event.getEntity() == null) {
			return;
		}
		if (event.getEntity().isCreative()) {
			return;
		}

        final BlockPos pos = event.getPosition().get();
		Block spawnerBlock = event.getState().getBlock();

		// this runs on both sides.
		// On the server to affect the real digging speed.
		// On the server to inflict revenge
		// On the client to affect the apparent visual digging speed.
		String debugSideType = "ServerSide";
		Player player = event.getEntity();

		if (player.level.isClientSide()) {
			debugSideType = "ClientSide";
		}

		if (player instanceof ServerPlayer) {
			debugSideType = "ServerSide";

		}
		if (MyConfig.debugLevel > 0) {
			System.out.println(debugSideType);
		}

		Item playerItem = player.getMainHandItem().getItem();
//	    	if (!playerItem.canHarvestBlock(p.getHeldItemMainhand(), event.getState())) {
//	    		return;
//	    	}
		
	
		boolean toolHarvestsBlockFaster = false;
		float originalToolSpeed = event.getOriginalSpeed();

		if (originalToolSpeed > 1.0f) {
			toolHarvestsBlockFaster = true;
		}

		// float baseDestroySpeed = playerItem.getDestroySpeed(p.getHeldItemMainhand(),
		// s);
		int revengeLevel = MyConfig.spawnerRevengeLevel - 1;

		if (MyConfig.debugLevel > 0) {
			System.out.println("Player" + player.getName().toString() + "breaking a spawner:" + debugSideType);
		}

		if (MyConfig.spawnerRevengeLevel > 0) {
			if (MyConfig.debugLevel > 0) {
				System.out.println("Spawner taking sweet revenge on Player" + player.getName().toString() + " at "
						+ (int) player.getX() + ", " + (int) player.getY() + ", " + (int) player.getZ() + ".");
			}

			// Serverside
			if (!(player.level.isClientSide())) {
				ServerPlayer serverPlayer = (ServerPlayer) event.getEntity();
				ServerLevel serverWorld = serverPlayer.getLevel();

				boolean destroyedLight = SharedUtilityMethods.removeLightNearSpawner(pos, serverWorld);

				// This is tricky--- if the player has a more powerful effect, it sometimes
				// sticks "on" and won't expire so remove it once it has half a second left.
				MobEffectInstance ei = player.getEffect(MobEffects.POISON);
				if (ei != null) {
					if (ei.getDuration() > 10) {
						return;
					}
					if ((ei.getDuration() < 1) || (ei.getAmplifier() > revengeLevel)) {
						serverPlayer.removeEffectNoUpdate(MobEffects.POISON);
					}
				}
				// Apply revenge
				if (revengeLevel >= 0) {
					serverPlayer.getLevel().playSound(null, pos, SoundEvents.ENDERMAN_AMBIENT,
							SoundSource.AMBIENT, 0.9f, 0.25f);
					if (revengeLevel < 4) {
						serverPlayer.addEffect(
								new MobEffectInstance(MobEffects.POISON, THREE_SECONDS, revengeLevel, true, SHOW_PARTICLES));
					} else if (revengeLevel < 7) {
						serverPlayer.addEffect(new MobEffectInstance(MobEffects.WITHER, THREE_SECONDS, revengeLevel - 3,
								true, SHOW_PARTICLES));
					} else {
						serverPlayer.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, THREE_SECONDS,
								revengeLevel - 6, true, SHOW_PARTICLES));
						serverPlayer.addEffect(new MobEffectInstance(MobEffects.WITHER, THREE_SECONDS, revengeLevel - 6,
								true, SHOW_PARTICLES));
					}
				}
			}
			if (MyConfig.debugLevel > 0) {
				System.out.println("Spawner Revenge applied.");
			}
		}

		// both sides
		float baseDestroySpeed = event.getOriginalSpeed();
		float newDestroySpeed = baseDestroySpeed;
		if (MyConfig.spawnerBreakSpeedMultiplier > 0) {
			newDestroySpeed = newDestroySpeed / (1 + MyConfig.spawnerBreakSpeedMultiplier);
			if (newDestroySpeed > 0) {
				event.setNewSpeed(newDestroySpeed);
				if (MyConfig.debugLevel > 0) {
					System.out
							.println("Slowed breaking spawner modifier applied:" + MyConfig.spawnerBreakSpeedMultiplier
									+ " slowing from " + baseDestroySpeed + " to " + newDestroySpeed + ".");
				}
			}
		}

		// client side
		if (player.level.isClientSide()) {
			if ((spamLimiter++) % 20 == 0 && (MyConfig.spawnerTextOff == 0)) {
				Utility.sendChat(player, "The spawner slowly breaks...", ChatFormatting.DARK_AQUA);
				if (MyConfig.debugLevel > 1) {
					Utility.sendChat(player,
							"Slowed breaking spawner modifier applied: " + MyConfig.spawnerBreakSpeedMultiplier
									+ " speed reduced from " + baseDestroySpeed + " to " + newDestroySpeed + ".",
									ChatFormatting.GREEN);
				}

			}
		}
	}

}
