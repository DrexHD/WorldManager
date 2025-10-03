package me.drex.worldmanager.gui.list;

import eu.pb4.sgui.api.elements.GuiElementBuilder;
import me.drex.worldmanager.command.TeleportCommand;
import me.drex.worldmanager.gui.util.PagedGui;
import me.drex.worldmanager.save.WorldConfig;
import me.drex.worldmanager.save.WorldManagerSavedData;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.MenuType;

import java.util.List;
import java.util.Map;

import static me.drex.message.api.LocalizedMessage.builder;
import static me.drex.message.api.LocalizedMessage.localized;

public class WorldList extends PagedGui<Map.Entry<ResourceLocation, WorldConfig>> {
    public WorldList(ServerPlayer player) {
        super(MenuType.GENERIC_9x6, player);
        setTitle(localized("worldmanager.gui.world_list.title"));
        build();
    }

    @Override
    protected List<Map.Entry<ResourceLocation, WorldConfig>> elements() {
        return WorldManagerSavedData.getSavedData(player.level().getServer()).getWorlds().entrySet().stream().toList();
    }

    @Override
    protected GuiElementBuilder toGuiElement(Map.Entry<ResourceLocation, WorldConfig> entry) {
        WorldConfig config = entry.getValue();
        ResourceLocation id = entry.getKey();
        return config.data.iconGuiElement().setName(builder("worldmanager.gui.world_list.entry.name").addPlaceholder("id", id.toString()).build())
            .addLoreLine(builder("worldmanager.gui.world_list.entry.lore").addPlaceholder("id", id.toString()).build())
            .setCallback(clickType -> {
                if (clickType.isLeft) {
                    if (Permissions.check(player, "worldmanager.command.worldmanager.teleport", 2)) {
                        if (TeleportCommand.teleport(player, entry.getValue(), id)) {
                            close();
                        }
                    }
                } else if (clickType.isRight) {
                    if (Permissions.check(player, "worldmanager.gui.manage", 2)) {
                        new ManageWorld(player, id, config, this).open();
                    }
                }
            });
    }
}
