package com.mactso.harderspawners.common.logic;

import java.util.List;

import com.mactso.harderspawners.modloader.config.MyConfig;
import com.mojang.logging.LogUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ProblemReporter.ScopedCollector;
import net.minecraft.world.entity.Display.ItemDisplay;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class TimeExtensionItemDisplays {
	
	private static final org.slf4j.Logger LOGGERUTIL =  LogUtils.getLogger();
	
	public static void buildDisplay(ServerLevel sLevel, BlockEntity sbe) {
	
		sLevel.playSound(null, sbe.getBlockPos(), SoundEvents.ENDER_EYE_LAUNCH, SoundSource.AMBIENT, 0.5f, 0.2f);
		ItemDisplay itemDisplay = EntityType.ITEM_DISPLAY.create(sLevel,EntitySpawnReason.COMMAND);
		
		itemDisplay.setCustomName(SpawnerExpiration.TIP);
		itemDisplay.setCustomNameVisible(true);
		
		CompoundTag temptag = buildItemDisplayNBT(itemDisplay);
		ScopedCollector preport = new ScopedCollector((org.slf4j.Logger) LOGGERUTIL);
		itemDisplay.load(TagValueInput.create(preport, sbe.getLevel().registryAccess(), temptag));
		
		// Position the display above the spawner
		Vec3 vWork = sbe.getBlockPos().getBottomCenter();
		itemDisplay.setPos(vWork.x, vWork.y + 1.5, vWork.z);
		itemDisplay.setDeltaMovement(0.0f, 0.0f, 0.0f);
		sLevel.addFreshEntity(itemDisplay);
	
	}
	

	private static CompoundTag buildItemDisplayNBT(ItemDisplay i) {
		
        ScopedCollector preport = new ScopedCollector(LOGGERUTIL);
        TagValueOutput vout = TagValueOutput.createWithoutContext(preport);
        i.save(vout);
        CompoundTag tag = vout.buildResult(); 
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
			if (item.hasCustomName() && SpawnerExpiration.TIP.getString().equals(item.getCustomName().getString())) {
				return; // Already present
			}
		}
	
		// Add new repair item display
		buildDisplay(sLevel, sbe);
	}

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
		itemTag.putString("id", MyConfig.getTimeExtensionItem());
		itemTag.putInt("Count", 1);
		return itemTag;
	}

//	public static void removeDisplayOnExpiration(ServerLevel sLevel, BlockEntity sbe) {
//		// Build an AABB around the spawner's block position, inflated by 4 blocks
//		AABB box = new AABB(sbe.getBlockPos()).inflate(4.0);
//	
//		List<ItemDisplay> displaysList = sLevel.getEntitiesOfClass(ItemDisplay.class, box);
//	
//		for (ItemDisplay item : displaysList) {
//			if (item.hasCustomName() && SpawnerExpiration.tip.getString().equals(item.getCustomName().getString())) {
//				item.remove(RemovalReason.DISCARDED);
//				return;
//			}
//		}
//	}
	
	/** Removes the floating item display above the spawner. */
	static void removeDisplay(ServerLevel sLevel, SpawnerBlockEntity sbe) {
	    AABB box = new AABB(sbe.getBlockPos().above()).inflate(2); // 2-block radius
	    List<ItemDisplay> displays = sLevel.getEntitiesOfClass(ItemDisplay.class, box);
	
	    for (ItemDisplay item : displays) {
	        if (item.getCustomName().getString().equals(SpawnerExpiration.TIP.getString())) {
	            item.discard();
	            break;
	        }
	    }
	}

}
