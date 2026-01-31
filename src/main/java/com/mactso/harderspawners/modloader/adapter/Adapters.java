package com.mactso.harderspawners.modloader.adapter;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.MappingResolver;
import net.minecraft.world.level.BaseSpawner;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;

/**
 * Adapter to read BaseSpawner.spawnDelay reflectively across Fabric 1.21.1 →
 * 1.21.11.
 * <p>
 * Tries multiple intermediary names (production) first, then falls back to
 * deobfuscated name ("spawnDelay") for development.
 */
public class Adapters {
	private static final Logger LOGGER = LogManager.getLogger();

	/** Cached reflective Field */
	private static final Field SPAWN_DELAY_FIELD = findSpawnDelayField();

	private static Field findSpawnDelayField() {
		MappingResolver resolver = FabricLoader.getInstance().getMappingResolver();

		// --- 1️⃣ Try to map to production intermediary name
		try {
			String obfName = resolver.mapFieldName("intermediary", "net.minecraft.class_1917", // BaseSpawner
					"field_9154", "I");
			Field f = BaseSpawner.class.getDeclaredField(obfName);
			f.setAccessible(true);
			return f;
		} catch (Exception Exception) {
			// Fall through to the Dev reflection
		}

		// --- 2️⃣ Fallback: get the Dev  ---
		try
		{
			Field f = BaseSpawner.class.getDeclaredField("spawnDelay");
			f.setAccessible(true);
			return f;
		} catch (Exception ignored) {
			// Fall through to hard failure.
		}

		// --- 3️⃣ Hard fail ---
		throw new IllegalStateException("Unable to reflect BaseSpawner.spawnDelay. "
				+ "Critical for mod functionality — environment/version mismatch likely.");
	}

	/**
	 * Get the spawnDelay value from a BaseSpawner instance.
	 *
	 * @param spawner the BaseSpawner instance
	 * @return the spawnDelay value, or Integer.MIN_VALUE if reflection fails
	 */
	public static int getSpawnDelay(BaseSpawner spawner) {
		if (spawner == null || SPAWN_DELAY_FIELD == null)
			return Integer.MIN_VALUE;
		try {
			return SPAWN_DELAY_FIELD.getInt(spawner);
		} catch (IllegalAccessException e) {
			LOGGER.warn("Failed to read BaseSpawner.spawnDelay reflectively.", e);
			return Integer.MIN_VALUE;
		}
	}

	/** Returns true if reflection successfully found the spawnDelay field. */
	public static boolean isWorking() {
		return SPAWN_DELAY_FIELD != null;
	}
}