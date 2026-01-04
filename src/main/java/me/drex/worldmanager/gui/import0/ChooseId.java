package me.drex.worldmanager.gui.import0;

import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.AnvilInputGui;
import eu.pb4.sgui.api.gui.SimpleGui;
import me.drex.worldmanager.gui.util.SkullTextures;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import java.util.function.Consumer;

public class ChooseId extends AnvilInputGui {
    private final Consumer<Identifier> consumer;
    private final SimpleGui previousGui;

    public ChooseId(ServerPlayer player, Identifier id, Consumer<Identifier> consumer, SimpleGui previousGui) {
        super(player, false);
        this.consumer = consumer;
        this.previousGui = previousGui;
        setTitle(Component.literal("Choose a world id for '" + id + "'"));
        setSlot(1,
            new GuiElementBuilder(Items.PLAYER_HEAD)
                .setName(Component.literal("Back!").withStyle(ChatFormatting.RED, ChatFormatting.BOLD))
                .setSkullOwner(SkullTextures.BACKWARD)
                .setCallback(previousGui::open)
        );
    }

    @Override
    public void onInput(String input) {
        super.onInput(input);
        Identifier id = Identifier.tryParse(input);
        if (id == null) {
            setSlot(2,
                new GuiElementBuilder(Items.RED_STAINED_GLASS_PANE)
                    .setName(Component.literal("Invalid id!").withStyle(ChatFormatting.RED))
            );
            return;
        }

        ResourceKey<Level> resourceKey = ResourceKey.create(Registries.DIMENSION, id);
        ServerLevel level = player.level().getServer().getLevel(resourceKey);
        if (level != null) {
            setSlot(2,
                new GuiElementBuilder(Items.RED_STAINED_GLASS_PANE)
                    .setName(Component.literal("World id is already taken!").withStyle(ChatFormatting.RED))
            );
            return;
        }
        setSlot(2,
            new GuiElementBuilder(Items.GREEN_STAINED_GLASS_PANE)
                .setName(Component.literal("Confirm!").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD))
                .setCallback(() -> {
                    consumer.accept(id);
                    previousGui.open();
                })
        );
    }
}
