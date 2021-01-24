package com.mactso.harderspawners.events;

import com.mactso.harderspawners.config.MyConfig;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.SpawnerBlock;
import net.minecraft.block.TorchBlock;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.PickaxeItem;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.Color;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.world.BlockEvent.BreakEvent;
import net.minecraftforge.eventbus.api.Event;
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
		String debugWorldName = "ServerSide";
		PlayerEntity player = event.getPlayer();

		if (player.world.isRemote()) {
			debugWorldName = "ClientSide";
		}

		if (player instanceof ServerPlayerEntity) {
			debugWorldName = "ServerSide";

		}
		if (MyConfig.debugLevel > 0) {
			System.out.println(debugWorldName);
		}

		Item playerItem = player.getHeldItemMainhand().getItem();
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
			System.out.println("Player" + player.getName().toString() + "breaking a spawner:" + debugWorldName);
		}

		if (MyConfig.spawnerRevengeLevel > 0) {
			if (MyConfig.debugLevel > 0) {
				System.out.println("Spawner taking sweet revenge on Player" + player.getName().toString() + " at "
						+ (int) player.getPosX() + ", " + (int) player.getPosY() + ", " + (int) player.getPosZ() + ".");
			}

			// Serverside
			if (!(player.world.isRemote())) {
				ServerPlayerEntity serverPlayer = (ServerPlayerEntity) event.getPlayer();
				World serverWorld = serverPlayer.getServerWorld();
				if (serverWorld.getLight(event.getPos()) > 6) {
					removeLightNearSpawner(event, serverWorld);
				}
				// This is tricky--- if the player has a more powerful effect, it sometimes
				// sticks "on" and won't expire so remove it once it has half a second left.
				EffectInstance ei = player.getActivePotionEffect(Effects.POISON);
				if (ei != null) {
					if (ei.getDuration() > 10) {
						return;
					}
					if ((ei.getDuration() < 1) || (ei.getAmplifier() > revengeLevel)) {
						serverPlayer.removeActivePotionEffect(Effects.POISON);
					}
				}
				// Apply revenge
				if (revengeLevel >= 0) {
					serverPlayer.getServerWorld().playSound(null, event.getPos(), SoundEvents.ENTITY_ENDERMAN_AMBIENT,
							SoundCategory.AMBIENT, 0.9f, 0.25f);
					if (revengeLevel < 4) {
						serverPlayer.addPotionEffect(
								new EffectInstance(Effects.POISON, THREE_SECONDS, revengeLevel, true, SHOW_PARTICLES));
					} else if (revengeLevel < 7) {
						serverPlayer.addPotionEffect(new EffectInstance(Effects.WITHER, THREE_SECONDS, revengeLevel - 3,
								true, SHOW_PARTICLES));
					} else {
						serverPlayer.addPotionEffect(new EffectInstance(Effects.BLINDNESS, THREE_SECONDS,
								revengeLevel - 6, true, SHOW_PARTICLES));
						serverPlayer.addPotionEffect(new EffectInstance(Effects.WITHER, THREE_SECONDS, revengeLevel - 6,
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
		if (player.world.isRemote()) {
			if ((spamLimiter++) % 20 == 0) {
				MyConfig.sendChat(player, "The spawner slowly breaks...", Color.fromTextFormatting(TextFormatting.DARK_AQUA));
				if (MyConfig.debugLevel > 1) {
					MyConfig.sendChat(player,
							"Slowed breaking spawner modifier applied: " + MyConfig.spawnerBreakSpeedMultiplier
									+ " speed reduced from " + baseDestroySpeed + " to " + newDestroySpeed + ".",
									Color.fromTextFormatting(TextFormatting.AQUA));
				}

			}
		}
	}

	private void removeLightNearSpawner(PlayerEvent.BreakSpeed event, World serverWorld) {
		int fX = event.getPos().getX();
		int fZ = event.getPos().getZ();
		int fYmin = event.getPos().getY() - 4;
		if (fYmin < 1)
			fYmin = 1;
		int fYmax = event.getPos().getY() + 4;
		if (fYmax > 254)
			fYmin = 254;
		int scanSize = 7;
		for (int dy = fYmin; dy <= fYmax; dy++) {
			for (int dx = fX - scanSize; dx <= fX + scanSize; dx++) {
				for (int dz = fZ - scanSize; dz <= fZ + scanSize; dz++) {
					BlockPos bP = new BlockPos(dx, dy, dz);
					Block b = serverWorld.getBlockState(bP).getBlock();
					int blockLightLevel = serverWorld.getBlockState(bP).getLightValue();
					if ((blockLightLevel > 7)) {
						serverWorld.destroyBlock(bP, true);
					}
					if (b == Blocks.LAVA) {
						serverWorld.setBlockState(bP, Blocks.COBBLESTONE.getDefaultState(), 3);
					}
				}
			}
		}
	}

}
