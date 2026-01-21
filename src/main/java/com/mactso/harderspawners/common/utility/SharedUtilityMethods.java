package com.mactso.harderspawners.common.utility;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.mactso.harderspawners.common.logic.ProcessSpawners;
import com.mactso.harderspawners.modloader.config.MyConfig;
import com.mactso.harderspawners.modloader.spawnerstorage.SpawnerAttachments;
import com.mactso.harderspawners.modloader.spawnerstorage.SpawnerStatsAdapter.SpawnerStatsWrapper;
import com.mactso.harderspawners.modloader.spawnerstorage.SpawnerStatsStorage;
import com.mojang.logging.LogUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ProblemReporter.ScopedCollector;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ComparatorBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.PoweredBlock;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.block.RepeaterBlock;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;

/**
 * Utility methods shared across the Harder Spawners mod.
 * 
 * <p>
 * Includes helper functions for spawner status, block brightness adjustments,
 * and destruction of lights near blocks.
 * </p>
 * 
 * <p>
 * <strong>Important:</strong> Some methods are computationally expensive but
 * are safe because they are throttled:
 * <ul>
 * <li>{@link #doDestroyLightingNearSpawner(BlockPos, ServerLevel)} is only
 * called when a spawner is about to spawn (spawnDelay == 1) or when a player
 * attempts to break a spawner.</li>
 * <li>This ensures that even with 3D scanning of nearby blocks, the performance
 * impact is limited.</li>
 * </ul>
 * </p>
 */
public class SharedUtilityMethods {

	private static final org.slf4j.Logger SPAWNERLOGGER = LogUtils.getLogger();
	private static final Set<Class<? extends Block>> ALWAYS_MAX_LIGHT = Set.of(RedstoneLampBlock.class,
			PoweredBlock.class, RedStoneWireBlock.class, LeverBlock.class, RepeaterBlock.class, ComparatorBlock.class);

	public static void logSpawnerState(
	        int level,
	        String context,
	        SpawnerBlockEntity sbe,
	        SpawnerStatsWrapper wrapper
	) {
	    if (sbe == null)
	        return;

	    BaseSpawner spawner = sbe.getSpawner();
	    CompoundTag spawnerTag = saveSpawnerToTag(sbe);
	    int delay = ProcessSpawners.getSpawnerDelay(sbe, spawner, spawnerTag);

	    MyUtilities.debugMsg(
	        level,
	        sbe.getBlockPos(),
	        context
	        + " Spawner state | life="
	        + (wrapper == null ? "null" : wrapper.getLifespan())
	        + " | stunned="
	        + (wrapper == null ? "null" : wrapper.isStunned())
	        + " | Orig [min="
	        + (wrapper == null ? "null" : wrapper.getOriginalMinSpawnDelay())
	        + ", max="
	        + (wrapper == null ? "null" : wrapper.getOriginalMaxSpawnDelay())
	        + ", delay="
	        + delay
	        + "] | NBT[min="
	        + spawnerTag.getInt("MinSpawnDelay")
	        + ", max="
	        + spawnerTag.getInt("MaxSpawnDelay")
	        + ", delay="
	        + spawnerTag.getInt("Delay")
	        + "]"
	    );
	}

	/** 
	 * Serializes a SpawnerBlockEntity into a CompoundTag. 
	 * @param sbe the spawner block entity 
	 * @return serialized NBT representing the spawner 
	 */
	public static CompoundTag saveSpawnerToTag(SpawnerBlockEntity sbe) {
		ScopedCollector problemReporter = new ScopedCollector(SPAWNERLOGGER);
		TagValueOutput output = TagValueOutput.createWithoutContext(problemReporter);
		sbe.getSpawner().save(output);
		return output.buildResult();
	}

	/** 
	 * Loads spawner data from a CompoundTag into a SpawnerBlockEntity. 
	 * @param sbe target spawner 
	 * @param tag NBT to load 
	 */
	public static void loadSpawnerFromTag(SpawnerBlockEntity sbe, CompoundTag tag) {
		ScopedCollector collector = new ScopedCollector(SPAWNERLOGGER);
		ValueInput input = TagValueInput.create(collector, sbe.getLevel().registryAccess(), tag);
		sbe.getSpawner().load(sbe.getLevel(), sbe.getBlockPos(), input);
	}

	/** 
	 * Returns true if the given spawner is stunned according to its SpawnerStats. 
	 */
	public static boolean isSpawnerStunned(SpawnerBlockEntity sbe) {
		SpawnerStatsStorage stats = sbe.getData(SpawnerAttachments.SPAWNER_STATS.get());
		return stats != null && stats.isStunned();
	}

	/**
	 * Adjusts the effective light level of a block, taking certain redstone
	 * components into account (which can provide maximum brightness regardless of
	 * the normal light emission).
	 *
	 * @param world            the level containing the block
	 * @param placedBlockState the state of the block
	 * @param placedBlock      the block type
	 * @param placedPos        the position of the block
	 * @return the effective light emission of the block
	 */
	public static int checkAdjustedBlockBrightness(Level world, BlockState placedBlockState, Block placedBlock,
			BlockPos placedPos) {

		if (ALWAYS_MAX_LIGHT.contains(placedBlock.getClass()))
			return 15;

		return placedBlockState.getLightEmission(world, placedPos);

	}

	/**
	 * Destroys light sources near the given block position if they exceed the
	 * hostile spawner light level limit. Note: This *only* runs when someone tries
	 * to break a spawner or when the spawner tries to spawn.
	 *
	 * <p>
	 * <strong>Trigger conditions:</strong>
	 * <ul>
	 * <li>Called when a player attempts to break a spawner block.</li>
	 * <li>Called when a spawner is about to spawn (spawnDelay == 1).</li>
	 * </ul>
	 * 
	 * <p>
	 * The method scans a cubic area around the target block and may:
	 * <ul>
	 * <li>Destroy blocks emitting light above the configured level.</li>
	 * <li>Extinguish fluids such as lava while playing a sound effect.</li>
	 * </ul>
	 * 
	 * <p>
	 * Although this method performs a potentially expensive 3D scan, the
	 * performance impact is minimal due to the throttled trigger conditions.
	 *
	 * @param pos    the block position to scan around
	 * @param sLevel the server level containing the block
	 * @return true if any lights or lava were destroyed
	 */

	public static boolean destroyLightingNearSpawner(SpawnerBlockEntity sbe) {

		Level level = sbe.getLevel();
		if ((level == null) || (!(level instanceof ServerLevel serverLevel)))
			return false;

		if (serverLevel.dimensionType().monsterSpawnBlockLightLimit() == 15)
			return false;

		BlockPos pos = sbe.getBlockPos();

		if (serverLevel.getBrightness(LightLayer.SKY, pos) == 15)
			return false;

		if (serverLevel.getMaxLocalRawBrightness(pos) < 1)
			return false;

		RandomSource rand = serverLevel.getRandom();
		int fYmin = Math.max((int) pos.getY() - 4, serverLevel.getMinY()); // 4 lower but not outside the world.
		int fYmax = Math.min((int) pos.getY() + 8, serverLevel.getHeight()); // 8 higher but not outside the world

		int scanSize = MyConfig.getDestroyLightRange();
		int lavaScanBoost = 0;
		boolean destroyedLight = false;
		boolean destroyedLava = false;
		int customLightLevel = MyConfig.getHostileSpawnerLightLevel();

		MutableBlockPos mutPos = pos.mutable();
		for (int dy = fYmin; dy <= fYmax + scanSize + lavaScanBoost; dy++) {
			for (int dx = pos.getX() - scanSize; dx <= pos.getX() + scanSize; dx++) {
				for (int dz = pos.getZ() - scanSize; dz <= pos.getZ() + scanSize; dz++) {

					if (rand.nextInt(100) > MyConfig.getDestroyLightPercentage())
						continue;

					mutPos.setX(dx);
					mutPos.setY(dy);
					mutPos.setZ(dz);

					BlockState bs = serverLevel.getBlockState(mutPos);
					Block b = bs.getBlock();
					// Debugging Code
					// SimpleParticleType particles = ParticleTypes.END_ROD;
					// slevel.sendParticles(particles, dx, dy, dz, 3, 0, 0, 0, -0.04D);
					int blockLightLevel = checkAdjustedBlockBrightness(serverLevel, bs, b, mutPos);

					if (blockLightLevel > customLightLevel) {
						if (b != Blocks.END_PORTAL) {
							serverLevel.destroyBlock(mutPos, true);
							destroyedLight = true;
						}
					}
					Fluid f = serverLevel.getFluidState(mutPos).getType();
					if (f.getFluidType().getLightLevel() > 0) {
						serverLevel.playSound(null, mutPos, SoundEvents.LAVA_EXTINGUISH, SoundSource.AMBIENT, 0.9f,
								0.25f);
						serverLevel.setBlock(mutPos, Blocks.AIR.defaultBlockState(), 3);
						lavaScanBoost = 4; // found lava- scan a larger vertical area.
						destroyedLava = true;
						destroyedLight = true;
					}
				}
			}
		}
		if (destroyedLava)
			MyUtilities.debugMsg(2, pos, "Destroyed lava near spawner.");
		return destroyedLight;
	}

	
	public static void applyConfigToMonsterSpawners(SpawnerBlockEntity sbe, CompoundTag spawnerTag) {

		// Local debug level for testing
		int testingDebugLevel = 0;

		MyUtilities.debugMsg(testingDebugLevel,
				"Entering doApplyConfigToMonsterSpawners for spawner at " + sbe.getBlockPos());

		// Nested SpawnData inside the spawner
		Optional<CompoundTag> optSpawnData = spawnerTag.getCompound("SpawnData");
		if (optSpawnData.isEmpty()) {
			MyUtilities.debugMsg(testingDebugLevel, "SpawnData tag is missing, aborting.");
			return;
		}

		CompoundTag spawnDataTag = optSpawnData.get();
		if (spawnDataTag.isEmpty()) {
			MyUtilities.debugMsg(testingDebugLevel, "SpawnData tag is empty, aborting.");
			return;
		}

		// Only apply to monster spawners
		if (!isMonsterSpawner(sbe, spawnerTag)) {
			MyUtilities.debugMsg(testingDebugLevel, "Spawner is not a monster spawner, skipping.");
			return;
		}

		MyUtilities.debugMsg(testingDebugLevel, "Applying configuration overrides to spawner.");

		// Apply configuration overrides directly to the spawner tag
		putIntIfDifferent(spawnerTag, "MaxNearbyEntities", MyConfig.getMaxNearbyEntities());
		MyUtilities.debugMsg(testingDebugLevel, "MaxNearbyEntities set to " + MyConfig.getMaxNearbyEntities());

		putIntIfDifferent(spawnerTag, "RequiredPlayerRange", MyConfig.getRequiredPlayerRange());
		MyUtilities.debugMsg(testingDebugLevel, "RequiredPlayerRange set to " + MyConfig.getRequiredPlayerRange());

		putIntIfDifferent(spawnerTag, "SpawnRange", MyConfig.getSpawnRange());
		MyUtilities.debugMsg(testingDebugLevel, "SpawnRange set to " + MyConfig.getSpawnRange());

		maybeOverrideSpawnDelays(spawnerTag);
		MyUtilities.debugMsg(testingDebugLevel, "Spawn delays processed with maybeOverrideSpawnDelays.");

		// Optionally rebuild SpawnData with custom light levels
		Optional<Tag> workSpawnData = ProcessSpawners.buildCustomLightLevelSpawnData(spawnDataTag);
		if (workSpawnData.isPresent() && !spawnDataTag.equals(workSpawnData.get())) {
			spawnerTag.put("SpawnData", workSpawnData.get());
			MyUtilities.debugMsg(testingDebugLevel, "SpawnData tag updated with custom light level spawn data.");
		} else {
			MyUtilities.debugMsg(testingDebugLevel, "SpawnData tag unchanged after custom light level processing.");
		}

		// Save back to spawner
		loadSpawnerFromTag(sbe, spawnerTag);
		MyUtilities.debugMsg(testingDebugLevel, "Spawner NBT loaded back into spawner block entity.");
	}

	private static void maybeOverrideSpawnDelays(CompoundTag tag) {

		int testingDebugLevel = 0;

		// Check if both min/max are vanilla
		boolean isVanilla = isSpawnerDelayVanilla(tag);

		// Preserve check
		if (MyConfig.isPreserveNonVanillaSpawnerTiming() && !isVanilla) {
			MyUtilities.debugMsg(testingDebugLevel,
					"PreserveNonVanillaSpawnerTiming is true and delays are non-vanilla, skipping override.");
			return;
		}
		MyUtilities.debugMsg(testingDebugLevel, "Overriding vanilla spawner delays.");
		 
		putIntIfDifferent(tag, "MinSpawnDelay", MyConfig.getMinSpawnDelayOverride());
		MyUtilities.debugMsg(testingDebugLevel, "MinSpawnDelay overridden to " + MyConfig.getMinSpawnDelayOverride());

		putIntIfDifferent(tag, "MaxSpawnDelay", MyConfig.getMaxSpawnDelayOverride());
		MyUtilities.debugMsg(testingDebugLevel, "MaxSpawnDelay overridden to " + MyConfig.getMaxSpawnDelayOverride());

	}

	private static boolean isSpawnerDelayVanilla(CompoundTag tag) {
		Optional<Integer> optMin = tag.getInt("MinSpawnDelay");
		Optional<Integer> optMax = tag.getInt("MaxSpawnDelay");
		return optMin.isPresent() && optMin.get() == 200 && optMax.isPresent() && optMax.get() == 800;
	}

	public static int getIntOrDefault(CompoundTag tag, String key, int defaultValue) {
	    Optional<Integer> opt = tag.getInt(key);
	    return opt.orElse(defaultValue);
	}
	
	private static void putIntIfDifferent(CompoundTag tag, String key, int value) {
		Optional<Integer> opt = tag.getInt(key);
		if (opt.isEmpty() || opt.get() != value)
			tag.putInt(key, value);
	}
	
	
	/** 
	 * Returns true if the spawner contains a monster-type entity. 
	 */
	public static boolean isMonsterSpawner(SpawnerBlockEntity sbe, CompoundTag spawnerTag) {

		if (spawnerTag == null) {
			return false;
		}

		boolean returnVal = spawnerTag.getCompound("SpawnData").flatMap(spawnData -> spawnData.getCompound("entity"))
				.flatMap(entityData -> entityData.getString("id")).flatMap(EntityType::byString)
				.map(eType -> eType.getCategory() == MobCategory.MONSTER).orElse(false);

		return returnVal;
	}
	
	public static void logSpawnerCompoundTag(SpawnerBlockEntity sbe, String context) {
	    if (sbe == null) return;

	    CompoundTag spawnerTag = SharedUtilityMethods.saveSpawnerToTag(sbe);
	    BlockPos pos = sbe.getBlockPos();

	    StringBuilder sb = new StringBuilder();
	    sb.append("{");

	    for (Map.Entry<String, Tag> entry : spawnerTag.entrySet()) {
	        sb.append(entry.getKey())
	          .append("=")
	          .append(entry.getValue())
	          .append(", ");
	    }

	    if (sb.length() > 1) sb.setLength(sb.length() - 2); // remove trailing comma
	    sb.append("}");

	    MyUtilities.debugMsg(0, pos, context + " Spawner CompoundTag: " + sb.toString());
	}
	
	public static String makeSpawnerCompoundTagReport(SpawnerBlockEntity sbe) {
	    if (sbe == null) return "<null spawner>";

	    CompoundTag spawnerTag = SharedUtilityMethods.saveSpawnerToTag(sbe);
	    BlockPos pos = sbe.getBlockPos();

	    StringBuilder sb = new StringBuilder();
	    sb.append("Spawner @ ").append(pos).append("\n");
	    sb.append("{\n");

	    for (Map.Entry<String, Tag> entry : spawnerTag.entrySet()) {
	        String key = entry.getKey();

	        // ---- formatting rules ----
	        if ("entity".equals(key)
	         || "custom_spawn_rules".equals(key)) {
	            sb.append("\n");
	        }

	        sb.append("  ")
	          .append(key)
	          .append(" = ")
	          .append(entry.getValue())
	          .append("\n");
	    }

	    sb.append("}");
	    return sb.toString();
	}


}
