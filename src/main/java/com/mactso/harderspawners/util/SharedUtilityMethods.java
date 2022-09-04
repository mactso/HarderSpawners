package com.mactso.harderspawners.util;

import com.mactso.harderspawners.config.MyConfig;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public class SharedUtilityMethods {

	// note: This *only* runs when someone tries to break a spawner or when the spawner tries to spawn.
	// Other routines catch when a block is placed.
	public static boolean removeLightNearSpawner(BlockPos pos, ServerLevel serverWorld) {
		boolean destroyedLight = false;
		RandomSource rand = serverWorld.getRandom();
		if (serverWorld.getMaxLocalRawBrightness(pos) > 6) {
			int fYmin = (int) pos.getY() - 4;
			if (fYmin < 1)
				fYmin = 1;
			int fYmax = (int) pos.getY() + 4;
			if (fYmax > 254)
				fYmin = 254;
			int scanSize = MyConfig.getDestroyLightRange();

			for (int dy = fYmin; dy <= fYmax; dy++) {
				for (int dx = pos.getX() - scanSize; dx <= pos.getX() + scanSize; dx++) {
					for (int dz = pos.getZ() - scanSize; dz <= pos.getZ() + scanSize; dz++) {
						BlockPos bP = new BlockPos(dx, dy, dz);
						Block b = serverWorld.getBlockState(bP).getBlock();
						int blockLightLevel = serverWorld.getBlockState(bP).getLightEmission(serverWorld,bP);
						if ((blockLightLevel > 7) && (rand.nextInt(100) <= MyConfig.getDestroyLightPercentage())) {
							if (b != Blocks.END_PORTAL) {
								serverWorld.destroyBlock(bP, true);
								destroyedLight = true;
							}
						}
						if ((b == Blocks.LAVA)&& (rand.nextInt(100) <= MyConfig.getDestroyLightPercentage())) {
							serverWorld.setBlock(bP, Blocks.AIR.defaultBlockState(), 3);
							destroyedLight = true;
						}
					}
				}
			}
		}
		return destroyedLight;
	}

}
