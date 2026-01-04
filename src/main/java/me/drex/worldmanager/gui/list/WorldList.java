package me.drex.worldmanager.gui.list;

import eu.pb4.sgui.api.elements.GuiElementBuilder;
import me.drex.worldmanager.command.TeleportCommand;
import me.drex.worldmanager.gui.util.PagedGui;
import me.drex.worldmanager.save.WorldConfig;
import me.drex.worldmanager.save.WorldManagerSavedData;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.MenuType;

import java.util.List;
import java.util.Map;

public class WorldList extends PagedGui<Map.Entry<Identifier, WorldConfig>> {
    public WorldList(ServerPlayer player) {
        super(MenuType.GENERIC_9x6, player);
        setTitle(Component.literal("World List"));
        build();
    }

    @Override
    protected List<Map.Entry<Identifier, WorldConfig>> elements() {
        return WorldManagerSavedData.getSavedData(player.level().getServer()).getWorlds().entrySet().stream().toList();
    }

    @Override
    protected GuiElementBuilder toGuiElement(Map.Entry<Identifier, WorldConfig> entry) {
        WorldConfig config = entry.getValue();
        Identifier id = entry.getKey();
        return config.data.iconGuiElement().setName(Component.literal(id.toString()).withStyle(ChatFormatting.YELLOW))
            .addLoreLine(Component.literal("Left Click to teleport!").withStyle(ChatFormatting.GRAY))
            .addLoreLine(Component.literal("Right Click to manage world!").withStyle(ChatFormatting.GRAY))
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
