package me.drex.worldmanager.gui.configure;

import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import me.drex.worldmanager.gui.util.PagedGui;
import me.drex.worldmanager.save.ChunkGenerators;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.chunk.ChunkGenerator;

import java.util.List;
import java.util.function.Consumer;

public class SelectChunkGenerator extends PagedGui<ChunkGenerators.Preset> {
    private final Consumer<ChunkGenerator> consumer;

    public SelectChunkGenerator(ServerPlayer player, SimpleGui previousGui, Consumer<ChunkGenerator> consumer) {
        super(MenuType.GENERIC_9x3, player, previousGui);
        this.consumer = consumer;
        setTitle(Component.literal("Select Chunk Generator"));
        build();
    }

    @Override
    protected List<ChunkGenerators.Preset> elements() {
        return ChunkGenerators.PRESETS;
    }

    @Override
    protected GuiElementBuilder toGuiElement(ChunkGenerators.Preset preset) {
        var chunkGenerator = preset.generator();
        return new GuiElementBuilder(preset.icon().asItem())
            .setName(preset.title())
            .setCallback(() -> {
                consumer.accept(chunkGenerator);
                previousGui.open();
            });
    }
}
