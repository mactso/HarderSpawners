package com.mactso.harderspawners.util;

import com.mactso.harderspawners.capabilities.CapabilitySpawner;
import com.mactso.harderspawners.capabilities.ISpawnerStatsStorage;
import com.mactso.harderspawners.config.MyConfig;

import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
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

public class SharedUtilityMethods {

    public static boolean isSpawnerStunned(SpawnerBlockEntity sbe) {
		ISpawnerStatsStorage cap = sbe.getCapability(CapabilitySpawner.SPAWNER_STORAGE).orElse(null);
		if (cap != null) {
			if (cap.isStunned())
				return true;
		}
		return false;
	}

	public static int adjustBlockBrightness(Level world, BlockState placedBlockState, Block placedBlock, BlockPos placedPos) {

		if (placedBlock instanceof RedstoneLampBlock)
			return 15;
		if (placedBlock instanceof PoweredBlock)
			return 15;
		if (placedBlock instanceof RedStoneWireBlock)
			return 15;
		if (placedBlock instanceof LeverBlock)
			return 15;
		if (placedBlock instanceof RepeaterBlock)
			return 15;
		if (placedBlock instanceof ComparatorBlock)
			return 15;

		return placedBlockState.getLightEmission(world, placedPos);

	}
	
	// note: This *only* runs when someone tries to break a spawner or when the
	// spawner tries to spawn.
	// Other routines catch when a block is placed.
	public static boolean doDestroyLightsNearBlockPos(BlockPos pos, ServerLevel slevel) {

		if (slevel.dimensionType().monsterSpawnBlockLightLimit() == 15) 
			return false;

		if (slevel.getBrightness(LightLayer.SKY, pos) == 15)
			return false;

		if (slevel.getMaxLocalRawBrightness(pos) < 1) 
			return false;

		boolean destroyedLight = false;
		RandomSource rand = slevel.getRandom();
		int fYmin = (int) pos.getY() - 4;

		if (fYmin < slevel.getMinBuildHeight())
			fYmin = slevel.getMinBuildHeight();
		int fYmax = (int) pos.getY() + 4;
		if (fYmax > slevel.getMaxBuildHeight())
			;
		fYmax = slevel.getMaxBuildHeight();
		int scanSize = MyConfig.getDestroyLightRange();
		int lavaScanSize = scanSize+4;
		boolean destroyedLava = false;
		MutableBlockPos mutPos = pos.mutable();
		int customLightLevel = MyConfig.getHostileSpawnerLightLevel();
		for (int dy = fYmin; dy <= fYmax; dy++) {
			for (int dx = pos.getX() - scanSize; dx <= pos.getX() + scanSize; dx++) {
				for (int dz = pos.getZ() - scanSize; dz <= pos.getZ() + scanSize; dz++) {
					mutPos.setX(dx);
					mutPos.setY(dy);
					mutPos.setZ(dz);

					BlockState bs = slevel.getBlockState(mutPos);
					Block b = bs.getBlock();
					// Debugging Code
					//					SimpleParticleType particles = ParticleTypes.END_ROD;
					//					slevel.sendParticles(particles, dx, dy, dz, 3, 0, 0, 0, -0.04D);
					int blockLightLevel =  adjustBlockBrightness(slevel, bs, b, mutPos);
							
					if ((blockLightLevel > customLightLevel) && (rand.nextInt(100) <= MyConfig.getDestroyLightPercentage())) {
						if (b != Blocks.END_PORTAL) {
							slevel.destroyBlock(mutPos, true);
							destroyedLight = true;
						}
					}
					Fluid f = slevel.getFluidState(mutPos).getType();
					if ((f.getFluidType().getLightLevel() > 0)
							&& (rand.nextInt(100) <= MyConfig.getDestroyLightPercentage())) {
						slevel.playSound(null, mutPos, SoundEvents.LAVA_EXTINGUISH, SoundSource.AMBIENT, 0.9f, 0.25f);
						slevel.setBlock(mutPos, Blocks.AIR.defaultBlockState(), 3);
						scanSize = lavaScanSize;
						destroyedLava = true;
						destroyedLight = true;
					}
				}
			}
		}
		return destroyedLight;
	}
	


}
