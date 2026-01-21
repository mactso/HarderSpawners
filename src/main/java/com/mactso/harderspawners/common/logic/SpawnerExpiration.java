package com.mactso.harderspawners.common.logic;

import java.util.Optional;

import com.mactso.harderspawners.common.utility.MyUtilities;
import com.mactso.harderspawners.modloader.config.MyConfig;
import com.mactso.harderspawners.modloader.spawnerstorage.SpawnerStatsAdapter;
import com.mactso.harderspawners.modloader.spawnerstorage.SpawnerStatsHelper;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.phys.BlockHitResult;

public class SpawnerExpiration {

    public static final Component TIP = Component.translatable("text.harderspawners.add_lifespan").withStyle(ChatFormatting.LIGHT_PURPLE);
    private static String cachedExtraLifespanConfiguredValue = null;
    private static Item cachedExtraLifespanItem = null;

    /**
     * Handles repairing a spawner when a player right-clicks it with a valid repair item.
     *
     * @param player   The player interacting
     * @param sLevel   The server level where the spawner is located
     * @param heldItem The item stack the player is holding
     * @param hit      The hit result of the interaction
     */
    public static void extendSpawnerExpirationTime(ServerPlayer player, ServerLevel sLevel, ItemStack heldItem, BlockHitResult hit) {

        BlockPos pos = hit.getBlockPos();
        if (!sLevel.getBlockState(pos).is(Blocks.SPAWNER)) return;
        if (!(sLevel.getBlockEntity(pos) instanceof SpawnerBlockEntity sbe)) return;

        // Use the wrapper to access spawner stats
        SpawnerStatsAdapter.SpawnerStatsWrapper statsWrapper = SpawnerStatsHelper.getOrCreateStats(sbe);
        if (statsWrapper == null) return;

        if (!isExtraLifespanItem(heldItem, sLevel)) return;

        // --- Handle expired or zero lifespans ---
        long currentLifespan = statsWrapper.getLifespan();
        long extraAmount = MyConfig.getExtraLifespanAmount() * statsWrapper.averageSpawnDelay();
        long newLifespan;

        if (currentLifespan <= 0) {
            // Expired or zero: reset to addLifespanAmount
            newLifespan = extraAmount;
        } else {
            // Otherwise, extend by addLifespanAmount
            newLifespan = currentLifespan + extraAmount;
        }

        MyUtilities.debugMsg(
                1,
                sbe.getBlockPos(),
                "Lifespan extend: Beginning=" + currentLifespan +
                        ", extra=" + extraAmount +
                        ", new=" + newLifespan
        );

        statsWrapper.setLifespan(newLifespan);

        // Visual / special effects
        ExtraLifetimeItemDisplays.removeDisplay(sLevel, sbe);
        SpecialEffects.doSpawnerTimeExtensionEffects(sLevel, pos);

        // Consume the item
        heldItem.shrink(1);
        MyUtilities.debugMsg(1, "Spawns Left Increased to: " + statsWrapper.getEstimatedSpawns());

        // Mark dirty and sync
        sbe.setChanged();
        player.getInventory().setChanged();
    }

    private static boolean isExtraLifespanItem(ItemStack stack, ServerLevel sLevel) {
        return stack.getItem() == getExtraLifespanItemAsItem(sLevel);
    }

    /** Checks whether the held item is the configured spawner time extension item. */
    private static Item getExtraLifespanItemAsItem(ServerLevel sLevel) {

        String configExtraLifespanItem = MyConfig.getAddLifespanItem();
        if (configExtraLifespanItem.equals(cachedExtraLifespanConfiguredValue)) {
            if (cachedExtraLifespanItem != null)
                return cachedExtraLifespanItem;
        }

        ResourceLocation itemLocation = ResourceLocation.tryParse(configExtraLifespanItem);
        if (itemLocation == null) {
            return Items.IRON_BLOCK;
        }

        Registry<Item> itemRegistry = MyUtilities.getRegistrySafe(sLevel.registryAccess(), Registries.ITEM);

        Optional<Item> optItem = itemRegistry.getOptional(itemLocation);
        if (optItem.isEmpty()) {
            MyUtilities.debugMsg(0, "ERROR: Configured Time extension item " + configExtraLifespanItem + " is not registered. Using Items.IRON_BLOCK");
            return Items.IRON_BLOCK;
        }

        Item timeExtensionItem = optItem.get();
        cachedExtraLifespanConfiguredValue = configExtraLifespanItem;
        cachedExtraLifespanItem = timeExtensionItem;

        return timeExtensionItem;
    }
}
