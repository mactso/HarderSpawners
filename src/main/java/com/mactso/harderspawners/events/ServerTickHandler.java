
package com.mactso.harderspawners.events;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.mactso.harderspawners.Main;
import com.mactso.harderspawners.config.MyConfig;
import com.mactso.harderspawners.util.MyUtilities;

import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.event.TickEvent.ServerTickEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

@Mod.EventBusSubscriber(
	    modid = Main.MODID,
	    bus = Mod.EventBusSubscriber.Bus.FORGE
	)
public class ServerTickHandler {

    public static record WorkRecord(ServerLevel level, BlockPos pos) {}

	public static List<WorkRecord> workList = new ArrayList<>();
	private static List<WeakReference<SpawnerBlockEntity>> sbeList = new ArrayList<>();
	private static List<WeakReference<SpawnerBlockEntity>> addList = new ArrayList<>();
	private static Set<BlockPos> spawnerLocations = new HashSet<>();
	private static int ticks = 0;
	private static long hasEntriesTime = 0;

	@SubscribeEvent
	public static void onServerTickEvent(ServerTickEvent.Post event) {
        // Run once per second
		if (--ticks <= 0) {
			ticks = 20;

            // Add new entries
				synchronized (addList) {
					sbeList.addAll(addList);
					addList.clear();
				}

				if (!sbeList.isEmpty()) {
					Iterator<WeakReference<SpawnerBlockEntity>> it = sbeList.iterator();
					while (it.hasNext()) {
						WeakReference<SpawnerBlockEntity> wSbe = it.next();
						SpawnerBlockEntity sbe = wSbe.get();
						if (!isSpawnerValid(sbe)) {
                        MyUtilities.debugMsg(1, "Removing invalid spawner from sbeList.");
							it.remove();
						} else if (sbe.hasLevel()) {
							// Without this, setting spawner player ranges higher won't work
							// until the player is within the default spawner range.
							MyUtilities.debugMsg(1, "Initializing Spawner at " + sbe.getBlockPos());
							SpawnerSpawnEvent.doInitNewSpawner(sbe);
							it.remove();
							MyUtilities.debugMsg(1, "Removing spawner after initialization at " + sbe.getBlockPos());
						}
					}
				}

		}
        // Handle client updates
		if (workList.isEmpty()) {
			return;
		}

		if (hasEntriesTime == 0) {
			hasEntriesTime = event.server().overworld().getGameTime();
		}

		long currentTime = event.server().overworld().getGameTime();

		if (currentTime > hasEntriesTime) {
			Iterator<WorkRecord> wi = workList.iterator();
			while (wi.hasNext()) {
				WorkRecord work = wi.next();
				BlockEntity blockEntity = work.level.getBlockEntity(work.pos);
				if (blockEntity != null) {
					Packet<?> pkt = blockEntity.getUpdatePacket();
					if (pkt != null) {
						LevelChunk chunk = work.level.getChunkAt(work.pos);
						PacketDistributor.TRACKING_CHUNK.with(chunk).send(pkt);
					}
				}

			}
			workList.clear();
			hasEntriesTime = 0;
		}
	}
	
	public static void resetShutdown () {
		synchronized (spawnerLocations) {
			spawnerLocations.clear();
		}
	}

	public static void addClientUpdate(ServerLevel level, BlockPos pos) {
		workList.add(new WorkRecord(level, pos));
	}

	public static void addSbeWorklistEntry(SpawnerBlockEntity sbe) {
		if (Thread.currentThread().getName().equals("Render thread"))
			return;
		MyUtilities.debugMsg(1,"Adding Spawner at "+ sbe.getBlockPos()+" to spawnerLocations");
		synchronized (spawnerLocations) {
			spawnerLocations.add(sbe.getBlockPos());
		}
		MyUtilities.debugMsg(1,"Adding Weak Reference to Spawner at "+ sbe.getBlockPos()+" to sbeList");
		synchronized (addList) {
			addList.add(new WeakReference<>(sbe));
		}
	}

	public static boolean isSpawnerNearby(Level level, BlockPos pos) {

		synchronized (spawnerLocations) {

			Iterator<BlockPos> it = spawnerLocations.iterator();
			while (it.hasNext()) {
				BlockPos spawnerPos = it.next();
				if (!isSpawnerInDestroyLightRange(pos, spawnerPos))
					continue;
				if (!isThisSpawnerInThisDimension(level, spawnerPos))
					continue;
				return true;
			}
		}
		return false;
	}

	private static boolean isThisSpawnerInThisDimension(Level level, BlockPos pos) {
		if (level.getBlockState(pos).getBlock() == Blocks.SPAWNER) {
			return true;
		}
		return false;
	}

	private static boolean isSpawnerInDestroyLightRange(BlockPos lightBlockPos, BlockPos spawnerPos) {
		
		int dx = Math.abs(lightBlockPos.getX() - spawnerPos.getX());
		int dy = Math.abs(lightBlockPos.getY() - spawnerPos.getY());
		int dz = Math.abs(lightBlockPos.getZ() - spawnerPos.getZ());

		if (dx <= MyConfig.getDestroyLightRange() && (dz <= MyConfig.getDestroyLightRange())) {
			if (dy <= MyConfig.getDestroyLightRange()) {
				return true;
			}
		}
		return false;
	}

	private static boolean isSpawnerValid(SpawnerBlockEntity sbe) {
		
		if (sbe == null)
			return false;
		if (sbe.isRemoved())
			return false;
		if (!sbe.hasLevel())
			return false;
		BlockPos sbePos = sbe.getBlockPos();
		
		if (sbePos == null)
			return false;

		return true;
	}
}
