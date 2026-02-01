package com.mactso.harderspawners.common.sounds;

import com.mactso.harderspawners.modloader.main.Main;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

/**
 * Registers custom SoundEvents for Harder Spawners mod (Fabric 1.21.1, official mappings)
 */
public class ModSounds {

    public static final SoundEvent SPAWNER_RECOVERS = registerSoundEvent("spawner_recovers");

    private static SoundEvent registerSoundEvent(String name) {
    	ResourceLocation id = ResourceLocation.fromNamespaceAndPath(Main.MODID, name);
        SoundEvent event = SoundEvent.createVariableRangeEvent(id);
        return Registry.register(BuiltInRegistries.SOUND_EVENT, id, event);
    }

    /** Forces class load and static registration */
    public static void register() {
        // Nothing needed here; static fields already register the sounds
    }
}
