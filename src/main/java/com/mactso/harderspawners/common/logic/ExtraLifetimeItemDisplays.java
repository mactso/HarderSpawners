package com.mactso.harderspawners.common.logic;

import java.util.List;

import com.mactso.harderspawners.modloader.config.MyConfig;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Display.ItemDisplay;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
/**
 * Handles creation, display, and removal of floating ItemDisplay entities
 * above spawners to visually indicate extra lifespan items.
 * Builds custom NBT for item transformation, scale, and display settings.
 * Ensures only one ItemDisplay per spawner exists at any time.
 * Provides server-side feedback with sound when displaying items.
 * Methods are fully static and operate on ServerLevel instances.
 * Designed for use with HarderSpawners mod lifespan visualization.
 */
public class ExtraLifetimeItemDisplays {
	
	/**
	 * Adds an ItemDisplay entity indicating low lifespan.
	 * The item displayed is the item that adds lifespan
	 * Sets the displays custom name to the localized translated word "Repair" 
	 * Plays a sound effect to indicate display creation.
	 */
	public static void buildDisplay(ServerLevel sLevel, BlockEntity sbe) {
	
		sLevel.playSound(null, sbe.getBlockPos(), SoundEvents.ENDER_EYE_LAUNCH, SoundSource.AMBIENT, 0.5f, 0.2f);
		ItemDisplay itemDisplay = EntityType.ITEM_DISPLAY.create(sLevel,EntitySpawnReason.COMMAND);
		
		itemDisplay.setCustomName(SpawnerLifespan.TIP);
		itemDisplay.setCustomNameVisible(true);
		
		CompoundTag temptag = buildItemDisplayNBT(itemDisplay);
		itemDisplay.load(temptag);
		
		// Position the display above the spawner
		Vec3 vWork = sbe.getBlockPos().getBottomCenter();
		itemDisplay.setPos(vWork.x, vWork.y + 1.5, vWork.z);
		itemDisplay.setDeltaMovement(0.0f, 0.0f, 0.0f);
		sLevel.addFreshEntity(itemDisplay);
	
	}
	

	
	private static CompoundTag buildItemDisplayNBT(ItemDisplay i) {
		
		CompoundTag tag = new CompoundTag();
		i.save(tag);
		tag.put("transformation", buildTransformationTag());
		tag.put("item", buildItemTag());
		tag.putString("billboard", "center");
		return tag;
		
	}


	public static void showDisplay(ServerLevel sLevel, BlockEntity sbe) {
		// Build an AABB centered on the spawner's block position, 2 blocks in each
		// direction
		BlockPos pos = sbe.getBlockPos();
		AABB box = new AABB(pos).inflate(2.0);
	
		// Check for existing ItemDisplay in that area
		List<ItemDisplay> displaysList = sLevel.getEntitiesOfClass(ItemDisplay.class, box);
	
		for (ItemDisplay item : displaysList) {
			if (item.hasCustomName() && SpawnerLifespan.TIP.getString().equals(item.getCustomName().getString())) {
				return; // Already present
			}
		}
	
		// Add new repair item display
		buildDisplay(sLevel, sbe);
	}

	/**
	 * Constructs the transformation NBT for an ItemDisplay.
	 * Includes translation, left and right rotations, and scaling.
	 * Translation is set to zero and scale to 0.5.
	 * Rotations are defaulted to no rotation (identity quaternion).
	 * Returns a CompoundTag to be attached to the display entity.
	 */
	public static CompoundTag buildTransformationTag() {
		CompoundTag transformationTag = new CompoundTag();
		ListTag translist = new ListTag();
		FloatTag zero = FloatTag.valueOf(0.0F);
		FloatTag one = FloatTag.valueOf(1.0F);
		translist.add(zero);
		translist.add(zero);
		translist.add(zero);
		transformationTag.put("translation", translist);
	
		ListTag lfRotlist = new ListTag();
		lfRotlist.add(zero);
		lfRotlist.add(zero);
		lfRotlist.add(zero);
		lfRotlist.add(one);
		transformationTag.put("left_rotation", lfRotlist);
	
		FloatTag scale = FloatTag.valueOf(0.5F);
		ListTag scalelist = new ListTag();
		scalelist.add(scale);
		scalelist.add(scale);
		scalelist.add(scale);
		transformationTag.put("scale", scalelist);
	
		ListTag rtRotlist = new ListTag();
		rtRotlist.add(zero);
		rtRotlist.add(zero);
		rtRotlist.add(zero);
		rtRotlist.add(one);
		transformationTag.put("right_rotation", rtRotlist);
		return transformationTag;
	}

	public static CompoundTag buildItemTag() {
		CompoundTag itemTag = new CompoundTag();
		itemTag.putString("id", MyConfig.getAddLifespanItem());
		itemTag.putInt("Count", 1);
		return itemTag;
	}

	
	/**
	 * Removes the floating ItemDisplay associated with a given spawner.
	 * Builds an axis-aligned bounding box 2 blocks around the spawner.
	 * Searches for an ItemDisplay with the correct custom name (TIP).
	 * Discards the entity if found to remove it from the world.
	 * Ensures that only one display per spawner is present at any time.
	 */
	public static void removeDisplay(ServerLevel sLevel, SpawnerBlockEntity sbe) {
	    AABB box = new AABB(sbe.getBlockPos().above()).inflate(2); // 2-block radius
	    List<ItemDisplay> displays = sLevel.getEntitiesOfClass(ItemDisplay.class, box);
	
	    for (ItemDisplay item : displays) {
	        if (item.getCustomName().getString().equals(SpawnerLifespan.TIP.getString())) {
	            item.discard();
	            break;
	        }
	    }
	}

}
