package com.mactso.harderspawners.common.logic;

import java.util.Random;

import com.mactso.harderspawners.common.utility.MyUtilities;
import com.mactso.harderspawners.common.utility.SpawnerUtilityMethods;
import com.mactso.harderspawners.modloader.config.MyConfig;
import com.mactso.harderspawners.modloader.spawnerstorage.SpawnerStatsAdapter;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Handles all special effects related to spawner states, such as expiration, breaking, and time extension.
 * This class provides methods to display particle effects and play sounds when spawners are nearing expiration,
 * are being broken, or when their expiration time is extended.
 */
public class SpecialEffects {

    private static final Random random = new Random();
    /**
     * Sends spawner-like flame and smoke particles at the given spawner position.
     * Server-side only. Automatically sent to nearby players.
     *
     * @param level  the server level
     * @param spawnerPos  the position of the spawner block
     */
    public static void sendSpawnerParticles(ServerLevel level, BlockPos spawnerPos) {
        Vec3 center = Vec3.atCenterOf(spawnerPos); // center of the block

        // 4 particles per call, similar to vanilla spawner
        for (int i = 0; i < 4; i++) {
            double offsetX = random.nextDouble() * 0.6 - 0.3;
            double offsetY = random.nextDouble() * 0.6 - 0.4;
            double offsetZ = random.nextDouble() * 0.6 - 0.3;

            double x = center.x + offsetX;
            double y = center.y + 0.5 + offsetY; // slightly above center
            double z = center.z + offsetZ;

            // Flame particle
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, x, y, z, 1, 0, 0, 0, 0);

            // Smoke particle
            level.sendParticles(ParticleTypes.SMOKE, x, y, z, 1, 0, 0, 0, 0);
        }
    }
    /**
     * Displays effects when a spawner is nearing its expiration time.
     * This method checks the spawner's expiration relative to the chunk age and triggers appropriate
     * particle and sound effects to indicate that the spawner is close to failing.
     *
     * @param sbe The spawner block entity to check and display effects for.
     */
	public static void doSpawnerExpiringSoonEffects(SpawnerBlockEntity sbe) {

		// Wrap the stats
		SpawnerStatsAdapter.SpawnerStatsWrapper statsWrapper = SpawnerStatsAdapter.getOrCreateStats(sbe);
		if (statsWrapper == null || statsWrapper.isInfinite()) {
			return; // Infinite Lifespan or missing stats → no failure effects
		}
		
		Level level = sbe.getLevel();
		if ((level == null) || (!(level instanceof ServerLevel serverLevel)))
				return;


		long lifeSpan = statsWrapper.getLifespan();
		MyUtilities.debugMsg(2, sbe.getBlockPos(), "Spawner time until failure: " + lifeSpan);

		// Threshold for "near failure" special effects (25 spawns * 500 ticks per spawn)
		// note avgTimePerSpawn is normally 200t+800t = 1000t / 2 = 500t = 25 seconds.
		// you can force debugging by setting avgTimePerSpawn to 20.
		long avgTimePerSpawn = statsWrapper.averageSpawnDelay();

		if (avgTimePerSpawn < 1)
			return;
		long expirationThreshold = 30L * avgTimePerSpawn;
		if (lifeSpan > expirationThreshold)
			return;

		// Show repair item if Lifespan Extension Enabled is enabled
		if (MyConfig.isAddLifespanEnabled()) {
			ExtraLifetimeItemDisplays.showDisplay(serverLevel, sbe);
		}

		// this occurs only on a spawn event in Forge. 
		// and it only occurs when delay=1 in Neoforge 

		// Play failing noise and particles
		int	remainingSpawns = (int) (lifeSpan / avgTimePerSpawn);
		SpecialEffects.doSpawnerExpiringNoise(serverLevel, sbe.getBlockPos(), remainingSpawns);
		SpecialEffects.doSpawnerExpiringParticles(serverLevel, sbe.getBlockPos(), remainingSpawns);
	}

	
    /**
     * Plays sound effects for a spawner that is about to expire.
     * The sound volume and type depend on how many spawns are left before expiration.
     *
     * @param sLevel The server level where the spawner is located.
     * @param pos The position of the spawner block.
     * @param spawnsLeft The number of spawns remaining before expiration.
     */
	public static void doSpawnerExpiringNoise(ServerLevel sLevel, BlockPos pos, int spawnsLeft) {

		float volume = 1.0f - (spawnsLeft / 30.0f);

		if (spawnsLeft > 18) {
			sLevel.playSound(null, pos, SoundEvents.FIREWORK_ROCKET_BLAST_FAR, SoundSource.AMBIENT, volume, 0.2f);
		} else if (spawnsLeft > 13) {
			sLevel.playSound(null, pos, SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.AMBIENT, volume, 0.2f);
		} else if (spawnsLeft > 7) {
			sLevel.playSound(null, pos, SoundEvents.FIREWORK_ROCKET_LARGE_BLAST_FAR, SoundSource.AMBIENT, volume, 0.2f);
		} else {
			sLevel.playSound(null, pos, SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, SoundSource.AMBIENT, volume, 0.2f);
		}
	}
	
    /**
     * Displays particle effects for a spawner that is about to expire.
     * The number and behavior of particles depend on how many spawns are left before expiration.
     *
     * @param sLevel The server level where the spawner is located.
     * @param pos The position of the spawner block.
     * @param spawnsLeft The number of spawns remaining before expiration.
     */
	public static void doSpawnerExpiringParticles(ServerLevel sLevel, BlockPos pos, int spawnsLeft) {

		RandomSource rand = sLevel.getRandom();
		int numParticles = 30 - spawnsLeft;

		for (int j = 0; j < numParticles; ++j) {
			double vx = 0.06 * rand.nextDouble() - 0.03d;
			double vz = 0.06 * rand.nextDouble() - 0.03d;
			Vec3 rfv = new Vec3(vx, 0.25, vz);
			double x = (double) pos.getX() + rand.nextDouble();
			double y = (double) pos.getY() + 0.95d;
			double z = (double) pos.getZ() + rand.nextDouble();
			SimpleParticleType particles = ParticleTypes.CAMPFIRE_COSY_SMOKE;
			sLevel.sendParticles(particles, x, y, z, 3, rfv.x, rfv.y, rfv.z, -0.04D);
		}
	}

    /**
     * Displays effects when a spawner is being broken.
     * This method plays a sound and shows particles depending on whether the spawner is stunned.
	 * this is called by breakspeed events the player creates when breaking a block.
	 * it is called by the doServerRevenge method.
     * 
     * @param pos The position of the spawner block.
     * @param serverPlayer The player who is breaking the spawner.
     * @param serverLevel The server level where the spawner is located.
     * @param sbe The spawner block entity being broken.
     * @param rand A random source for particle effects.
     */
	public static void doSpawnerBreakingEffects(final BlockPos pos, ServerPlayer serverPlayer, ServerLevel serverLevel, SpawnerBlockEntity sbe,
			RandomSource rand) {

		int smokeIntensity = 12;
		float volume = 0.9f;
		SimpleParticleType defaultParticle = ParticleTypes.CAMPFIRE_COSY_SMOKE;
		double vx = 0.06 * rand.nextDouble() - 0.03d;
		double vz = 0.06 * rand.nextDouble() - 0.03d;
		Vec3 rfv = new Vec3(vx, 0.05, vz);

		if (SpawnerUtilityMethods.isSpawnerStunned(sbe)) {
			smokeIntensity = 2;
			volume = 0.01f;
			defaultParticle = ParticleTypes.WHITE_SMOKE;
		}

		serverPlayer.level().playSound(null, pos, SoundEvents.ENDERMAN_HURT, SoundSource.AMBIENT, volume, 0.3f);
		for (

				int j = 0; j < smokeIntensity; ++j) {
			SimpleParticleType particles = defaultParticle;
			if (j % 3 == 0) {
				particles = ParticleTypes.ELECTRIC_SPARK;
			}
			double x = (double) pos.getX() + rand.nextDouble();
			double y = (double) pos.getY() + 0.95d;
			double z = (double) pos.getZ() + rand.nextDouble();
			serverLevel.sendParticles(particles, x, y, z, 3, rfv.x, rfv.y, rfv.z, -0.04D);
		}
	}

    /**
     * Plays particle and sound effects to show that a spawner's expiration time has been extended.
     *
     * @param sLevel The server level where the spawner is located.
     * @param pos The position of the spawner block.
     */
	static void doSpawnerTimeExtensionEffects(ServerLevel sLevel, BlockPos pos) {
	    sLevel.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.AMBIENT, 1.0f, 1.0f);
	
	    for (int j = 0; j < 11; ++j) {
	        SimpleParticleType particles = ParticleTypes.SOUL_FIRE_FLAME;
	        Vec3 rfv = new Vec3(0.06 * sLevel.getRandom().nextDouble() - 0.03d, 0.05,
	                0.06 * sLevel.getRandom().nextDouble() - 0.03d);
	        sLevel.sendParticles(particles,
	                pos.getX() + sLevel.getRandom().nextDouble(),
	                pos.getY() + 0.95d,
	                pos.getZ() + sLevel.getRandom().nextDouble(),
	                3,
	                rfv.x, rfv.y, rfv.z, -0.04D);
	    }
	}
	
}
