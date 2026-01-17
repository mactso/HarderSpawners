
package com.mactso.harderspawners.modloader.adapter;

import net.minecraft.world.level.BaseSpawner;
import net.neoforged.fml.util.ObfuscationReflectionHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.lang.reflect.Field;

public class Adapters {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Field SPAWN_DELAY_FIELD;

    
    static {
        Field field = null;
        try {
            // findField handles the Mojang/SRG mapping switch automatically
            field = ObfuscationReflectionHelper.findField(BaseSpawner.class, "spawnDelay");  // <- this is neoforge helper.
            field.setAccessible(true);
        } catch (Exception e) {
            LOGGER.error("HarderSpawners: Could not find or access spawnDelay field!", e);
        }
        SPAWN_DELAY_FIELD = field;
    }

    /**
     * Confirms that the reflection field was successfully found and authorized.
     * Call this once during mod startup or before your first loop for peace of mind.
     */
    public static boolean isWorking() {
        return SPAWN_DELAY_FIELD != null;
    }

    /**
     * Retrieves the current spawnDelay.
     * @return current delay, or -1 if reflection is failing.
     */
    public static int getDelay(BaseSpawner spawner) {
        if (SPAWN_DELAY_FIELD == null || spawner == null) {
            return (0-Integer.MAX_VALUE);
        }
        try {
            return SPAWN_DELAY_FIELD.getInt(spawner);
        } catch (IllegalAccessException e) {
            return (0-Integer.MAX_VALUE);
        }
    }
}