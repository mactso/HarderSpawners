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

//    public static SpawnerDurabilityItem getMobSpawnerSpawnsCountByMobType(EntityType<?> entityType) {
//        if (SpawnerDurabilityRangeByMobType.isEmpty()) {
//            init();
//        }
//
//        // Use BuiltInRegistries to get the ResourceLocation for this EntityType
//        ResourceLocation key = BuiltInRegistries.ENTITY_TYPE.getKey(entityType);
//
//        SpawnerDurabilityItem t = null;
//        if (key != null) {
//            t = SpawnerDurabilityRangeByMobType.get(key.toString());
//        }
//
//        if (t == null) {
//            t = SpawnerDurabilityRangeByMobType.get(defaultKey);
//        }
//
//        return t;
//    }

	
	
    public static void init() {

        SpawnerDurabilityRangeByMobType.clear();

        MyUtilities.debugMsg(0, "Harder Spawners: Initializing Spawner Durability Settings.");

        String configLine;
        StringTokenizer lines = new StringTokenizer(MyConfig.getMobSpawnerDurabilityRangesString(), ";");

        while (lines.hasMoreElements()) {
            configLine = lines.nextToken().trim();
            if (configLine.isEmpty()) continue;

            try {
                StringTokenizer st = new StringTokenizer(configLine, ",");
                String key = st.nextToken().trim(); // modid:mobid or "harderspawners:default"

                if (!key.equals(defaultKey)) {
                    ResourceLocation entityKey = ResourceLocation.tryParse(key);
                    if (entityKey == null || BuiltInRegistries.ENTITY_TYPE.get(entityKey) == null) {
                        MyUtilities.debugMsg(0, "WARN : Harder Spawners : Undefined Mob : " + configLine);
                    }
                }

                int minSpawns = Integer.parseInt(st.nextToken().trim());
                if (minSpawns < 0) minSpawns = 0;

                int maxSpawns = Integer.parseInt(st.nextToken().trim());
                if (maxSpawns < minSpawns) maxSpawns = minSpawns;

                SpawnerDurabilityRangeByMobType.put(key, new SpawnerDurabilityItem(minSpawns, maxSpawns));

            } catch (Exception e) {
                MyUtilities.debugMsg(0, "ERROR: Harder Spawners : Bad Mob Config Line : " + configLine);
            }
        }

        MyUtilities.debugMsg(0, "Harder Spawners: Spawner Durability Settings Initialization complete.");
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
