package com.mactso.harderspawners.events;

import java.util.List;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mactso.harderspawners.capabilities.CapabilitySpawner;
import com.mactso.harderspawners.capabilities.ISpawnerStatsStorage;
import com.mactso.harderspawners.config.MobSpawnerManager;
import com.mactso.harderspawners.config.MobSpawnerManager.SpawnerDurabilityItem;
import com.mactso.harderspawners.config.MyConfig;
import com.mactso.harderspawners.util.SharedUtilityMethods;
import com.mactso.harderspawners.util.Utility;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.InclusiveRange;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Display.ItemDisplay;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentTable;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.Level.ExplosionInteraction;
import net.minecraft.world.level.SpawnData;
import net.minecraft.world.level.SpawnData.CustomSpawnRules;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent.FinalizeSpawn;
import net.minecraftforge.eventbus.api.Event.Result;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class SpawnerSpawnEvent {
	private static int debugThreadIdentifier = 0;
	private static final Logger LOGGER = LogManager.getLogger();
	private static BlockPos lastSpawnerPos = null;
	public static long lastSpawnTime;

	static int MAX_AGE = 1200;
	private static final int EFFECT_LEVEL_0 = 0;
	public static Component tip = Component.literal("Add Durability").withStyle(ChatFormatting.LIGHT_PURPLE);

	//
	// context - this event only happens once every 15 to 45 seconds per active
	// spawner
	//

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public void onCheckSpawnerSpawn(MobSpawnEvent.FinalizeSpawn event) {

		if (isErrorFree(event)) {
			ServerLevel sLevel = (ServerLevel) event.getLevel();
			if (!sLevel.isUnobstructed(event.getEntity())) {
				event.setResult(Result.DENY);
				return;
			}
			event.setResult(Result.ALLOW); // prevent entity spawn rules from blocking it
			doDebugThreadMsg(event);
			doProcessSpawner(event);
		}

	}

	
	
	// Note this is called once per mob spawned.
	public static void doProcessSpawner(MobSpawnEvent.FinalizeSpawn event) {

		if (event.getSpawner().getSpawnerBlockEntity() instanceof SpawnerBlockEntity sbe) {

			ServerLevel sLevel = (ServerLevel) event.getLevel();

			BaseSpawner mySpawner = sbe.getSpawner();
			BlockPos spawnerPos = sbe.getBlockPos();

			CompoundTag tag = new CompoundTag();
			mySpawner.save(tag); // Save spawner values into the tag

			boolean initialized = doInitNewSpawner(sbe);
			boolean changed = doHandleStunnedSpawner(sbe, tag);

			if (initialized || changed) {
				mySpawner.load(sLevel, spawnerPos, tag);
				ServerTickHandler.addClientUpdate(sLevel,spawnerPos);
			}

			if (isFirstSpawnInGroup(sLevel, spawnerPos)) {
				doUseASpawn(sLevel, sbe, mySpawner);
				if (isMonsterSpawner(sbe, tag)) {
					doProtectiveMobBuffs(event, sLevel);
					SharedUtilityMethods.doDestroyLightsNearBlockPos(sbe.getBlockPos(), sLevel);
					doSpawnerFails(event, sbe);
				}
			}
		}

	}

	
	
	private static boolean isFirstSpawnInGroup(ServerLevel sLevel, BlockPos spawnerPos) {

		if ((spawnerPos.equals(lastSpawnerPos)) && (lastSpawnTime == sLevel.getGameTime()))
			return true;

		lastSpawnerPos = spawnerPos;
		lastSpawnTime = sLevel.getGameTime();

		return false;

	}

	
	
	private static void doProtectiveMobBuffs(MobSpawnEvent.FinalizeSpawn event, ServerLevel sLevel) {

		if ((MyConfig.getHostileSpawnerResistDaylightDuration() > 0)
				&& (sLevel.getMaxLocalRawBrightness(event.getEntity().blockPosition()) > 8)) {
			event.getEntity().addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE,
					MyConfig.getHostileSpawnerResistDaylightDuration() * 20, EFFECT_LEVEL_0, false, false));
		}

		if (sLevel.containsAnyLiquid(event.getEntity().getBoundingBox())) {
			if (!event.getEntity().canBreatheUnderwater()) {
				event.getEntity().addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING,
						MyConfig.getHostileSpawnerResistDaylightDuration() * 20, EFFECT_LEVEL_0, false, false));
			}
		}
	}

	// TODO: Test this every release.
	// cap is confirmed valid before this method is called.
	private static void doSpawnerFails(MobSpawnEvent.FinalizeSpawn event, SpawnerBlockEntity sbe) {

		if (event.getEntity() instanceof Silverfish) {
			return;
		}

		ISpawnerStatsStorage cap = sbe.getCapability(CapabilitySpawner.SPAWNER_STORAGE).orElse(null);
		if ((cap.isInfiniteDurability()))
			return;
		if (cap.getDurability() > 0)
			return;

		ServerLevel sLevel = (ServerLevel) event.getLevel();
		BlockPos pos = sbe.getBlockPos();

		sLevel.destroyBlock(pos, false);
		RandomSource chance = event.getLevel().getRandom();
		double explodeRoll = 100.0 * chance.nextDouble();
		if ((explodeRoll) < MyConfig.getSpawnersExplodePercentage()) {
			Vec3 v = new Vec3(pos.getX(), pos.getY(), pos.getZ());
			
			sLevel.explode(null, null, null, v.x, v.y, v.z, 4.0f, true,
					ExplosionInteraction.BLOCK);
		}

	}

	
	
	public static boolean doInitNewSpawner(SpawnerBlockEntity sbe) {

		ISpawnerStatsStorage cap = sbe.getCapability(CapabilitySpawner.SPAWNER_STORAGE).orElse(null);

		if ((cap == null) || (cap.isInitialized()))
			return false;

		Utility.debugMsg(1, "Trying to initialize spawner at " + sbe.getBlockPos());
		
		CompoundTag tag = new CompoundTag();
		sbe.getSpawner().save(tag);
		CompoundTag spawnDataTag = tag.getCompound("SpawnData");
		CompoundTag entityTag = spawnDataTag.getCompound("entity");
		Optional<EntityType<?>> entityType = EntityType.by(entityTag);

		if (entityType.isPresent()) { // Getting Spawner Durability requires an Entity Type.
			Utility.debugMsg(1, "Initializing spawner at " + sbe.getBlockPos());
			doInitMonsterSpawnerNBT(sbe, tag, spawnDataTag, entityTag);
			doInitNewSpawnerCapability(cap, entityType);
			sbe.setChanged();
		}

		return true;
	}



	private static void doInitMonsterSpawnerNBT(SpawnerBlockEntity sbe, CompoundTag tag, CompoundTag spawnDataTag,
			CompoundTag entityTag) {
		if (isMonsterSpawner(sbe, tag)) {
			if (tag.getInt("MaxNearbyEntities") != MyConfig.getMaxNearbyEntities())
				tag.putInt("MaxNearbyEntities", MyConfig.getMaxNearbyEntities());
			if (tag.getInt("RequiredPlayerRange") != MyConfig.getRequiredPlayerRange())
				tag.putInt("RequiredPlayerRange", MyConfig.getRequiredPlayerRange());
			if (tag.getInt("SpawnRange") != MyConfig.getSpawnRange())
				tag.putInt("SpawnRange", MyConfig.getSpawnRange());
			Optional<Tag> workSpawnData = buildCustomLightLevelSpawnData(spawnDataTag, entityTag);
			if (workSpawnData.isPresent()) {
				if (!spawnDataTag.equals(workSpawnData.get())) {
					tag.put("SpawnData", workSpawnData.get());
				}
			}
			sbe.getSpawner().load(sbe.getLevel(), sbe.getBlockPos(), tag);
		}
	}

	
	
	private static void doInitNewSpawnerCapability(ISpawnerStatsStorage cap, Optional<EntityType<?>> entityType) {
		SpawnerDurabilityItem durabilityItem = MobSpawnerManager.getMobSpawnerSpawnsCountByMobType(entityType.get());
		cap.setDurability(durabilityItem.initDurabilityValue());
		cap.setInfinite(durabilityItem.isInfiniteDurability());
		cap.setInitialized();
	}

	
	
	private static Optional<Tag> buildCustomLightLevelSpawnData(CompoundTag spawnDataTag, CompoundTag entityTag) {

		SpawnData spawndata = SpawnData.CODEC.parse(NbtOps.INSTANCE, spawnDataTag)
				.resultOrPartial(p_186391_ -> LOGGER.warn("Invalid SpawnData: {}", p_186391_))
				.orElseGet(SpawnData::new);
		Optional<EquipmentTable> equipment = spawndata.equipment();

		int lightLevel = MyConfig.getHostileSpawnerLightLevel();
		int blocklight = lightLevel;
		int skylight = lightLevel;
		CustomSpawnRules c = new SpawnData.CustomSpawnRules(new InclusiveRange<Integer>(0, blocklight),
				new InclusiveRange<Integer>(0, skylight));

		SpawnData s = new SpawnData(entityTag, Optional.of(c), equipment);

		return SpawnData.CODEC.encodeStart(NbtOps.INSTANCE, s).result();

	}

	
	
	private static boolean isMonsterSpawner(SpawnerBlockEntity sbe, CompoundTag tag) {

		Optional<EntityType<?>> eType = EntityType.by(tag.getCompound("SpawnData").getCompound("entity"));

		if (eType.isEmpty())
			return false;

		if (eType.get().getCategory() == MobCategory.MONSTER)
			return true;

		return false;
	}

	
	
	private boolean isErrorFree(FinalizeSpawn event) {

		if ((event.getLevel().isClientSide()))
			return false;

		if (event.getSpawnType() != MobSpawnType.SPAWNER)
			return false;

		if (event.getSpawner() == null)
			return false;

		BlockEntity sbe = event.getSpawner().getSpawnerBlockEntity();
		if (sbe == null) // Bumblezone, issue #8.
			return false;

		ISpawnerStatsStorage cap = sbe.getCapability(CapabilitySpawner.SPAWNER_STORAGE).orElse(null);
		if (cap == null) {
			return false;
		}

		return true;
	}

	
	
	private static boolean doHandleStunnedSpawner(SpawnerBlockEntity sbe, CompoundTag tag) {

		ISpawnerStatsStorage cap = sbe.getCapability(CapabilitySpawner.SPAWNER_STORAGE).orElse(null);

		if (!cap.isStunned())
			return false;

		doStunDebugMsg(sbe, tag, cap);

		tag.putInt("MaxSpawnDelay", cap.getMaxSpawnDelay());
		tag.putInt("MinSpawnDelay", cap.getMinSpawnDelay());
		cap.setStunned(false);
		sbe.setChanged();
		return true;

	}

	
	
	private static void doStunDebugMsg(BlockEntity sbe, CompoundTag tag, ISpawnerStatsStorage cap) {
		if (MyConfig.getDebugLevel() > 0)
			return;
		BlockPos pos = sbe.getBlockPos();
		Utility.debugMsg(1, pos, "Restoring Stunned Spawner");
		Utility.debugMsg(2, pos, "Stunned Spawner stunned values: (max):" + tag.getInt("MaxSpawnDelay") + "(min):"
				+ tag.getInt("MinSpawnDelay"));
		Utility.debugMsg(2, pos,
				"Restoring Spawner saved values: (max):" + cap.getMaxSpawnDelay() + "(min):" + cap.getMinSpawnDelay());
	}

	
	
	public static void doUseASpawn(ServerLevel sLevel, SpawnerBlockEntity sbe, BaseSpawner mySpawner) {

		ISpawnerStatsStorage cap = sbe.getCapability(CapabilitySpawner.SPAWNER_STORAGE).orElse(null);

		// cap confirmed non-null at the start
		if (cap.isInfiniteDurability()) {
			return;
		}

		int spawnsLeft = cap.getDurability() - 1;

//		// tODO debug code
//		Utility.debugMsg(0, "Disable Debug Code");
//		spawnsLeft = 24;
//		// tODO debug code

		cap.setDurability(spawnsLeft);
		sbe.setChanged();

		if (spawnsLeft < 25) {
			doSpawnerFailingEffects(sLevel, sbe, spawnsLeft);
		}

//		// tODO debug code
//		Utility.debugMsg(0, "Disable Debug Code");
//		if (spawnsLeft < 5) {
//			int debug8 = 8; // TODO this is for debugging so I can reset the spawnsleft count.
//		}
//		// tODO debug code

	}

	
	
	private static void doSpawnerFailingEffects(ServerLevel sLevel, BlockEntity sbe, int durabilityLeft) {

		if (MyConfig.isDurabilityRepairEnabled()) {
			doShowRepairItemDisplay(sLevel, sbe);
		}
		doSpawnerFailingNoise(sLevel, sbe.getBlockPos(), durabilityLeft);
		doSpawnerFailingParticles(sLevel, sbe.getBlockPos(), durabilityLeft);

	}

	
	
	private static void doShowRepairItemDisplay(ServerLevel sLevel, BlockEntity sbe) {
		List<ItemDisplay> displaysList = sLevel.getEntitiesOfClass(ItemDisplay.class,
				sbe.getRenderBoundingBox().inflate(2));

		for (ItemDisplay item : displaysList) {

			if ((item.hasCustomName()) && (tip.getString().equals(item.getCustomName().getString()))) {
				return;
			}
		}

		buildAndAddRepairItemDisplay(sLevel, sbe);

	}

	
	
	private static void buildAndAddRepairItemDisplay(ServerLevel sLevel, BlockEntity sbe) {
		
		sLevel.playSound(null, sbe.getBlockPos(), SoundEvents.ENDER_EYE_LAUNCH, SoundSource.AMBIENT, 0.5f, 0.2f);
		ItemDisplay itemDisplay = EntityType.ITEM_DISPLAY.create(sLevel);
		itemDisplay.setCustomName(tip);
		itemDisplay.setCustomNameVisible(true);
		CompoundTag temptag = buildItemDisplayNBT(itemDisplay);
		itemDisplay.load(temptag);
		Vec3 vWork = sbe.getBlockPos().getBottomCenter();
		itemDisplay.moveTo(vWork.x, vWork.y + 1.5, vWork.z, 0.0f, 0.0f);
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

	
	
	private static CompoundTag buildItemTag() {
		CompoundTag itemTag = new CompoundTag();
		itemTag.putString("id", MyConfig.getDurabilityItem());
		itemTag.putInt("Count", 1);
		return itemTag;
	}

	
	
	private static CompoundTag buildTransformationTag() {
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

	
	
	private static void doSpawnerFailingNoise(ServerLevel sLevel, BlockPos pos, int spawnsLeft) {

		float volume = 1.0f - (spawnsLeft / 30.0f);

		if (spawnsLeft > 18) {
			sLevel.playSound(null, pos, SoundEvents.FIREWORK_ROCKET_BLAST_FAR, SoundSource.AMBIENT, volume, 0.2f);
		} else if (spawnsLeft > 13) {
			sLevel.playSound(null, pos, SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.AMBIENT, volume, 0.2f);
		} else if (spawnsLeft > 7) {
			sLevel.playSound(null, pos, SoundEvents.FIREWORK_ROCKET_LARGE_BLAST_FAR, SoundSource.AMBIENT, volume, 0.2f);
		} else {
			sLevel.playSound(null, pos, SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, SoundSource.AMBIENT, volume, 0.2f);
		}
	}

	
	
	private static void doSpawnerFailingParticles(ServerLevel sLevel, BlockPos pos, int spawnsLeft) {

		RandomSource rand = sLevel.getRandom();
		int numParticles = 30 - spawnsLeft;

		for (int j = 0; j < numParticles; ++j) {
			double vx = 0.06 * rand.nextDouble() - 0.03d;
			double vz = 0.06 * rand.nextDouble() - 0.03d;
			Vec3 rfv = new Vec3(vx, 0.25, vz);
			double x = (double) pos.getX() + rand.nextDouble();
			double y = (double) pos.getY() + 0.95d;
			double z = (double) pos.getZ() + rand.nextDouble();
			SimpleParticleType particles = ParticleTypes.CAMPFIRE_COSY_SMOKE;
			sLevel.sendParticles(particles, x, y, z, 3, rfv.x, rfv.y, rfv.z, -0.04D);
		}
	}

	
	
	private static void doDebugThreadMsg(MobSpawnEvent.FinalizeSpawn event) {
		debugThreadIdentifier = (debugThreadIdentifier + 1) % 10000;
		Utility.debugMsg(1, "HarderSpawners: (" + debugThreadIdentifier + ") Checking Spawner Spawn Event at "
				+ (int) event.getX() + "+(int)event.getY()+" + (int) event.getZ() + ".");
	}

}
