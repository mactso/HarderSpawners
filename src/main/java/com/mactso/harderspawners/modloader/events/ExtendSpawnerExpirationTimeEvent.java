package com.mactso.harderspawners.modloader.events;

import com.mactso.harderspawners.common.logic.SpawnerExpiration;
import com.mactso.harderspawners.modloader.config.MyConfig;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Handles player interaction events that extend a spawner's expiration time.
 *
 * <p>This event listens for {@link PlayerInteractEvent.RightClickBlock} on the
 * server side and delegates to {@link SpawnerExpiration} to apply durability
 * or lifespan extensions when the player uses a valid repair item.</p>
 *
 * <p>The logic is gated by configuration and only runs when spawner durability
 * repair is enabled.</p>
 */
public class ExtendSpawnerExpirationTimeEvent {

    /**
     * Registers this event handler with the NeoForge event bus.
     *
     * <p>This is called during mod initialization.</p>
     */
    public void register() {
        NeoForge.EVENT_BUS.register(new ExtendSpawnerExpirationTimeEvent());
    }

    /**
     * Handles right-click interactions on blocks to extend spawner expiration time.
     *
     * <p>This method runs only on the server, checks configuration flags,
     * retrieves the item held in the player's main hand, and delegates
     * processing to {@link SpawnerExpiration#extendSpawnerExpirationTime}.</p>
     *
     * @param event the block right-click interaction event
     */
    @SubscribeEvent
    public void onRightClick(PlayerInteractEvent.RightClickBlock event) {

        if (!(event.getLevel() instanceof ServerLevel sLevel))
            return;

        if (!MyConfig.isAddLifespanEnabled())
            return;

        ItemStack heldItem = event.getEntity().getItemInHand(InteractionHand.MAIN_HAND);

        SpawnerExpiration.extendSpawnerExpirationTime(event, sLevel, heldItem);
    }
}