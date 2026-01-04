package me.drex.worldmanager.gui.list;

import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import me.drex.worldmanager.command.SpawnCommand;
import me.drex.worldmanager.command.TeleportCommand;
import me.drex.worldmanager.gui.util.ConfirmTypeGui;
import me.drex.worldmanager.gui.util.GuiElements;
import me.drex.worldmanager.save.WorldConfig;
import me.drex.worldmanager.save.WorldManagerSavedData;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class ManageWorld extends SimpleGui {
    private final Identifier id;
    private final WorldConfig config;
    private final SimpleGui previousGui;

    public ManageWorld(ServerPlayer player, Identifier id, WorldConfig config, SimpleGui previousGui) {
        super(MenuType.GENERIC_9x3, player, false);
        this.id = id;
        this.config = config;
        this.previousGui = previousGui;
        setTitle(Component.literal("Manage World " + id));
        build();
    }

    public void build() {
        setSlot(0,
            new GuiElementBuilder(Items.ENDER_PEARL)
                .setName(Component.literal("Teleport Back").withStyle(ChatFormatting.AQUA))
                .addLoreLine(Component.literal("Teleport to your last location in the world.").withStyle(ChatFormatting.GRAY))
                .setCallback(() -> {
                    if (Permissions.check(player, "worldmanager.command.worldmanager.teleport", 2)) {
                        if (TeleportCommand.teleport(player, config, id)) {
                            close();
                        }
                    }
                })
        );
        setSlot(1,
            new GuiElementBuilder(Items.GRASS_BLOCK)
                .setName(Component.literal("Teleport To Spawn").withStyle(ChatFormatting.GREEN))
                .addLoreLine(Component.literal("Teleport to the world spawn.").withStyle(ChatFormatting.GRAY))
                .setCallback(() -> {
                    if (Permissions.check(player, "worldmanager.command.worldmanager.spawn", 2)) {
                        if (SpawnCommand.spawn(player, config, id)) {
                            close();
                        }
                    }
                })
        );
        setSlot(2,
            config.data.iconGuiElement()
                .setName(Component.literal("Change World Icon").withStyle(ChatFormatting.YELLOW))
                .addLoreLine(Component.literal("Set the world icon to the item in your hand.").withStyle(ChatFormatting.GRAY))
                .setCallback(() -> {
                    if (Permissions.check(player, "worldmanager.command.worldmanager.seticon", 2)) {
                        ItemStack item = player.getMainHandItem();
                        if (!item.isEmpty()) {
                            config.data.icon = item;
                        }
                        WorldManagerSavedData.getSavedData(player.level().getServer()).setDirty();
                        build();
                    }
                })
        );
        setSlot(18, GuiElements.back(previousGui));
        setSlot(26,
            new GuiElementBuilder(Items.TNT)
                .setName(Component.literal("Delete World").withStyle(ChatFormatting.RED, ChatFormatting.BOLD))
                .addLoreLine(Component.literal("Deletes the world, kicking all players that are currently in it.").withStyle(ChatFormatting.GRAY))
                .setCallback(() -> {
                    if (Permissions.check(player, "worldmanager.command.worldmanager.delete", 2)) {
                        new ConfirmTypeGui(player, id.toString(), () -> {
                            WorldManagerSavedData savedData = WorldManagerSavedData.getSavedData(player.level().getServer());
                            if (savedData.removeWorld(id)) {
                                close();
                            } else {
                                this.open();
                            }
                        }, this).open();
                    }
                })
        );
    }
}
