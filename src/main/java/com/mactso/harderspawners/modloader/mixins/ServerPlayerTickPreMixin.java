package com.mactso.harderspawners.modloader.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mactso.harderspawners.modloader.events.ServerPlayerTickPreMixinEvent;

import net.minecraft.server.level.ServerPlayer;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerTickPreMixin {

    @Inject(method = "tick", at = @At("HEAD"))
    private void onServerPlayerTick(CallbackInfo ci) {
        ServerPlayer sp = (ServerPlayer) (Object) this;

        // Call your spawner handler
        ServerPlayerTickPreMixinEvent.handleServerPLayerTickPreEvent(sp);
    }
}
	

