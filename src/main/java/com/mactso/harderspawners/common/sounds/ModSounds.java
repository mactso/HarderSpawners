package com.mactso.harderspawners.common.sounds;

import com.mactso.harderspawners.modloader.main.Main;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Registers custom SoundEvents for Harder Spawners mod.
 */
public class ModSounds {
	
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(BuiltInRegistries.SOUND_EVENT, Main.MODID);


    // All vanilla sounds use variable range events.
    public static final Holder<SoundEvent> SPAWNER_RECOVERS = SOUND_EVENTS.register(
            "spawner_recovers",
            // Takes in the registry name
            SoundEvent::createVariableRangeEvent
    );

}
