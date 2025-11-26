package me.drex.worldmanager.gui;

import eu.pb4.sgui.api.elements.GuiElementBuilder;
import me.drex.worldmanager.command.ExportCommand;
import me.drex.worldmanager.gui.util.PagedGui;
import me.drex.worldmanager.save.WorldConfig;
import me.drex.worldmanager.save.WorldManagerSavedData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import java.util.*;

public class ExportWorlds extends PagedGui<ExportWorlds.Entry> {
    private final Set<ResourceLocation> selectedWorlds = new HashSet<>();

    public ExportWorlds(ServerPlayer player) {
        super(MenuType.GENERIC_9x6, player);
        setTitle(Component.literal("Select Worlds To Export"));
        build();
    }

    @Override
    public GuiElementBuilder getNavigationBar(int index) {
        if (index > 0 && index < 8) {
            return new GuiElementBuilder(Items.GREEN_STAINED_GLASS_PANE)
                .setName(Component.literal("Confirm").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD))
                .setCallback(() -> {
                    close();
                    ExportCommand.exportWorlds(player.createCommandSourceStack(), selectedWorlds);
                });
        }
        return super.getNavigationBar(index);
    }

    @Override
    protected List<Entry> elements() {
        List<Entry> result = new ArrayList<>();
        MinecraftServer server = player.level().getServer();
        server.levelKeys().forEach(key -> {
            ResourceLocation id = key.location();
            WorldConfig config = WorldManagerSavedData.getSavedData(server).getConfig(id);
            GuiElementBuilder builder;
            if (config != null) {
                builder = config.data.iconGuiElement();
            } else {
                Item icon = Items.STONE;
                if (key == Level.OVERWORLD) {
                    icon = Items.GRASS_BLOCK;
                } else if (key == Level.NETHER) {
                    icon = Items.NETHERRACK;
                } else if (key == Level.END) {
                    icon = Items.END_STONE;
                }
                builder = new GuiElementBuilder(icon);
            }
            result.add(new Entry(id, builder));

        });
        return result;
    }

    @Override
    protected GuiElementBuilder toGuiElement(Entry entry) {
        ResourceLocation id = entry.id();
        return entry.icon()
            .setName(Component.literal(id.toString()).withStyle(ChatFormatting.YELLOW))
            .setDamage(selectedWorlds.contains(id) ? 1 : 0)
            .setMaxDamage(Integer.MAX_VALUE)
            .hideDefaultTooltip()
            .addLoreLine(Component.literal("Left click to select / deselect").withStyle(ChatFormatting.GRAY))
            .setCallback(() -> {
                if (selectedWorlds.contains(id)) {
                    selectedWorlds.remove(id);
                } else {
                    selectedWorlds.add(id);
                }
                build();
            });
    }

    public record Entry(ResourceLocation id, GuiElementBuilder icon) {
    }

}
