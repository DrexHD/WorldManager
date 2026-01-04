package me.drex.worldmanager.gui.configure;

import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import me.drex.worldmanager.save.WorldConfig;
import me.drex.worldmanager.save.WorldData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;

public abstract class ConfigureWorld extends SimpleGui {

    protected final Identifier id;
    protected Holder<DimensionType> type;
    protected ChunkGenerator generator;
    protected long seed;
    protected boolean tickTime;

    public ConfigureWorld(ServerPlayer player, Identifier id) {
        super(MenuType.GENERIC_9x3, player, false);
        this.id = id;
        setTitle(Component.literal("Configure World " + id));
        setupDefaults(player.level().getServer());
        build();
    }

    private void setupDefaults(MinecraftServer server) {
        RegistryAccess.Frozen frozen = server.registryAccess();
        HolderLookup<DimensionType> dimensionType = frozen.lookupOrThrow(Registries.DIMENSION_TYPE);
        this.type = dimensionType.getOrThrow(BuiltinDimensionTypes.OVERWORLD);
        this.generator = server.overworld().getChunkSource().getGenerator();
        this.seed = 0;
        tickTime = true;
    }

    public void build() {
        setSlot(0,
            new GuiElementBuilder(Items.ARMOR_STAND)
                .setName(Component.literal("Dimension Type").withStyle(ChatFormatting.GREEN))
                .addLoreLine(Component.literal("World height, sky light, beds working, ...").withStyle(ChatFormatting.GRAY))
                .addLoreLine(Component.literal("Current: ").withStyle(ChatFormatting.GRAY).append(Component.literal(type.unwrapKey().map(resourceKey -> resourceKey.identifier()).map(Identifier::toString).orElse("???")).withStyle(ChatFormatting.GOLD)))
                .setCallback(() -> {
                    new SelectDimensionType(player, this, type -> {
                        this.type = type;
                        build();
                    }).open();
                })
        );
        setSlot(1,
            new GuiElementBuilder(Items.GRASS_BLOCK)
                .setName(Component.literal("Chunk Generator").withStyle(ChatFormatting.GREEN))
                .addLoreLine(Component.literal("How the world is shaped").withStyle(ChatFormatting.GRAY))
                .addLoreLine(Component.literal("Current: ").withStyle(ChatFormatting.GRAY).append(Component.literal(generator.getTypeNameForDataFixer().map(resourceKey -> resourceKey.identifier()).map(Identifier::toString).orElse("???")).withStyle(ChatFormatting.GOLD)))
                .setCallback(() -> {
                    new SelectChunkGenerator(player, this, generator -> {
                        this.generator = generator;
                        build();
                    }).open();
                })
        );
        setSlot(2,
            new GuiElementBuilder(Items.WHEAT_SEEDS)
                .setName(Component.literal("World Seed").withStyle(ChatFormatting.LIGHT_PURPLE))
                .addLoreLine(Component.literal("Current: ").withStyle(ChatFormatting.GRAY).append(Component.literal(String.valueOf(seed)).withStyle(ChatFormatting.GOLD)))
                .setCallback(() -> {
                    new SelectSeed(player, this, seed -> {
                        this.seed = seed;
                        build();
                    }).open();
                })
        );
        setSlot(3,
            new GuiElementBuilder(Items.CLOCK)
                .setName(Component.literal("Time Ticking").withStyle(ChatFormatting.AQUA))
                .addLoreLine(Component.literal("Enable / disable daylight cycle").withStyle(ChatFormatting.GRAY))
                .addLoreLine(Component.literal("Current: ").withStyle(ChatFormatting.GRAY).append(Component.literal(tickTime ? "Enabled" : "Disabled").withStyle(tickTime ? ChatFormatting.GREEN : ChatFormatting.RED)))
                .setCallback(() -> {
                    this.tickTime = !this.tickTime;
                    build();
                })
        );

        for (int i = 2 * 9; i < 3 * 9; i++) {
            setSlot(i,
                new GuiElementBuilder(Items.GREEN_STAINED_GLASS_PANE)
                    .setName(Component.literal("Confirm!").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD))
                    .setCallback(() -> {
                        WorldConfig config = new WorldConfig(
                            type,
                            generator,
                            seed,
                            tickTime,
                            new WorldData()
                        );
                        confirm(config);
                        close();
                    })
            );
        }
    }

    protected abstract void confirm(WorldConfig config);
}
