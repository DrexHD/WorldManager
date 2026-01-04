package me.drex.worldmanager.save;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.DebugLevelSource;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorPreset;
import xyz.nucleoid.fantasy.util.VoidChunkGenerator;

import java.util.LinkedList;
import java.util.List;

public class ChunkGenerators {
    public static final List<Preset> PRESETS = new LinkedList<>();

    public static void init() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            RegistryAccess.Frozen registry = server.registryAccess();
            server.getAllLevels().forEach(level -> {
                ChunkGenerator generator = level.getChunkSource().getGenerator();
                ItemLike icon = Items.STONE;
                if (generator instanceof NoiseBasedChunkGenerator noiseBasedChunkGenerator) {
                    icon = noiseBasedChunkGenerator.generatorSettings().value().defaultBlock().getBlock();
                }
                var title = Component.translatable("generator.minecraft.normal").append(": ").append(Component.literal(level.dimension().identifier().toString()));
                PRESETS.add(new Preset(title, icon, generator));
            });

            registry.lookupOrThrow(Registries.FLAT_LEVEL_GENERATOR_PRESET).listElements().forEach(reference -> {
                ResourceKey<FlatLevelGeneratorPreset> key = reference.key();
                FlatLevelGeneratorPreset flatPreset = reference.value();
                Component title = Component.translatable("generator.minecraft.flat").append(": ")
                    .append(Component.translatable("flat_world_preset." + key.identifier().toLanguageKey()));
                PRESETS.add(new Preset(title, flatPreset.displayItem().value(), new FlatLevelSource(flatPreset.settings())));
            });
            PRESETS.add(new Preset(Component.literal("Void"), Items.STRUCTURE_VOID, new VoidChunkGenerator(registry.lookupOrThrow(Registries.BIOME).get(Biomes.THE_VOID).orElseThrow())));
            PRESETS.add(new Preset(Component.literal("Debug"), Items.COMMAND_BLOCK, new DebugLevelSource(registry.lookupOrThrow(Registries.BIOME).get(Biomes.PLAINS).orElseThrow())));
        });
    }

    public record Preset(Component title, ItemLike icon, ChunkGenerator generator) {
    }
}
