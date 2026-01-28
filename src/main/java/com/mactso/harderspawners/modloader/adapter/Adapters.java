package com.mactso.harderspawners.modloader.adapter;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.MappingResolver;
import net.minecraft.world.level.BaseSpawner;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;

/**
 * Adapter to read BaseSpawner.spawnDelay in Fabric 1.21.1 using reflection only.
 */
public class Adapters {
    private static final Logger LOGGER = LogManager.getLogger();

    // Cached reflective field
    private static Field SPAWN_DELAY_FIELD;

    static {
        SPAWN_DELAY_FIELD = findSpawnDelayField();
    }

    
    
    private static Field findSpawnDelayField() {


        // Try deobfuscated name first
        try {
            Field f = BaseSpawner.class.getDeclaredField("spawnDelay");
            f.setAccessible(true);
            LOGGER.debug("HarderSpawners: Found BaseSpawner.spawnDelay via direct name.");
            return f;
        } catch (NoSuchFieldException ignored) {}

        // Fallback: use Fabric MappingResolver to find obfuscated field
        try {
            MappingResolver resolver = FabricLoader.getInstance().getMappingResolver();

            // Intermediary names for 1.21.1
            String obfName = resolver.mapFieldName(
                    "intermediary",
                    "net.minecraft.class_1917", // BaseSpawner
                    "field_9154",               // spawnDelay
                    "I"
            );

            if (obfName != null) {
                Field f = BaseSpawner.class.getDeclaredField(obfName);
                f.setAccessible(true);
                LOGGER.debug("HarderSpawners: Found BaseSpawner.spawnDelay via MappingResolver: " + obfName);
                return f;
            }
        } catch (Exception e) {
            LOGGER.warn("HarderSpawners: Could not find BaseSpawner.spawnDelay with MappingResolver.", e);
        }

        // Hard fail: throw an exception to crash immediately
        throw new IllegalStateException(
            "HarderSpawners: Unable to reflect into critical field BaseSpawner.spawnDelay in Adapters.findSpawnDelayField. " +
            "This is required for the mod to function and indicates a version mismatch or invalid environment."
        );
    }

    /**
     * Returns the spawn delay of a BaseSpawner instance using reflection.
     * @param spawner BaseSpawner instance
     * @return spawnDelay value or Integer.MIN_VALUE on failure
     */
    public static int getSpawnDelay(BaseSpawner spawner) {
        if (spawner == null || SPAWN_DELAY_FIELD == null) return Integer.MIN_VALUE;

        try {
            return SPAWN_DELAY_FIELD.getInt(spawner);
        } catch (IllegalAccessException e) {
            LOGGER.warn("HarderSpawners: Failed to read spawnDelay reflectively.", e);
            return Integer.MIN_VALUE;
        }
    }

    /** Returns true if reflection found the spawnDelay field successfully. */
    public static boolean isWorking() {
        return SPAWN_DELAY_FIELD != null;
    }
}
