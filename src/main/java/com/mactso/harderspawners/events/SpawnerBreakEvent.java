package com.mactso.harderspawners.events;

import com.mactso.harderspawners.config.MyConfig;
import com.mactso.harderspawners.util.SharedUtilityMethods;

import net.minecraft.block.Block;
import net.minecraft.block.SpawnerBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.text.Color;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class SpawnerBreakEvent {
	static int spamLimiter = 0;
	static boolean SHOW_PARTICLES = true;
	
	@SubscribeEvent
	public void blockBreakSpeed(PlayerEvent.BreakSpeed event) {

		final int THREE_SECONDS = 60;


		if (event.getState().getBlock() == null) {
			return;
		}
		if (!(event.getState().getBlock() instanceof SpawnerBlock)) {
			return;
		}
		if (event.getPlayer() == null) {
			return;
		}
		if (event.getPlayer().isCreative()) {
			return;
		}

		Block spawnerBlock = event.getState().getBlock();

		// this runs on both sides.
		// On the server to affect the real digging speed.
		// On the server to inflict revenge
		// On the client to affect the apparent visual digging speed.
		String debugSideType = "ServerSide";
		PlayerEntity player = event.getPlayer();

		if (player.level.isClientSide()) {
			debugSideType = "ClientSide";
		}

		if (player instanceof ServerPlayerEntity) {
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
				ServerPlayerEntity serverPlayer = (ServerPlayerEntity) event.getPlayer();
				ServerWorld serverWorld = serverPlayer.getLevel();

				boolean destroyedLight = SharedUtilityMethods.removeLightNearSpawner(event.getPos(), serverWorld);

				// This is tricky--- if the player has a more powerful effect, it sometimes
				// sticks "on" and won't expire so remove it once it has half a second left.
				EffectInstance ei = player.getEffect(Effects.POISON);
				if (ei != null) {
					if (ei.getDuration() > 10) {
						return;
					}
					if ((ei.getDuration() < 1) || (ei.getAmplifier() > revengeLevel)) {
						serverPlayer.removeEffectNoUpdate(Effects.POISON);
					}
				}
				// Apply revenge
				if (revengeLevel >= 0) {
					serverPlayer.getLevel().playSound(null, event.getPos(), SoundEvents.ENDERMAN_AMBIENT,
							SoundCategory.AMBIENT, 0.9f, 0.25f);
					if (revengeLevel < 4) {
						serverPlayer.addEffect(
								new EffectInstance(Effects.POISON, THREE_SECONDS, revengeLevel, true, SHOW_PARTICLES));
					} else if (revengeLevel < 7) {
						serverPlayer.addEffect(new EffectInstance(Effects.WITHER, THREE_SECONDS, revengeLevel - 3,
								true, SHOW_PARTICLES));
					} else {
						serverPlayer.addEffect(new EffectInstance(Effects.BLINDNESS, THREE_SECONDS,
								revengeLevel - 6, true, SHOW_PARTICLES));
						serverPlayer.addEffect(new EffectInstance(Effects.WITHER, THREE_SECONDS, revengeLevel - 6,
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
				MyConfig.sendChat(player, "The spawner slowly breaks...", Color.fromLegacyFormat(TextFormatting.DARK_AQUA));
				if (MyConfig.debugLevel > 1) {
					MyConfig.sendChat(player,
							"Slowed breaking spawner modifier applied: " + MyConfig.spawnerBreakSpeedMultiplier
									+ " speed reduced from " + baseDestroySpeed + " to " + newDestroySpeed + ".",
									Color.fromLegacyFormat(TextFormatting.AQUA));
				}

			}
		}
	}

}
