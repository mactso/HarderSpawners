package com.mactso.harderspawners.sounds;

import com.mactso.harderspawners.Main;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.IForgeRegistry;

public class ModSounds {	
public static final SoundEvent SPAWNER_WAILS = create("spawner.wails");

private static SoundEvent create(String key)
{
	ResourceLocation res = ResourceLocation.fromNamespaceAndPath(Main.MODID, key);
	SoundEvent ret = SoundEvent.createVariableRangeEvent(res);
	return ret;
}

public static void registerHelper(IForgeRegistry<SoundEvent> registry, SoundEvent s)
{
	registry.register(s.location(), s);
}

public static void register(IForgeRegistry<SoundEvent> registry)
{
	registerHelper(registry,SPAWNER_WAILS);
}

}