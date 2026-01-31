package com.mactso.harderspawners.common.logic;

import java.util.Iterator;
import java.util.List;

import com.mactso.harderspawners.common.utility.MyUtilities;
import com.mactso.harderspawners.modloader.config.MyConfig;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;

public final class ExplosionDetonateEventLogic {

	private ExplosionDetonateEventLogic() {
		/* prevent accidental instantiation */
	}

	/**
	 * Removes spawner blocks from an explosion's affected block list.
	 *
	 * @param level  Server level in which the explosion occurs
	 * @param blocks Mutable list of block positions selected for destruction
	 */
	public static void protectSpawnersFromExplosion(ServerLevel level, List<BlockPos> blocks) {
		Iterator<BlockPos> iterator = blocks.iterator();

		if (MyConfig.isDebug())
			MyUtilities.debugMsg(1,
					"ExplosionDetonateEventLogic: Filtering explosion list with " + blocks.size() + " entries.");

		while (iterator.hasNext()) {
			BlockPos pos = iterator.next();

			if (level.getBlockState(pos).is(Blocks.SPAWNER)) {
				iterator.remove();

				if (MyConfig.isDebug())
					MyUtilities.debugMsg(2, pos, "ExplosionDetonateEventLogic: Removed a spawner from the list.");
			}
		}
	}
}
