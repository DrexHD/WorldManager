package me.drex.worldmanager.gui.import0;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import me.drex.worldmanager.command.ImportCommand;
import me.drex.worldmanager.extractor.ArchiveExtractor;
import me.drex.worldmanager.gui.util.PagedGui;
import me.drex.worldmanager.save.WorldConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Items;

import java.util.*;

public class ImportWorlds extends PagedGui<Identifier> {
    private final Map<Identifier, WorldConfig> availableWorlds;
    private final ArchiveExtractor archiveExtractor;
    private final Map<Identifier, Identifier> importWorldIds;

    public ImportWorlds(ServerPlayer player, Map<Identifier, WorldConfig> availableWorlds, ArchiveExtractor archiveExtractor) {
        super(MenuType.GENERIC_9x6, player);
        this.availableWorlds = availableWorlds;
        this.archiveExtractor = archiveExtractor;
        this.importWorldIds = new HashMap<>();
        setTitle(Component.literal("Select Worlds To Import"));
    }

    @Override
    public void onOpen() {
        super.onOpen();
        build();
    }

    @Override
    public GuiElementBuilder getNavigationBar(int index) {
        if (index > 0 && index < 8) {
            GuiElementBuilder builder = new GuiElementBuilder(Items.GREEN_STAINED_GLASS_PANE)
                .setName(Component.literal("Confirm").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD))
                .setCallback(() -> {
                    close();
                    try {
                        ImportCommand.importWorlds(player.createCommandSourceStack(), availableWorlds, importWorldIds, archiveExtractor);
                    } catch (CommandSyntaxException e) {
                        player.sendSystemMessage(Component.literal(e.getMessage()).withStyle(ChatFormatting.RED));
                    }
                });
            builder.addLoreLine(Component.literal("Click to import " + importWorldIds.size() + " world(s).").withStyle(ChatFormatting.GRAY));
            builder.addLoreLine(Component.empty());
            importWorldIds.forEach((id, resourceLocation) ->
                builder.addLoreLine(Component.literal(id.toString() + " -> " + resourceLocation.toString()).withStyle(ChatFormatting.YELLOW))
            );

            return builder;
        }
        return super.getNavigationBar(index);
    }

    @Override
    protected List<Identifier> elements() {
        return new LinkedList<>(availableWorlds.keySet());
    }

    @Override
    protected GuiElementBuilder toGuiElement(Identifier id) {
        boolean hasId = importWorldIds.containsKey(id);
        var builder = new GuiElementBuilder(Items.STONE)
            .hideDefaultTooltip()
            .setCallback(() -> {
                if (hasId) {
                    importWorldIds.remove(id);
                } else {
                    new ChooseId(player, id, resourceLocation -> importWorldIds.put(id, resourceLocation), this).open();
                }
                build();
            });
        if (hasId) {
            builder
                .setItem(Items.LIME_STAINED_GLASS_PANE)
                .setName(Component.literal(id.toString()).withStyle(ChatFormatting.GREEN))
                .addLoreLine(Component.literal("New world id: ").withStyle(ChatFormatting.GRAY).append(Component.literal(importWorldIds.get(id).toString()).withStyle(ChatFormatting.GOLD)))
                .addLoreLine(Component.literal("Click to deselect world").withStyle(ChatFormatting.GRAY));
        } else {
            builder
                .setItem(Items.YELLOW_STAINED_GLASS_PANE)
                .setName(Component.literal(id.toString()).withStyle(ChatFormatting.YELLOW))
                .addLoreLine(Component.literal("Click to pick world id").withStyle(ChatFormatting.GRAY));
        }
        return builder;
    }
}
