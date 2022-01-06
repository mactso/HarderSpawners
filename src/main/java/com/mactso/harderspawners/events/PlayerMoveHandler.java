package com.mactso.harderspawners.events;

import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent.PlayerTickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber()
public class PlayerMoveHandler {
	@SubscribeEvent

	public void PlayerMove(PlayerTickEvent event) {
		if (event.player instanceof ServerPlayer sp) {
			BlockPos pos = sp.blockPosition();
			MutableBlockPos mp = new MutableBlockPos();
			if ((sp.getId() + sp.level.getGameTime()) % 10 == 0) {
				for (int x = -16; x <= 16; x++) {
					for (int z = -16; z <= 16; z++) {
						for (int y = -16; y <= 16; y++) {
							mp.setWithOffset(pos,x,z,y);
							int dm = pos.distManhattan(mp);
							if (pos.distManhattan(mp) > 4) {
								BlockState bs = sp.level.getBlockState(mp);
								if (bs.getBlock() == Blocks.SPAWNER) {
//									MyConfig.sendChat(sp,
//											"spawner block found at " + mp,
//											TextColor.fromLegacyFormat(ChatFormatting.GOLD));
									if (bs.hasBlockEntity()) {
										BlockEntity be = sp.level.getBlockEntity(mp);
										if ((be != null) && (be.getType() == BlockEntityType.MOB_SPAWNER)) {
											SpawnerBlockEntity sbe = (SpawnerBlockEntity) be;
											SpawnerSpawnEvent.updateHostileSpawnerValues(sbe.getSpawner());
//											MyConfig.sendChat(sp,
//													"spawner found at " + mp,
//													TextColor.fromLegacyFormat(ChatFormatting.GOLD));
										}
//										MyConfig.sendChat(sp,
//												"has entity inside it" + mp,
//												TextColor.fromLegacyFormat(ChatFormatting.GOLD));
									}
								}
							}
						}
					}
				}
			}
		}
	}
}
