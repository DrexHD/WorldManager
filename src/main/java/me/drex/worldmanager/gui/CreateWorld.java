package me.drex.worldmanager.gui;

import me.drex.worldmanager.gui.configure.ConfigureWorld;
import me.drex.worldmanager.save.WorldConfig;
import me.drex.worldmanager.save.WorldManagerSavedData;
import me.drex.worldmanager.util.VersionUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import xyz.nucleoid.fantasy.Fantasy;
import xyz.nucleoid.fantasy.RuntimeWorldHandle;

public class CreateWorld extends ConfigureWorld {
    public CreateWorld(ServerPlayer player, Identifier id) {
        super(player, id);
    }

    @Override
    protected void confirm(WorldConfig config) {
        var server = player.level().getServer();
        Fantasy fantasy = Fantasy.get(server);

        RuntimeWorldHandle handle = fantasy.getOrOpenPersistentWorld(id, config.toRuntimeWorldConfig());

        WorldManagerSavedData savedData = WorldManagerSavedData.getSavedData(server);
        savedData.addWorld(id, config, handle);
        player.sendSystemMessage(Component.empty()
            .append(Component.literal("World " + id + " has been created successfully. "))
            .append(Component.literal("Click to teleport!").withStyle(style ->
                    style.withColor(ChatFormatting.AQUA).withUnderlined(true)
                        .withClickEvent(VersionUtil.runCommand("/wm tp " + id))
                )
            ));
    }
}
