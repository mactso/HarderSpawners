package com.mactso.harderspawners.modloader.spawnerstorage;

import com.mactso.harderspawners.modloader.main.Main;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public class SpawnerAttachments {
	
	

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Main.MODID);

    
    // Registering the SpawnerStatsStorage attachment
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<SpawnerStatsStorage>> SPAWNER_STATS =
            ATTACHMENT_TYPES.register("spawner_stats",
                    () -> AttachmentType.serializable(SpawnerStatsStorage::new).build()
            );

    // Register the DeferredRegister to the mod bus
    public static void register(IEventBus modBus) {
        ATTACHMENT_TYPES.register(modBus);
    }
    
}