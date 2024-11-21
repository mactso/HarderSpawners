package com.mactso.harderspawners.events;

import com.mactso.harderspawners.capabilities.CapabilitySpawner;
import com.mactso.harderspawners.capabilities.ISpawnerStatsStorage;
import com.mactso.harderspawners.config.MyConfig;
import com.mactso.harderspawners.util.SharedUtilityMethods;
import com.mactso.harderspawners.util.Utility;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SpawnerBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent.BreakEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class SpawnerBreakHandler {
	static int spamLimiter = 0;
	static boolean SHOW_PARTICLES = true;
	static long nextActionTime = 0;
	static final int THREE_SECONDS = 60;

	@SubscribeEvent
	public void onBreakBlock(BreakEvent event) {

		if (MyConfig.getSpawnerMinutesStunned() == 0) {
			return;
		}

		ServerPlayer sp = (ServerPlayer) event.getPlayer();
		if (sp.isCreative())
			return;

		if (!event.isCancelable())
			return;

		// server side only event.
		ServerLevel serverLevel = (ServerLevel) sp.level();
		BlockPos pos = event.getPos();
		Block b = serverLevel.getBlockState(pos).getBlock();
		if (b != Blocks.SPAWNER)
			return;
		BlockEntity be = serverLevel.getBlockEntity(pos);
		if (be == null)
			return;
		if (be instanceof SpawnerBlockEntity sbe) {
			BaseSpawner mySpawner = sbe.getSpawner();
			ISpawnerStatsStorage cap = sbe.getCapability(CapabilitySpawner.SPAWNER_STORAGE).orElse(null);
			if (!cap.isStunned()) {

				serverLevel.playSound(null, pos, SoundEvents.ALLAY_DEATH, SoundSource.AMBIENT, 1.0f, 1.0f);

				CompoundTag tag = new CompoundTag();
				tag = mySpawner.save(tag);

				Utility.debugMsg(1, pos, "Stunning Spawner");
				cap.setMaxSpawnDelay(tag.getInt("MaxSpawnDelay"));
				cap.setMinSpawnDelay(tag.getInt("MinSpawnDelay"));
				cap.setStunned(true);
				Utility.debugMsg(2, pos, "Stunned Spawner saved values: (max):" + cap.getMaxSpawnDelay() + "(min):"
						+ cap.getMinSpawnDelay());

				tag.putInt("MinSpawnDelay", MyConfig.getSpawnerTicksStunned());
				tag.putInt("MaxSpawnDelay", MyConfig.getSpawnerTicksStunned() + 10);
				tag.putInt("Delay", MyConfig.getSpawnerTicksStunned() + 5);

				mySpawner.load(serverLevel, sbe.getBlockPos(), tag);
				sbe.setChanged();
				Utility.debugMsg(1, pos, "Stunned Spawner stunned values: (max):" + tag.getInt("MaxSpawnDelay")
						+ "(min):" + tag.getInt("MinSpawnDelay"));

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

		if (isEligible(event)) {
			// this optionally runs on both sides.
			// On the server, change the real digging speed.
			// On the server, inflict revenge
			// On the optional client side, change the visual digging speed.
			Player player = event.getEntity();
			final BlockPos pos = event.getPosition().get();
			doSidedDebugMessage(event, player);
			doServerSideRevenge(event, pos, player);
			doBreakSpeedAdjustment(event, player);
		}
	}

	private boolean isEligible(PlayerEvent.BreakSpeed event) {

		if (event.getState().getBlock() == null || event.getPosition().isEmpty()) {
			return false;
		}
		if (!(event.getState().getBlock() instanceof SpawnerBlock)) {
			return false;
		}
		if (event.getEntity() == null) {
			return false;
		}
		if (event.getEntity().isCreative()) {
			return false;
		}
		return true;
	}

	private void doSidedDebugMessage(PlayerEvent.BreakSpeed event, Player player) {
		if (MyConfig.getDebugLevel() == 0)
			return;

		String debugSideType = "ServerSide";
		if (player.level().isClientSide()) {
			debugSideType = "ClientSide";
		}
		if (player instanceof ServerPlayer) {
			debugSideType = "ServerSide";

		}
		Utility.debugMsg(1, debugSideType);
	}

	private void doBreakSpeedAdjustment(PlayerEvent.BreakSpeed event, Player player) {
		// potentially both sides
		float baseDestroySpeed = event.getOriginalSpeed();
		float newDestroySpeed = baseDestroySpeed;
		if (MyConfig.getSpawnerBreakSpeedModifier() > 0) {
			newDestroySpeed = newDestroySpeed / (1 + MyConfig.getSpawnerBreakSpeedModifier());
			if (newDestroySpeed > 0) {
				event.setNewSpeed(newDestroySpeed);
				Utility.debugMsg(1,
						"Slowed breaking spawner modifier applied:" + MyConfig.getSpawnerBreakSpeedModifier()
								+ " slowing from " + baseDestroySpeed + " to " + newDestroySpeed + ".");
			}
		}
		doOptionalClientMessage(player);
	}

	private void doServerSideRevenge(PlayerEvent.BreakSpeed event, final BlockPos pos, Player player) {

		if (MyConfig.getSpawnerRevengeLevel() == 0)
			return;

		if (!(player.level().isClientSide())) {
			long gameTime = player.level().getGameTime();
			ServerPlayer serverPlayer = (ServerPlayer) event.getEntity();
			ServerLevel slevel = (ServerLevel) serverPlayer.level();
			BlockEntity be = slevel.getBlockEntity(pos);
			if (!(be instanceof SpawnerBlockEntity))
				return;

			SpawnerBlockEntity sbe = (SpawnerBlockEntity) be;

			if (nextActionTime < gameTime) {
				RandomSource rand = player.level().getRandom();
				nextActionTime = gameTime + 2 + rand.nextInt(3);
				doSpawnerBreakingEffects(pos, player, slevel, sbe, rand);
				SharedUtilityMethods.doDestroyLightsNearBlockPos(pos, slevel);
			}
			doSpawnerRevenge(pos, serverPlayer, sbe);
		}
	}

	// This only runs if installed on both sides or on the integrated server.
	private void doOptionalClientMessage(Player player) {
		if (!player.level().isClientSide())
			return;
		if ((spamLimiter++) % 20 == 0 && (MyConfig.getSpawnerTextOff() == 0)) {
			Utility.sendChat(player, "The spawner slowly breaks...", ChatFormatting.DARK_AQUA);
		}
	}

	private void doSpawnerRevenge(final BlockPos pos, ServerPlayer serverPlayer, SpawnerBlockEntity sbe) {

		float volume = 0.8f;
		if (SharedUtilityMethods.isSpawnerStunned(sbe))
			volume = 0.25f;

		serverPlayer.level().playSound(null, pos, SoundEvents.ENDERMAN_AMBIENT, SoundSource.AMBIENT, volume, 0.25f);
		Holder<MobEffect> effect = MobEffects.POISON;
		int revengeLevel = MyConfig.getSpawnerRevengeLevel();
		if (revengeLevel >= 5) {
			effect = MobEffects.WITHER;
		}
		int amplifier = revengeLevel - 1;
		Utility.updateEffect(serverPlayer, amplifier, effect, THREE_SECONDS);
	}

	private void doSpawnerBreakingEffects(final BlockPos pos, Player player, ServerLevel sLevel, SpawnerBlockEntity sbe,
			RandomSource rand) {

		int smokeIntensity = 12;
		float volume = 0.9f;
		SimpleParticleType defaultParticle = ParticleTypes.CAMPFIRE_COSY_SMOKE;
		double vx = 0.06 * rand.nextDouble() - 0.03d;
		double vz = 0.06 * rand.nextDouble() - 0.03d;
		Vec3 rfv = new Vec3(vx, 0.05, vz);

		if (SharedUtilityMethods.isSpawnerStunned(sbe)) {
			smokeIntensity = 2;
			volume = 0.01f;
			defaultParticle = ParticleTypes.WHITE_SMOKE;
		}

		player.level().playSound(null, pos, SoundEvents.ENDERMAN_HURT, SoundSource.AMBIENT, volume, 0.3f);
		for (

				int j = 0; j < smokeIntensity; ++j) {
			SimpleParticleType particles = defaultParticle;
			if (j % 3 == 0) {
				particles = ParticleTypes.ELECTRIC_SPARK;
			}
			double x = (double) pos.getX() + rand.nextDouble();
			double y = (double) pos.getY() + 0.95d;
			double z = (double) pos.getZ() + rand.nextDouble();
			sLevel.sendParticles(particles, x, y, z, 3, rfv.x, rfv.y, rfv.z, -0.04D);
		}
	}

}
