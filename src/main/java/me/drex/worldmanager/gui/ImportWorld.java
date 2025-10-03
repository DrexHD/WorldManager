package me.drex.worldmanager.gui;

import me.drex.worldmanager.gui.configure.ConfigureWorld;
import me.drex.worldmanager.save.WorldConfig;
import me.drex.worldmanager.save.WorldManagerSavedData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import xyz.nucleoid.fantasy.Fantasy;
import xyz.nucleoid.fantasy.RuntimeWorldHandle;

import static me.drex.message.api.LocalizedMessage.builder;

public class ImportWorld extends ConfigureWorld {
    public ImportWorld(ServerPlayer player, ResourceLocation id) {
        super(player, id);
    }

    @Override
    protected void confirm(WorldConfig config) {
        var server = player.level().getServer();
        Fantasy fantasy = Fantasy.get(server);

        RuntimeWorldHandle handle = fantasy.getOrOpenPersistentWorld(id, config.toRuntimeWorldConfig());
        WorldManagerSavedData savedData = WorldManagerSavedData.getSavedData(server);
        savedData.addWorld(id, config, handle);
        player.sendSystemMessage(builder("worldmanager.command.import").addPlaceholder("id", id.toString()).build(), false);
    }
}
