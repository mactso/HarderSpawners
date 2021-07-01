package com.mactso.harderspawners.util;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.server.ServerWorld;

public class SharedUtilityMethods {

	public static boolean removeLightNearSpawner(BlockPos pos, ServerWorld serverWorld) {
		boolean destroyedLight = false;
		if (serverWorld.getLight(pos) > 6) {
			int fYmin = (int) pos.getY() - 4;
			if (fYmin < 1)
				fYmin = 1;
			int fYmax = (int) pos.getY() + 4;
			if (fYmax > 254)
				fYmin = 254;
			int scanSize = 7;

			for (int dy = fYmin; dy <= fYmax; dy++) {
				for (int dx = pos.getX() - scanSize; dx <= pos.getX() + scanSize; dx++) {
					for (int dz = pos.getZ() - scanSize; dz <= pos.getZ() + scanSize; dz++) {
						BlockPos bP = new BlockPos(dx, dy, dz);
						Block b = serverWorld.getBlockState(bP).getBlock();
						int blockLightLevel = serverWorld.getBlockState(bP).getLightValue();
						if ((blockLightLevel > 7)) {
							if (b != Blocks.END_PORTAL) {
								serverWorld.destroyBlock(bP, true);
								destroyedLight = true;
							}
						}
						if (b == Blocks.LAVA) {
							serverWorld.setBlockState(bP, Blocks.AIR.getDefaultState(), 3);
							destroyedLight = true;
						}
					}
				}
			}
		}
		return destroyedLight;
	}

}
