package com.mactso.harderspawners.events;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.mactso.harderspawners.util.Utility;

import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.ServerTickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.PacketDistributor;

public class ServerTickHandler {

	public static record workRecord(ServerLevel level, BlockPos pos) {

	}

	public static List<workRecord> workList = new ArrayList<>();
	private static List<WeakReference<SpawnerBlockEntity>> sbelist = new ArrayList<>();
	private static List<WeakReference<SpawnerBlockEntity>> addlist = new ArrayList<>();
	private static int ticks = 0;

	private static long hasEntriesTime = 0;

	@SubscribeEvent
	public void onServerTickEvent(ServerTickEvent event) {

		if (event.phase == Phase.END && (--ticks) <= 0) {
			ticks = 20;
			synchronized (sbelist) {
				sbelist.addAll(addlist);
				addlist.clear();
			}
			Utility.debugMsg(1, "Processing synchronized list of spawners to init if needed");
				Iterator<WeakReference<SpawnerBlockEntity>> it = sbelist.iterator();
				while (it.hasNext()) {
					SpawnerBlockEntity sbe = it.next().get();
					if (sbe == null || sbeTest(sbe))
						it.remove();
				}
			}

		if (workList.isEmpty())

		{
			return;
		}

		if (hasEntriesTime == 0) {
			hasEntriesTime = event.getServer().overworld().getGameTime();
		}

		long currentTime = event.getServer().overworld().getGameTime();

		if (currentTime > hasEntriesTime) {
			Iterator<workRecord> wi = workList.iterator();
			while (wi.hasNext()) {
				workRecord work = wi.next();
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

	public static void addClientUpdate(ServerLevel level, BlockPos pos) {
		workList.add(new workRecord(level, pos));
	}

	public static void addSbeWorklistEntry(SpawnerBlockEntity sbe) {
		if (Thread.currentThread().getName().equals("Render thread"))
			return;
		Utility.debugMsg(1, "Adding Weak Reference to Spawner at " + sbe.getBlockPos() + " to sbeList");
		synchronized (addlist) {
			addlist.add(new WeakReference<>(sbe));
		}
	}

	private static boolean sbeTest(SpawnerBlockEntity sbe) {
		if (sbe.isRemoved())
			return true;
		else if (sbe.hasLevel()) {
			if (sbe.getLevel().isClientSide()) {
				return false;
			} else {
				SpawnerSpawnEvent.updateHostileSpawnerValues(sbe,sbe.getSpawner(), false);
			return true;
			}
		}
		return false;
	}

}
