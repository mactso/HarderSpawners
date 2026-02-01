package com.mactso.harderspawners.common.logic;

import java.util.Optional;
import java.util.Set;

import com.mactso.harderspawners.common.utility.MyUtilities;
import com.mactso.harderspawners.common.utility.SpawnerUtilityMethods;
import com.mactso.harderspawners.modloader.config.MyConfig;
import com.mactso.harderspawners.modloader.spawnerstorage.SpawnerStatsAdapter.SpawnerStatsWrapper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.InclusiveRange;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentTable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.SpawnData;
import net.minecraft.world.level.SpawnData.CustomSpawnRules;
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
import net.minecraft.world.level.material.FluidState;

/**
 * Contains logic for handling blocks and fluids above spawners.
 */
public class SpawnerLightLogic {

	public static final Set<Class<? extends Block>> ALWAYS_MAX_LIGHT = Set.of(RedstoneLampBlock.class,
			PoweredBlock.class, RedStoneWireBlock.class, LeverBlock.class, RepeaterBlock.class, ComparatorBlock.class);

	/**
	 * Checks all light above a spawner in the given level for blocks above them
	 * that may interfere with mob spawning (light or fluids) and handles them.
	 */
	public static void removeLightSourcesAboveASpawner(ServerLevel serverLevel, SpawnerBlockEntity sbe) {

		if (sbe.isRemoved())
			return;
		BlockPos spawnerPos = sbe.getBlockPos();
		int monsterSpawnBlockLightLimit = serverLevel.dimensionType().monsterSpawnBlockLightLimit();
		int blockLightAtSpawner = serverLevel.getBrightness(LightLayer.BLOCK, spawnerPos);

		if (blockLightAtSpawner < monsterSpawnBlockLightLimit) {
			return;
		}

		BlockPos.MutableBlockPos mutPos = new BlockPos.MutableBlockPos();

		int x = spawnerPos.getX();
		int y = spawnerPos.getY();
		int z = spawnerPos.getZ();

		// Scan up to 16 blocks above the spawner
		for (int yOffset = 1; yOffset <= 16; yOffset++) {
			mutPos.set(x, y + yOffset, z);
			removeABrightBlockAboveSpawner(serverLevel, monsterSpawnBlockLightLimit, mutPos);
		}
	}

	public static void removeABrightBlockAboveSpawner(ServerLevel serverLevel, int spawnLightLevel, BlockPos testPos) {

		BlockState testState = serverLevel.getBlockState(testPos);

		if (getAdjustedBlockStateLightEmission(testState) <= spawnLightLevel) {
			return;
		}
		if (testState.getLightEmission() < spawnLightLevel && testState.getBlock() != Blocks.REDSTONE_LAMP) {
			return;
		}

		FluidState fluidState = serverLevel.getFluidState(testPos);

		// Destroy offending bright block
		if (!fluidState.isEmpty()) {
			if (fluidState.isSource()) {
				serverLevel.setBlock(testPos, Blocks.COBBLESTONE.defaultBlockState(), 3);
			} else {
				serverLevel.setBlock(testPos, Blocks.AIR.defaultBlockState(), 3);
			}
			serverLevel.playSound(null, testPos, SoundEvents.LAVA_EXTINGUISH, SoundSource.AMBIENT, 0.9f, 0.25f);

		} else {
			serverLevel.destroyBlock(testPos, true);
		}
	}

	/**
	 * Adjusts the effective light level of a block, taking certain redstone
	 * components into account (which can provide maximum brightness regardless of
	 * the normal light emission).
	 *
	 * @param world          the level containing the block
	 * @param testBlockState the state of the block
	 * @param placedBlock    the block type
	 * @param placedPos      the position of the block
	 * @return the effective light emission of the block
	 */
	public static int getAdjustedBlockStateLightEmission(BlockState testBlockState) {

		if (SpawnerLightLogic.ALWAYS_MAX_LIGHT.contains(testBlockState.getBlock().getClass()))
			return 15;

		return testBlockState.getLightEmission();

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

		// 4 lower but not outside the world.
		int fYmin = (int) pos.getY() - 4;
		if (fYmin < serverLevel.getMinY())
			fYmin = serverLevel.getMinY();
		// 8 higher but not outside the world
		int fYmax = (int) pos.getY() + 8;
		if (fYmax > serverLevel.getMaxY())
		fYmax = serverLevel.getMaxY();

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
					int blockLightLevel = getAdjustedBlockStateLightEmission(bs);

					if (blockLightLevel > customLightLevel) {
						if (b != Blocks.END_PORTAL) {
							serverLevel.destroyBlock(mutPos, true);
							destroyedLight = true;
						}
					}
					// Fluidstate is minmecraft. Fluidstate.lightEmission is forge.
				}
			}
		}
		if (destroyedLava)
			MyUtilities.debugMsg(2, pos, "Destroyed lava near spawner.");
		return destroyedLight;
	}

	/**
	 * Rebuilds SpawnData with custom light-level spawn rules applied. Preserves
	 * existing entity data and equipment. Uses config-defined hostile spawner light
	 * level. Returns encoded SpawnData tag if entity data exists. Returns
	 * empty/absent result if SpawnData is invalid or missing entity.
	 */
	public static Optional<Tag> buildCustomLightLevelSpawnData(CompoundTag spawnDataTag) {

		SpawnData spawndata = SpawnData.CODEC.parse(NbtOps.INSTANCE, spawnDataTag)
				.resultOrPartial(p_186391_ -> ProcessSpawners.LOGGER.warn("Invalid SpawnData: {}", p_186391_))
				.orElseGet(SpawnData::new);
		Optional<EquipmentTable> equipment = spawndata.equipment();

		int lightLevel = MyConfig.getHostileSpawnerLightLevel();
		int blocklight = lightLevel; // The light level
		int skylight = lightLevel;
		CustomSpawnRules c = new SpawnData.CustomSpawnRules(new InclusiveRange<Integer>(0, blocklight),
				new InclusiveRange<Integer>(0, skylight));

		Optional<CompoundTag> optEntityTag = spawnDataTag.getCompound("entity");
		if (optEntityTag.isEmpty())
			return null;
		CompoundTag entityTag = optEntityTag.get();

		SpawnData s = new SpawnData(entityTag, Optional.of(c), equipment);

		return SpawnData.CODEC.encodeStart(NbtOps.INSTANCE, s).result();

	}

	/**
	 * Reapplies configured spawn light-level rules to a spawner. Used defensively
	 * in case other mods modify SpawnData light limits. Rebuilds SpawnData via
	 * codec to preserve entity and equipment data. Reloads the updated tag into
	 * BaseSpawner immediately before spawn. No-op if SpawnData or entity data is
	 * missing.
	 */
	public static void reapplyCustomLightRules(SpawnerBlockEntity sbe, CompoundTag spawnerTag,
			SpawnerStatsWrapper statsWrapper) {

		Optional<CompoundTag> optSpawnDataTag = spawnerTag.getCompound("SpawnData");
		if (optSpawnDataTag.isEmpty())
			return;
		CompoundTag spawnDataTag = optSpawnDataTag.get();

		if (spawnDataTag.isEmpty()) // overly defensive code. if we are here, it's not empty.
			return;

		// Use your existing codec-based method to build the new tag
		Optional<Tag> workSpawnData = buildCustomLightLevelSpawnData(spawnDataTag);
		if (workSpawnData == null)
			return;
		if (workSpawnData.isPresent()) {
			spawnerTag.put("SpawnData", workSpawnData.get());
			// Load the modified tag back into the internal BaseSpawner logic
			SpawnerUtilityMethods.loadSpawnerFromTag(sbe, spawnerTag);

			sbe.setChanged();
			MyUtilities.debugMsg(2, "JIT: Light levels reapplied to " + sbe.getBlockPos().toShortString());
		}

	}
	
    /**
     * Checks if a hostile mob may spawn at the given position.
     * Considers both block light and effective sunlight (time of day, weather, moon phase).
     *
     * @param level the server level
     * @param pos   the block position
     * @return true if spawning is allowed, false otherwise
     */
    public static boolean canSpawnAt(ServerLevel level, BlockPos pos) {
        int blockLight = level.getBrightness(LightLayer.BLOCK, pos);
        int maxBlockLight = MyConfig.getHostileSpawnerLightLevel();

        if (blockLight > maxBlockLight) {
            MyUtilities.debugMsg(1, "[SpawnerLightLogic] Spawn blocked. Blocklight [" + blockLight + "] > " + maxBlockLight);
            return false;
        }

        int effectiveSunlight = getEffectiveSunlight(level, pos);
        int maxSunlight = MyConfig.getHostileSpawnerLightLevel();

        if (effectiveSunlight > maxSunlight) {
            MyUtilities.debugMsg(1, "[SpawnerLightLogic] Spawn blocked. Effective sunlight [" + effectiveSunlight + "] > " + maxSunlight);
            return false;
        }

        MyUtilities.debugMsg(1, "[SpawnerLightLogic] Spawn allowed. Blocklight [" + blockLight + "] <= " + maxBlockLight
                + ", Effective sunlight [" + effectiveSunlight + "] <= " + maxSunlight);
        return true;
    }

	/**
	 * Returns the effective sunlight at a block, adjusted for time of day and
	 * weather. This mimics the vanilla mob spawning check.
	 *
	 * @param level the server level
	 * @param pos   the block position
	 * @return skylight 0-15 after weather/time adjustment
	 */
	public static int getEffectiveSunlight(ServerLevel level, BlockPos pos) {
		// Raw skylight (0-15)
		int skyLight = level.getBrightness(LightLayer.SKY, pos);
		int skyDarken = level.getSkyDarken(); // 0-15
		// TODO verify this still works the same way.
		skyLight = skyLight - skyDarken;
		// Apply weather adjustments
		if (level.isThundering()) {
			skyLight -= 7; // thunder reduces sky light for spawn checks
		} else if (level.isRaining()) {
			skyLight -= 3; // rain reduces sky light for spawn checks
		}

		// TODO look at old isDay().   Consider !(isMoonVisible()) or time of day.
		if (!level.isBrightOutside()) {

			int moonPhase =  level.getMoonPhase();
			int moonDarken = 4 - Math.abs(4 - moonPhase);
			skyLight -= moonDarken;
		}
		// Clamp to 0-15
		if (skyLight < 0)
			skyLight = 0;
		if (skyLight > 15)
			skyLight = 15;

		return skyLight;
	}

}