package me.drex.worldmanager.gui.util;

import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;

public class GuiElements {
    public static GuiElementBuilder back(SimpleGui previousGui) {
        if (previousGui != null) {
            return new GuiElementBuilder(Items.PLAYER_HEAD)
                .setSkullOwner(SkullTextures.BACKWARD)
                .setCallback(previousGui::open)
                .setName(Component.literal("Back").withStyle(ChatFormatting.RED));
        } else {
            return new GuiElementBuilder(Items.AIR);
        }
    }
}
