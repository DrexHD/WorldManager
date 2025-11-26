package me.drex.worldmanager.util;

import net.minecraft.network.chat.ClickEvent;

public class VersionUtil {
    public static ClickEvent runCommand(String command) {
        //? if >= 1.21.5 {
        return new ClickEvent.RunCommand(command);
        //?} else {
        /*return new ClickEvent(ClickEvent.Action.RUN_COMMAND, command);
        *///?}
    }
}
