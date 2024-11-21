package com.mactso.harderspawners.events;

import java.util.List;

import com.mactso.harderspawners.capabilities.CapabilitySpawner;
import com.mactso.harderspawners.capabilities.ISpawnerStatsStorage;
import com.mactso.harderspawners.config.MyConfig;
import com.mactso.harderspawners.util.Utility;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Display.ItemDisplay;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber // Ensure the event subscriber is registered
public class IncreaseSpawnerDurabilityEvent {

	@SubscribeEvent
	public static void onRightClick(PlayerInteractEvent.RightClickBlock event) {

		Level level = event.getLevel();
		if (level.isClientSide)
			return;

		ServerLevel sLevel = (ServerLevel) event.getLevel();
		ItemStack heldItem = event.getEntity().getItemInHand(InteractionHand.MAIN_HAND);

		if (MyConfig.isDurabilityRepairEnabled()) {
			doRepairSpawnerDurability(event, level, sLevel, heldItem);
		}
	}

	private static void doRepairSpawnerDurability(PlayerInteractEvent.RightClickBlock event, Level level,
			ServerLevel sLevel, ItemStack heldItem) {
		if (!(heldItem.getItem() == MyConfig.getDurabilityItemAsItem()))
			return;

		if (!(sLevel.getBlockState(event.getPos()).is(Blocks.SPAWNER)))
			return;

		BlockPos pos = event.getPos();
		if (sLevel.getBlockEntity(pos) instanceof SpawnerBlockEntity sbe) {

			// Stop further FORGE processing of this event
			event.setCanceled(true);
			event.setCancellationResult(InteractionResult.SUCCESS);

			ISpawnerStatsStorage cap = sbe.getCapability(CapabilitySpawner.SPAWNER_STORAGE).orElse(null);
			if (cap.getDurability() > 250) {
				level.playSound(null, pos, SoundEvents.DISPENSER_FAIL, SoundSource.AMBIENT, 1.0f, 1.0f);
				doRemoveItemDisplay(sLevel, sbe);
				return;
			}
			cap.setDurability(cap.getDurability() + MyConfig.getDurabilityRepairAmount());
			doRemoveItemDisplay(sLevel, sbe);
			doSpawnerRepairFeedback(level, sLevel, pos);
			heldItem.shrink(1);
			Utility.debugMsg(1, "Spawns Left Increased to :" + cap.getDurability());

			// let Minecraft and Forge know to save these changes and update the Client.
			sbe.setChanged();
			event.getEntity().getInventory().setChanged();
		}
	}

	private static void doRemoveItemDisplay(ServerLevel sLevel, SpawnerBlockEntity sbe) {
		List<ItemDisplay> itemDisplaysList = sLevel.getEntitiesOfClass(ItemDisplay.class,
				sbe.getRenderBoundingBox().inflate(2));

		for (ItemDisplay item : itemDisplaysList) {
			if (item.getCustomName().getString().equals(SpawnerSpawnEvent.tip.getString())) {
				item.discard();
				break;
			}
		}
	}

	private static void doSpawnerRepairFeedback(Level level, ServerLevel sLevel, BlockPos pos) {

		level.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.AMBIENT, 1.0f, 1.0f);
		RandomSource rand = sLevel.getRandom();

		for (int j = 0; j < 11; ++j) {
			SimpleParticleType particles = ParticleTypes.SOUL_FIRE_FLAME;
			double x = (double) pos.getX() + rand.nextDouble();
			double y = (double) pos.getY() + 0.95d;
			double z = (double) pos.getZ() + rand.nextDouble();
			double vx = 0.06 * rand.nextDouble() - 0.03d;
			double vz = 0.06 * rand.nextDouble() - 0.03d;
			Vec3 rfv = new Vec3(vx, 0.05, vz);
			sLevel.sendParticles(particles, x, y, z, 3, rfv.x, rfv.y, rfv.z, -0.04D);
		}

	}
}
