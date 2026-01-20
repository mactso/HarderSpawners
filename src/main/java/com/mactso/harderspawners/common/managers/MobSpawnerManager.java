package com.mactso.harderspawners.common.managers;

import java.util.Hashtable;
import java.util.Random;
import java.util.StringTokenizer;

import com.mactso.harderspawners.common.utility.MyUtilities;
import com.mactso.harderspawners.modloader.config.MyConfig;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

public class MobSpawnerManager {
	public static Hashtable<String, SpawnerLifespanItem> SpawnerLifespanRangeByMobType = new Hashtable<>();
	private static final String defaultKey = "harderspawners:default";

	/**
	 * Returns the SpawnerDurabilityItem for the given mob key. Falls back to the
	 * configured default, and if that is missing, uses 50–500.
	 */
	public static SpawnerLifespanItem getLifespanForMob(String mobKey) {
		SpawnerLifespanItem t = SpawnerLifespanRangeByMobType.get(mobKey);

		if (t != null)
			return t;

		// fallback to configured default
		SpawnerLifespanItem defaultItem = SpawnerLifespanRangeByMobType.get(defaultKey);
		if (defaultItem != null)
			return defaultItem;

		// final hardcoded fallback
		MyUtilities.debugMsg(0, "WARNING: No default spawner lifespan configured! Using fallback 200–600.");
		return new SpawnerLifespanItem(150, 650); // this is the number of spawns
	}

	public static void init() {

		SpawnerLifespanRangeByMobType.clear();

		MyUtilities.debugMsg(0, "Harder Spawners: Initializing Spawner Lifespan Settings.");

		String configLine;
		StringTokenizer lines = new StringTokenizer(MyConfig.getMobSpawnerLifespanRangesString(), ";");

		while (lines.hasMoreElements()) {
			configLine = lines.nextToken().trim();
			if (configLine.isEmpty())
				continue;

			try {
				StringTokenizer st = new StringTokenizer(configLine, ",");
				String key = st.nextToken().trim(); // modid:mobid or harderspawners:default

				boolean valid = true;

				if (!key.equals(defaultKey)) {
					Identifier entityKey = Identifier.tryParse(key);
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
					SpawnerLifespanRangeByMobType.put(key, new SpawnerLifespanItem(minSpawns, maxSpawns));
					MyUtilities.debugMsg(0, "Add valid : " + configLine);
				}
			} catch (Exception e) {
				MyUtilities.debugMsg(0, "ERROR: Harder Spawners : Bad Mob Config Line : " + configLine);
			}
		}

		MyUtilities.debugMsg(0, "Harder Spawners: Spawner Durability Settings Initialization complete.");
	}

	// keeps track of the spawner durability by Mob Type.
	public static class SpawnerLifespanItem {
		int minLifespan; // min number of spawns
		int maxLifespan; // max number of spawns

		public SpawnerLifespanItem(int minimumSpawnsIn, int maximumSpawnsIn) {
			this.minLifespan = minimumSpawnsIn;
			this.maxLifespan = maximumSpawnsIn;
		}

		public boolean isInfiniteLifespan() {
			if (minLifespan == 0)
				return true;
			if (maxLifespan == 0)
				return true;
			return false;
		}

		public int initLifespanValue() {

			if (minLifespan == 0)
				return 0;
			if (maxLifespan == 0)
				return 0;
			if (maxLifespan <= minLifespan)
				return 0;

			Random r = new Random();
			int range = maxLifespan - minLifespan;

			return r.nextInt(range) + minLifespan;

		}

		public int getMinLifespan() {
			return minLifespan;
		}

		public double getMaxLifespan() {
			return maxLifespan;
		}

	}

}
