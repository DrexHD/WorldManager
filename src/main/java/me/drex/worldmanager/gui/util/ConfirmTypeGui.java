package me.drex.worldmanager.gui.util;

import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.AnvilInputGui;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;

public class ConfirmTypeGui extends AnvilInputGui {
    private final String text;
    private final Runnable action;
    private final SimpleGui previousGui;

    public ConfirmTypeGui(ServerPlayer player, String text, Runnable action, SimpleGui previousGui) {
        super(player, false);
        this.text = text;
        this.action = action;
        this.previousGui = previousGui;
        setTitle(Component.literal("Type '" + text + "' below to confirm!").withStyle(ChatFormatting.DARK_RED));
    }

    @Override
    public void onInput(String input) {
        if (input.equals(text)) {
            setSlot(2,
                new GuiElementBuilder(Items.PLAYER_HEAD)
                    .setName(Component.literal("Confirm!").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD))
                    .setSkullOwner(SkullTextures.CHECKMARK)
                    .setCallback(action)
            );
        } else {
            clearSlot(2);
        }
        setSlot(1,
            new GuiElementBuilder(Items.PLAYER_HEAD)
                .setName(Component.literal("Cancel!").withStyle(ChatFormatting.RED, ChatFormatting.BOLD))
                .setSkullOwner(SkullTextures.BACKWARD)
                .setCallback(previousGui::open)
        );
    }
}
