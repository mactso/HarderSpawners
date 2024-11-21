package com.mactso.harderspawners.config;

import java.util.Hashtable;
import java.util.Optional;
import java.util.Random;
import java.util.StringTokenizer;

import com.mactso.harderspawners.util.Utility;

import net.minecraft.world.entity.EntityType;
import net.minecraftforge.registries.ForgeRegistries;

public class MobSpawnerManager {
	public static Hashtable<String, SpawnerDurabilityItem> SpawnerDurabilityRangeByMobType = new Hashtable<>();
	private static String defaultKey = "harderspawners:default";

	public static SpawnerDurabilityItem getMobSpawnerSpawnsCountByMobType(EntityType<?> entityType) {
		if (SpawnerDurabilityRangeByMobType.isEmpty()) {
			init();
		}
		
		SpawnerDurabilityItem t = SpawnerDurabilityRangeByMobType.get(ForgeRegistries.ENTITY_TYPES.getKey(entityType).toString());
		if (t == null) {
			t = SpawnerDurabilityRangeByMobType.get(defaultKey);
		}
		return t;
	}

	
	
	public static void init() {

		SpawnerDurabilityRangeByMobType.clear();

		Utility.debugMsg(0,"Harder Spawners: Initializing Spawner Durability Settings.");

		String oneLine = "";
		// Forge Issue 6464 patch.
		StringTokenizer lines = new StringTokenizer(MyConfig.getMobSpawnerDurabilityRangesString(), ";");
		while (lines.hasMoreElements()) {
			oneLine = lines.nextToken().trim();
			if (!oneLine.isEmpty()) {
				try {
					
					StringTokenizer st = new StringTokenizer(oneLine, ",");

					String key = st.nextToken(); // mod and mob
					if (!key.equals(defaultKey)) {
						Optional<EntityType<?>> etOpt = EntityType.byString(key);
						if (etOpt.isEmpty()) {
							Utility.debugMsg(0, "WARN : Harder Spawners :  Undefined Mob : " + oneLine);
						}
					}

					int minSpawns = Integer.parseInt(st.nextToken().trim());
					if (minSpawns < 0) {
						minSpawns = 0;
					}

					int maxSpawns = Integer.parseInt(st.nextToken().trim());
					if (maxSpawns < minSpawns) {
						maxSpawns = minSpawns;
					}

					SpawnerDurabilityRangeByMobType.put(key, new SpawnerDurabilityItem(minSpawns, maxSpawns));

				} catch (Exception e) {
					Utility.debugMsg(0, "ERROR: Harder Spawners :  Bad Mob Config Line : " + oneLine);
				}

			}

		}

		Utility.debugMsg(0,"Harder Spawners: Spawner Durability Settings Initialization complete.");

	}

	// keeps track of the spawner durability by Mob Type.
	public static class SpawnerDurabilityItem {
		int minimumDurability;
		int maximumDurability;

		public SpawnerDurabilityItem(int minimumSpawnsIn, int maximumSpawnsIn) {
			this.minimumDurability = minimumSpawnsIn;
			this.maximumDurability = maximumSpawnsIn;
		}

		
		public boolean isInfiniteDurability () {
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
