//package com.mactso.harderspawners.common.logic;
//
//import com.mactso.harderspawners.common.utility.MyUtilities;
//import com.mactso.harderspawners.modloader.config.MyConfig;
//
//public class BlockBreak {
//    /**
//     * Adjusts the original destroy speed of a block according to the mod's spawner
//     * break speed modifier.
//     * A higher modifier reduces the effective destroy speed for spawners.
//     * Returns the original speed if the modifier is 0 or adjustment results in
//     * non-positive speed.
//     * Includes a debug message to track invocation in development.
//     *
//     * @param originalDestroySpeed the destroy speed calculated by vanilla
//     * @return the modified destroy speed after applying config modifier
//     */
//	// your existing logic method
//	public static float doBreakSpeedAdjustment(float originalDestroySpeed) {
//		float baseDestroySpeed = originalDestroySpeed;
//		float newDestroySpeed = baseDestroySpeed;
//		if (MyConfig.isDebug())
//			MyUtilities.debugMsg(1, "dobreakSpeedAdjustment");
//		
//		// clamped -1..n where -1 means "do not adjust speed" 
//		if (MyConfig.getSpawnerBreakSpeedModifier() > 0) {
//			newDestroySpeed = newDestroySpeed / (1 + MyConfig.getSpawnerBreakSpeedModifier());
//			if (newDestroySpeed > 0) {
//				return newDestroySpeed;
//			}
//		}
//		return originalDestroySpeed;
//
//	}
//
//}
