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

	/** List of known intermediary field names across 1.21.1 → 1.21.11 */

	private static Field findSpawnDelayField() {
		MappingResolver resolver = FabricLoader.getInstance().getMappingResolver();

		// --- 1 check production intermediary name first
		String candidate = "field_9154";
		try {
			LOGGER.debug("Attempting to map field: class=net.minecraft.class_1917, candidate=" + candidate
					+ ", descriptor=I");			
			
			String obfName = resolver.mapFieldName("intermediary", "net.minecraft.class_1917", // BaseSpawner
					candidate, "I");

			Field f = BaseSpawner.class.getDeclaredField(obfName);
			f.setAccessible(true);
			LOGGER.debug("Successfully found BaseSpawner.spawnDelay field via MappingResolver: " + obfName);
			return f;

		} catch (NoSuchFieldException nsfe) {
			LOGGER.warn("NoSuchFieldException: could not find field for candidate: " + candidate, nsfe);
		} catch (Exception e) {
			LOGGER.error("Unexpected exception while processing candidate: " + candidate, e);
		}

		// --- 2 Fallback: deobfuscated name for dev ---
		try {
			Field f = BaseSpawner.class.getDeclaredField("spawnDelay");
			f.setAccessible(true);
			LOGGER.debug("Found BaseSpawner.spawnDelay via direct deobfuscated name.");
			return f;
		} catch (NoSuchFieldException ignored) {
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
