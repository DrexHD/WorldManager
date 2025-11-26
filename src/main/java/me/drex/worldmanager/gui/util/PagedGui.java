package me.drex.worldmanager.gui.util;

import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Items;

import java.util.List;

public abstract class PagedGui<T> extends SimpleGui {
    private static final int WIDTH = 9;
    protected final SimpleGui previousGui;

    private int page = 0;

    public PagedGui(MenuType<ChestMenu> menuType, ServerPlayer player) {
        this(menuType, player, null);
    }

    public PagedGui(MenuType<ChestMenu> menuType, ServerPlayer player, SimpleGui previousGui) {
        super(menuType, player, false);
        this.previousGui = previousGui;
        assert height > 1;
        assert width == WIDTH;
    }

    public void build() {
        List<T> elements = elements();
        var slots = getVirtualSize() - WIDTH;
        for (int slotIndex = 0; slotIndex < slots; slotIndex++) {
            var elementIndex = page * slots + slotIndex;
            if (elementIndex >= elements.size()) {
                clearSlot(slotIndex);
                continue;
            }
            var element = elements.get(elementIndex);
            setSlot(slotIndex, toGuiElement(element));
        }

        for (int i = 0; i < WIDTH; i++) {
            GuiElementBuilder bar = getNavigationBar(i);
            if (bar != null) setSlot(slots + i, bar);
        }
    }

    public GuiElementBuilder getNavigationBar(int index) {
        var slots = getVirtualSize() - WIDTH;
        return switch (index) {
            case 0 -> new GuiElementBuilder(Items.PLAYER_HEAD)
                .setSkullOwner(SkullTextures.ARROW_LEFT)
                .setName(Component.literal("Next Page").withStyle(ChatFormatting.YELLOW))
                .setCallback(() -> {
                    page = Math.max(page - 1, 0);
                    build();
                });
            case 4 -> GuiElements.back(previousGui);
            case 8 -> new GuiElementBuilder(Items.PLAYER_HEAD)
                .setSkullOwner(SkullTextures.ARROW_RIGHT)
                .setName(Component.literal("Previous Page").withStyle(ChatFormatting.YELLOW))
                .setCallback(() -> {
                    var maxPage = (elements().size() - 1) / slots;
                    page = Math.min(page + 1, maxPage);
                    build();
                });
            default -> null;
        };
    }

    protected abstract List<T> elements();

    protected abstract GuiElementBuilder toGuiElement(T t);
}
