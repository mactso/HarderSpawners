package com.mactso.harderspawners.events;

import com.mactso.harderspawners.config.MyConfig;
import com.mactso.harderspawners.util.SharedUtilityMethods;
import com.mactso.harderspawners.util.Utility;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SpawnerBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent.BreakEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class SpawnerBreakHandler {
	static int spamLimiter = 0;
	static boolean SHOW_PARTICLES = true;
	static long cGameTime = 0;

	@SubscribeEvent
	public void onBreakBlock(BreakEvent event) {

		if (MyConfig.getSpawnerMinutesStunned() == 0) {
			return;
		}

		ServerPlayer sp = (ServerPlayer) event.getPlayer();
		if (sp.isCreative())
			return;

		if (!event.isCancelable()) {
			return;
		}

		// server side only event.
		ServerLevel serverLevel = (ServerLevel) sp.level();
		BlockPos pos = event.getPos();
		BlockState bs = serverLevel.getBlockState(pos);
		Block b = bs.getBlock();

		if (b != Blocks.SPAWNER) {
			return;
		}

		BlockEntity be = serverLevel.getBlockEntity(pos);
		sp.level().playSound(null, pos, SoundEvents.ENDERMITE_DEATH, SoundSource.AMBIENT, 1.0f, 1.0f);

		if (be instanceof SpawnerBlockEntity sbe) {

			BaseSpawner mySpawner = sbe.getSpawner();
			CompoundTag tag = new CompoundTag();
			tag = mySpawner.save(tag);
			int minSpawnDelay = tag.getInt("MinSpawnDelay");

			int stunnedTicks = MyConfig.getSpawnerMinutesStunned() * 1200;

			// on first break, "stun" spawner..
			// on second break, let it break naturally.
			if (tag.getInt("MinSpawnDelay") != stunnedTicks) {
				tag.putInt("MinSpawnDelay", stunnedTicks);
				tag.putInt("MaxSpawnDelay", stunnedTicks + 10);
				tag.putInt("Delay", stunnedTicks + 5);
				mySpawner.load(mySpawner.getSpawnerBlockEntity().getLevel(),
						mySpawner.getSpawnerBlockEntity().getBlockPos(), tag);
				sbe.setChanged();
				event.setCanceled(true);
				ServerTickHandler.addClientUpdate(serverLevel, pos);
			} else {
				sp.level().playSound(null, pos, SoundEvents.DISPENSER_FAIL, SoundSource.AMBIENT, 1.0f, 1.0f);
				ServerTickHandler.addClientUpdate(serverLevel, pos);
				event.setCanceled(true);
			}

		}

	}

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
		long gameTime = player.level().getGameTime();

		if (player.level().isClientSide()) {
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

		if (MyConfig.spawnerRevengeLevel > 0) {


			// Serverside
			if (!(player.level().isClientSide())) {

				SimpleParticleType particles = ParticleTypes.CAMPFIRE_COSY_SMOKE;
				ServerPlayer serverPlayer = (ServerPlayer) event.getEntity();
				ServerLevel serverWorld = (ServerLevel) serverPlayer.level();
				BlockEntity be = serverWorld.getBlockEntity(pos);
				if (!(be instanceof SpawnerBlockEntity))
					return;
				int stunnedTicks = MyConfig.getSpawnerMinutesStunned() * 1200;
				SpawnerBlockEntity sbe = (SpawnerBlockEntity) be;
				BaseSpawner mySpawner = sbe.getSpawner();
				CompoundTag tag = new CompoundTag();
				tag = mySpawner.save(tag);
				
				int minSpawnDelay = tag.getInt("MinSpawnDelay");
				if (tag.getInt("MinSpawnDelay") == stunnedTicks) {
					particles = ParticleTypes.ELECTRIC_SPARK;
				}
				RandomSource rand = player.level().getRandom();
				double vx = 0.06 * rand.nextDouble() - 0.03d;
				double vz = 0.06 * rand.nextDouble() - 0.03d;

				Vec3 rfv = new Vec3(vx, 0.05, vz);

				if (cGameTime < gameTime) {
					
					cGameTime = gameTime + 2 + rand.nextInt(3);
					player.level().playSound(null, pos, SoundEvents.ENDERMAN_HURT, SoundSource.AMBIENT, 0.11f, 0.3f);
					BlockPos p = pos;
					for (int j = 0; j < 11; ++j) {
						double x = (double) pos.getX() + rand.nextDouble();
						double y = (double) pos.getY() + 0.95d;
						double z = (double) pos.getZ() + rand.nextDouble();
						serverWorld.sendParticles(particles, x,y,z, 3, rfv.x, rfv.y, rfv.z, -0.04D);
					}
				}
				
				
				boolean destroyedLight = SharedUtilityMethods.removeLightNearSpawner(pos, serverWorld);

				if (revengeLevel >= 0) {
					serverPlayer.level().playSound(null, pos, SoundEvents.ENDERMAN_AMBIENT, SoundSource.AMBIENT,
							0.9f, 0.25f);
					MobEffect effect = MobEffects.POISON;
					if (revengeLevel >= 4) {
						effect = MobEffects.WITHER;
					}
					// This is tricky--- if the player has a more powerful effect, it sometimes
					// sticks "on" and won't expire so remove it once it has half a second left.
					MobEffectInstance ei = player.getEffect(effect);
					if (ei != null) {
						if (ei.getDuration() > 10) {
							return;
						}
						if ((ei.getDuration() < 1) || (ei.getAmplifier() > revengeLevel)) {
							serverPlayer.removeEffectNoUpdate(effect);
						}
					}

					serverPlayer.addEffect(
							new MobEffectInstance(effect, THREE_SECONDS, revengeLevel % 4, true, SHOW_PARTICLES));

					serverPlayer.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, THREE_SECONDS, revengeLevel % 4,
							true, SHOW_PARTICLES));

				}
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
		if (player.level().isClientSide()) {


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
