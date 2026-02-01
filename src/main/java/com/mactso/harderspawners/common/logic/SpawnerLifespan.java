package com.mactso.harderspawners.common.logic;

import com.mactso.harderspawners.common.managers.SpawnerPositionManager;
import com.mactso.harderspawners.common.utility.MyUtilities;
import com.mactso.harderspawners.modloader.config.MyConfig;
import com.mactso.harderspawners.modloader.config.MyConfig.EndOfLifespanAction;
import com.mactso.harderspawners.modloader.spawnerstorage.SpawnerStatsAdapter;
import com.mactso.harderspawners.modloader.spawnerstorage.SpawnerStatsAdapter.SpawnerStatsWrapper;
import com.mactso.harderspawners.modloader.spawnerstorage.SpawnerStatsHelper;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level.ExplosionInteraction;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.phys.Vec3;

public class SpawnerLifespan {

	public static final Component TIP = Component.translatable("text.harderspawners.add_lifespan")
			.withStyle(ChatFormatting.LIGHT_PURPLE);
	private static String cachedExtraLifespanConfiguredValue = null;
	private static Item cachedExtraLifespanItem = null;

	public static void handleSpawnerEndOfLife(ServerLevel serverLevel, SpawnerBlockEntity sbe,
			SpawnerStatsWrapper statsWrapper, CompoundTag spawnerTag) {

		// SharedUtilityMethods.logSpawnerState(1, "handleSpawnerLifespanEnd.pre", sbe,
		// statsWrapper);
		if (sbe.isRemoved()) return;
		if (statsWrapper.isExpired()) {
			Enum<EndOfLifespanAction> action = MyConfig.getEndOfLifespanAction();
			if (action == MyConfig.EndOfLifespanAction.LINGER) {
				// stun lingering spawner for 25 minutes.

				SpawnerStunLogic.stunSpawnerWithLinger(serverLevel, sbe, statsWrapper, spawnerTag);
				return;
			}
			// action == DESTROYED
			doSpawnerEndOfLifeDestroy(serverLevel, sbe, statsWrapper, spawnerTag);
			return;
		}
	}

	/**
	 * Handles repairing a spawner when a player right-clicks it with a valid repair
	 * item.
	 *
	 * @param player   The player interacting
	 * @param sLevel   The server level where the spawner is located
	 * @param heldItem The item stack the player is holding
	 * @param hit      The hit result of the interaction
	 */
	public static void addSpawnerLifespan(ServerLevel serverLevel, ServerPlayer serverPlayer, BlockPos pos,
			SpawnerBlockEntity sbe, ItemStack stack) {

		// Use the wrapper to access spawner stats
		SpawnerStatsAdapter.SpawnerStatsWrapper statsWrapper = SpawnerStatsHelper.getOrCreateStats(sbe);
		if (statsWrapper == null)
			return;

		if (!isExtraLifespanItem(stack, serverLevel))
			return;

		applyAddedLifespan(statsWrapper, pos);
		SpawnerStunLogic.restoreStunnedSpawner(serverLevel, sbe, statsWrapper);

		// Consume the addLifespan item
		stack.shrink(1);
		// Mark dirty and sync
		serverPlayer.getInventory().setChanged();
		MyUtilities.debugMsg(1, "Spawns Left Increased to: " + statsWrapper.getEstimatedSpawns());

		// Visual / special effects
		ExtraLifetimeItemDisplays.removeDisplay(serverLevel, sbe);
		SpecialEffects.doSpawnerTimeExtensionEffects(serverLevel, pos);

	}

	private static void applyAddedLifespan(SpawnerStatsWrapper statsWrapper, BlockPos pos) {

		long currentLifespan = statsWrapper.getLifespan();
		long extraAmount = MyConfig.getExtraLifespanAmount() * statsWrapper.averageSpawnDelay();
		long newLifespan;

		if (currentLifespan <= 0) {
			newLifespan = extraAmount;
		} else {
			newLifespan = currentLifespan + extraAmount;
		}

		statsWrapper.setLifespan(newLifespan);

		if (MyConfig.isDebug())
			MyUtilities.debugMsg(1, pos, "Lifespan extend: Beginning=" + currentLifespan + ", extra=" + extraAmount
					+ ", new=" + newLifespan);

	}

	public static boolean isExtraLifespanItem(ItemStack stack, ServerLevel serverLevel) {
		return stack.getItem() == getExtraLifespanItemAsItem(serverLevel);
	}

	/**
	 * Checks whether the held item is the configured spawner time extension item.
	 */
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
		Item timeExtensionItem = MyUtilities.getItem(sLevel, itemLocation);
		if (timeExtensionItem == null) {
			MyUtilities.debugMsg(0, "ERROR: Configured Time extension item " + configExtraLifespanItem
					+ " is not registered. Using Items.IRON_BLOCK");
			return Items.IRON_BLOCK;
		}

		cachedExtraLifespanConfiguredValue = configExtraLifespanItem;
		cachedExtraLifespanItem = timeExtensionItem;

		return timeExtensionItem;
	}

	/**
	 * Handles final cleanup when a spawner's lifespan is exhausted. Removes
	 * extralifespan Displays and destroys the spawner block then Skips explosions
	 * for silverfish spawners to protect End Portals. otherwise checks chance for
	 * explosion.
	 * 
	 * @return true if the spawner expired and was destroyed (with optional
	 *         explosion), false if the spawner is still active.
	 */

	public static boolean doSpawnerEndOfLifeDestroy(ServerLevel serverLevel, SpawnerBlockEntity sbe,
			SpawnerStatsWrapper statsWrapper, CompoundTag spawnerTag) {

		// Remove extraLifespan display items near the spawner
		ExtraLifetimeItemDisplays.removeDisplay(serverLevel, sbe);

		// Destroy the spawner block
		BlockPos pos = sbe.getBlockPos();

		// Mark the block entity as removed immediately to avoid NPE in chunk iteration
		ExtraLifetimeItemDisplays.removeDisplay(serverLevel, sbe);
		sbe.setRemoved();
	    serverLevel.getChunk(pos).removeBlockEntity(pos); 
		SpawnerPositionManager.forgetSpawner(serverLevel, pos);
		serverLevel.destroyBlock(pos, true); // drops loot table drops, not spawner blocks even with silk touch.

		// Avoid Exploding SilverFish Spawners to protect End Portals.
		String entityId = statsWrapper.getOriginalEntityId();
		if ("minecraft:silverfish".equals(entityId)) { // implied null entityId protection.
			return true;
		}

		// Random chance for explosion
		Double chance = serverLevel.random.nextDouble();
		double explodeRoll = 100.0 * chance;
		if (MyConfig.isDebug())
			MyUtilities.debugMsg(1, "Explode chance was :" + explodeRoll);
		if (explodeRoll < MyConfig.getSpawnersExplodePercentage()) {
			Vec3 v = new Vec3(pos.getX(), pos.getY(), pos.getZ());
			serverLevel.explode(null, // no entity responsible
					null, // no damage source
					null, // no context
					v.x, v.y, v.z, 4.0f, // explosion strength
					true, // causes block damage
					ExplosionInteraction.BLOCK);
		}
		return true;

	}
}
