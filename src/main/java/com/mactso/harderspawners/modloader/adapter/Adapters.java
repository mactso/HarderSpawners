package com.mactso.harderspawners.modloader.adapter;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.level.BaseSpawner;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;

/**
 * Adapter to read BaseSpawner.spawnDelay in Fabric 1.21.1.
 * 
 * Tries in order:
 * 1) Mixin accessor (preferred)
 * 2) Reflection using deobfuscated name ("spawnDelay")
 * 3) Reflection using Fabric MappingResolver to handle obfuscated environments
 */
public class Adapters {
    private static final Logger LOGGER = LogManager.getLogger();

    // Cached reflective field
    private static Field SPAWN_DELAY_FIELD;

    static {
        SPAWN_DELAY_FIELD = findField();
    }

    private static Field findField() {
        // First try deobfuscated name directly
        try {
            Field f = BaseSpawner.class.getDeclaredField("spawnDelay");
            f.setAccessible(true);
            LOGGER.debug("HarderSpawners: Found BaseSpawner.spawnDelay via direct name.");
            return f;
        } catch (NoSuchFieldException ignored) {}

        // Fallback: use Fabric MappingResolver
        try {
            String obfName = FabricLoader.getInstance()
                    .getMappingResolver()
                    .mapFieldName("intermediary", "net.minecraft.world.level.BaseSpawner", "spawnDelay", "I");
            if (obfName != null) {
                Field f = BaseSpawner.class.getDeclaredField(obfName);
                f.setAccessible(true);
                LOGGER.debug("HarderSpawners: Found BaseSpawner.spawnDelay via MappingResolver: " + obfName);
                return f;
            }
        } catch (Exception e) {
            LOGGER.warn("HarderSpawners: Could not find BaseSpawner.spawnDelay with MappingResolver.", e);
        }

        LOGGER.error("HarderSpawners: Failed to find BaseSpawner.spawnDelay! spawnDelay access will not work.");
        return null;
    }

    /**
     * Returns the spawn delay of a BaseSpawner instance.
     * Tries Mixin accessor first, then reflection fallback.
     * @param spawner BaseSpawner instance
     * @return spawnDelay value or Integer.MIN_VALUE on failure
     */
    public static int getDelay(BaseSpawner spawner) {
        if (spawner == null) return Integer.MIN_VALUE;

        // 1) Try Mixin accessor
        try {
            if (spawner instanceof com.mactso.harderspawners.mixin.BaseSpawnerAccessor accessor) {
                return accessor.harderSpawners$getSpawnDelay();
            }
        } catch (Throwable ignored) {}

        // 2) Reflection fallback
        if (SPAWN_DELAY_FIELD != null) {
            try {
                return SPAWN_DELAY_FIELD.getInt(spawner);
            } catch (IllegalAccessException e) {
                LOGGER.warn("HarderSpawners: Failed to read spawnDelay reflectively.", e);
            }
        }

        // 3) Total failure
        return Integer.MIN_VALUE;
    }

    /** Returns true if the adapter is able to read spawnDelay. */
    public static boolean isWorking() {
        return SPAWN_DELAY_FIELD != null;
    }
}
