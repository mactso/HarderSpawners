package com.mactso.harderspawners.modloader.events;

import com.mactso.harderspawners.common.commands.MyCommands;
import com.mactso.harderspawners.common.utility.MyUtilities;
import com.mactso.harderspawners.modloader.main.Main;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

public class MyCommandsRegisterEvent {

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            MyUtilities.debugMsg(0, Main.MODID + ": Registering Commands");
            MyCommands.register(dispatcher);
        });
    }
}
