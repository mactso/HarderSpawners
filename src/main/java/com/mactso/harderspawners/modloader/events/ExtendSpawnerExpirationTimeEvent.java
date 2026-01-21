package com.mactso.harderspawners.modloader.events;

import com.mactso.harderspawners.common.logic.SpawnerExpiration;
import com.mactso.harderspawners.modloader.config.MyConfig;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.InteractionResult;

public class ExtendSpawnerExpirationTimeEvent {

    /**
     * Registers the Fabric callback for right-clicking blocks.
     * Call this during mod initialization.
     */
    public static void register() {
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (world.isClientSide()) return InteractionResult.PASS;

            if (!MyConfig.isAddLifespanEnabled()) return InteractionResult.PASS;

            if (!(world instanceof ServerLevel sLevel)) return InteractionResult.PASS;

            ItemStack heldItem = player.getItemInHand(hand);

            SpawnerExpiration.extendSpawnerExpirationTime(hitResult, sLevel, heldItem);
            return InteractionResult.SUCCESS;
        });
    }
}
