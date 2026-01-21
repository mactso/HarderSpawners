package com.mactso.harderspawners.modloader.events;
import com.mactso.harderspawners.common.commands.MyCommands;
import com.mactso.harderspawners.common.utility.MyUtilities;
import com.mactso.harderspawners.modloader.main.Main;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public class MyCommandsRegisterEvent {
    @SubscribeEvent
    public void onCommandsRegistry(RegisterCommandsEvent event) {
        MyUtilities.debugMsg(0, Main.MODID + ": Registering Commands");
        MyCommands.register(event.getDispatcher());
    }
}



