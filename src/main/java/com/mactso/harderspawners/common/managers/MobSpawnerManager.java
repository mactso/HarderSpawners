package com.mactso.harderspawners.common.managers;

import java.util.Hashtable;
import java.util.Random;
import java.util.StringTokenizer;

import com.mactso.harderspawners.common.utility.MyUtilities;
import com.mactso.harderspawners.modloader.config.MyConfig;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

public class MobSpawnerManager {
	public static Hashtable<String, SpawnerDurabilityItem> SpawnerDurabilityRangeByMobType = new Hashtable<>();
	private static final String defaultKey = "harderspawners:default";

	/**
	 * Returns the SpawnerDurabilityItem for the given mob key. Falls back to the
	 * configured default, and if that is missing, uses 50–500.
	 */
	public static SpawnerDurabilityItem getDurabilityForMob(String mobKey) {
		SpawnerDurabilityItem t = SpawnerDurabilityRangeByMobType.get(mobKey);

		if (t != null)
			return t;

		// fallback to configured default
		SpawnerDurabilityItem defaultItem = SpawnerDurabilityRangeByMobType.get(defaultKey);
		if (defaultItem != null)
			return defaultItem;

		// final hardcoded fallback
		MyUtilities.debugMsg(0, "WARNING: No default spawner durability configured! Using fallback 200–600.");
		return new SpawnerDurabilityItem(200, 600);
	}

	public static void init() {

		SpawnerDurabilityRangeByMobType.clear();

		MyUtilities.debugMsg(0, "Harder Spawners: Initializing Spawner Durability Settings.");

		String configLine;
		StringTokenizer lines = new StringTokenizer(MyConfig.getMobSpawnerDurabilityRangesString(), ";");

		while (lines.hasMoreElements()) {
			configLine = lines.nextToken().trim();
			if (configLine.isEmpty())
				continue;

			try {
				StringTokenizer st = new StringTokenizer(configLine, ",");
				String key = st.nextToken().trim(); // modid:mobid or harderspawners:default

				boolean valid = true;

				if (!key.equals(defaultKey)) {
					ResourceLocation entityKey = ResourceLocation.tryParse(key);
					if (entityKey == null || !BuiltInRegistries.ENTITY_TYPE.containsKey(entityKey)) {
						MyUtilities.debugMsg(0, "WARN : Harder Spawners : Undefined Mob : " + configLine);
						valid = false;
					}
				}

				int minSpawns = Integer.parseInt(st.nextToken().trim());
				if (minSpawns < 0)
					minSpawns = 0;

				int maxSpawns = Integer.parseInt(st.nextToken().trim());
				if (maxSpawns < minSpawns)
					maxSpawns = minSpawns;

				if (valid) {
					SpawnerDurabilityRangeByMobType.put(key, new SpawnerDurabilityItem(minSpawns, maxSpawns));
					MyUtilities.debugMsg(0, "Add valid : " + configLine);
				}
			} catch (Exception e) {
				MyUtilities.debugMsg(0, "ERROR: Harder Spawners : Bad Mob Config Line : " + configLine);
			}
		}

		MyUtilities.debugMsg(0, "Harder Spawners: Spawner Durability Settings Initialization complete.");
	}

	// keeps track of the spawner durability by Mob Type.
	public static class SpawnerDurabilityItem {
		int minimumDurability; // min number of spawns
		int maximumDurability; // max number of spawns

		public SpawnerDurabilityItem(int minimumSpawnsIn, int maximumSpawnsIn) {
			this.minimumDurability = minimumSpawnsIn;
			this.maximumDurability = maximumSpawnsIn;
		}

		public boolean isInfiniteDurability() {
			if (minimumDurability == 0)
				return true;
			if (maximumDurability == 0)
				return true;
			return false;
		}

		public int initDurabilityValue() {

			if (minimumDurability == 0)
				return 0;
			if (maximumDurability == 0)
				return 0;
			if (maximumDurability <= minimumDurability)
				return 0;

			Random r = new Random();
			int range = maximumDurability - minimumDurability;

			return r.nextInt(range) + minimumDurability;

		}

		public int getMinimumDurability() {
			return minimumDurability;
		}

		public double getMaximumdurability() {
			return maximumDurability;
		}

	}

}
