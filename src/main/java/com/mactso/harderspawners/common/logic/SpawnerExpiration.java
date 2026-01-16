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
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

public class SpawnerExpiration {

	public static final Component TIP = Component.translatable("text.harderspawners.add_durability").withStyle(ChatFormatting.LIGHT_PURPLE);
	private static String cachedTimeExtensionConfiguredValue = null;
	private static Item cachedTimeExtensionItem = null;

	/**
	 * Handles repairing a spawner when a player right-clicks it with a valid repair item.
	 * Uses the SpawnerStatsWrapper instead of direct storage access.
	 *
	 * @param event    The player right-click block event
	 * @param sLevel   The server level where the spawner is located
	 * @param heldItem The item stack the player is holding
	 */
	public static void extendSpawnerExpirationTime(PlayerInteractEvent.RightClickBlock event, ServerLevel sLevel, ItemStack heldItem) {

	    BlockPos pos = event.getPos();
	    if (!sLevel.getBlockState(pos).is(Blocks.SPAWNER)) return;

	    if (!(sLevel.getBlockEntity(pos) instanceof SpawnerBlockEntity sbe)) return;

	    // Use the wrapper to access spawner stats
	    SpawnerStatsAdapter.SpawnerStatsWrapper statsWrapper = SpawnerStatsHelper.getOrCreateStats(sbe);
	    if (statsWrapper == null) return;

	    if (!isTimeExtensionItem(heldItem, sLevel)) return;

	    // Stop further NeoForge / vanilla processing
	    event.setCanceled(true);
	    event.setCancellationResult(InteractionResult.SUCCESS);

	    // calculate the number of extra spawns times the average time per spawn.
	    long extraTime = statsWrapper.getAverageTimePerSpawn() * MyConfig.getSpawnsAmount();
		statsWrapper.setSpawnerExpirationTime(statsWrapper.getSpawnerExpirationTime()+ extraTime) ;

		TimeExtensionItemDisplays.removeDisplay(sLevel, sbe);
	    SpecialEffects.doSpawnerTimeExtensionEffects(sLevel, pos);

	    heldItem.shrink(1);
	    MyUtilities.debugMsg(1, "Spawns Left Increased to: " + statsWrapper.getDurability());

	    // Mark dirty and sync
	    sbe.setChanged();
	    event.getEntity().getInventory().setChanged();
	}

	
	
    private static boolean isTimeExtensionItem(ItemStack stack, ServerLevel sLevel) {
        return stack.getItem() == getTimeExtensionItemAsItem(sLevel);
    }
	
    /** Checks whether the held item is the configured spawner time extension item. */
    private static Item getTimeExtensionItemAsItem(ServerLevel sLevel) {
    	
    	String configTimeExtensionItem = MyConfig.getTimeExtensionItem();
    	if (configTimeExtensionItem.equals(cachedTimeExtensionConfiguredValue)) {
    		if (cachedTimeExtensionItem != null) 
    			return cachedTimeExtensionItem;
    	}

    	ResourceLocation itemLocation = ResourceLocation.tryParse(configTimeExtensionItem);
        if (itemLocation == null) {
            return Items.IRON_BLOCK;
        }

        Registry<Item> itemRegistry = MyUtilities.getRegistrySafe(sLevel.registryAccess(), Registries.ITEM);

        Optional<Item> optItem = itemRegistry.getOptional(itemLocation);
        if (optItem.isEmpty()) {
        	MyUtilities.debugMsg(0, "ERROR: Configured Time extension item " + configTimeExtensionItem + " is not registered.  Using Items.IRON_BLOCK");
        	return Items.IRON_BLOCK;
        }

        Item timeExtensionItem = optItem.get();
        cachedTimeExtensionConfiguredValue = configTimeExtensionItem;
        cachedTimeExtensionItem = timeExtensionItem;

        return timeExtensionItem;
        
    }
}

