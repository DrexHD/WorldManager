package me.drex.worldmanager.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.datafixers.DataFixer;
import com.mojang.serialization.Dynamic;
import me.drex.worldmanager.WorldManager;
import me.drex.worldmanager.extractor.*;
import me.drex.worldmanager.gui.import0.ImportWorlds;
import me.drex.worldmanager.mixin.MinecraftServerAccessor;
import me.drex.worldmanager.save.Location;
import me.drex.worldmanager.save.WorldConfig;
import me.drex.worldmanager.save.WorldData;
import me.drex.worldmanager.save.WorldManagerSavedData;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.io.FilenameUtils;
import xyz.nucleoid.fantasy.Fantasy;
import xyz.nucleoid.fantasy.RuntimeWorldHandle;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
//? if >= 1.21.5 {
import java.util.stream.Collectors;
import java.util.stream.Stream;
//? }

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class ImportCommand {

    public static final Set<String> DIMENSION_PREFIXES = Set.of("data", "region", "entities", "poi");
    private static final List<ArchiveExtractor> EXTRACTORS = List.of(
        new FolderArchiveExtractor(),
        new ZipArchiveExtractor(),
        new RarArchiveExtractor(),
        new TarGzArchiveExtractor()
    );
    public static final SuggestionProvider<CommandSourceStack> PATHS = (context, builder) ->
    {
        try {
            return SharedSuggestionProvider.suggest(
                Files.list(FabricLoader.getInstance().getGameDir()).filter(path -> {
                    if (Files.isDirectory(path)) {
                        return true;
                    } else {
                        return EXTRACTORS.stream().anyMatch(archiveExtractor -> archiveExtractor.supports(path));
                    }
                }).map(path -> "\"" + path + "\"").toList(), builder
            );
        } catch (IOException e) {
            return Suggestions.empty();
        }
    };

    public static final DynamicCommandExceptionType MISSING_LEVEL_DAT = new DynamicCommandExceptionType((file) -> Component.literal("Failed to find/read level.dat in '" + file + "'"));
    public static final DynamicCommandExceptionType RAR5 = new DynamicCommandExceptionType((file) -> Component.literal("Failed to extract rar file '" + file + "'. RAR v5 is not supported, please extract manually!"));
    public static final Dynamic2CommandExceptionType UNKNOWN_EXTENSION = new Dynamic2CommandExceptionType((file, extension) -> Component.literal("Failed to extract file '" + file + "'. Unknown extension: '" + extension + "'!"));
    public static final DynamicCommandExceptionType IO_EXCEPTION = new DynamicCommandExceptionType((world) -> Component.literal("Failed to extract/copy world '" + world + "'. Check console for more details!"));

    public static LiteralArgumentBuilder<CommandSourceStack> build() {
        return literal("import")
            .requires(Permissions.require("worldmanager.command.worldmanager.import", 4))
            .then(
                argument("path", StringArgumentType.string())
                    .suggests(PATHS)
                    .executes(context -> openGui(context.getSource(), StringArgumentType.getString(context, "path")))
            );
    }

    public static int openGui(CommandSourceStack source, String localPath) throws CommandSyntaxException {
        MinecraftServer server = source.getServer();
        ServerPlayer player = source.getPlayerOrException();

        Path fullPath = FabricLoader.getInstance().getGameDir().resolve(localPath);
        Map<ResourceLocation, WorldConfig> worldConfigs;
        try {
            Optional<ArchiveExtractor> extractor = EXTRACTORS.stream()
                .filter(e -> e.supports(fullPath))
                .findFirst();
            if (extractor.isEmpty()) {
                throw UNKNOWN_EXTENSION.create(fullPath, FilenameUtils.getExtension(fullPath.toString()));
            }

            worldConfigs = extractor.get().open(fullPath, server);
            if (worldConfigs.isEmpty()) throw MISSING_LEVEL_DAT.create(fullPath);

            new ImportWorlds(player, worldConfigs, extractor.get()).open();
            return 1;
        } catch (IOException e) {
            WorldManager.LOGGER.error("Failed to open world archive", e);
            throw IO_EXCEPTION.create(fullPath);
        }
    }

    public static int importWorlds(CommandSourceStack source, Map<ResourceLocation, WorldConfig> worldConfigs, Map<ResourceLocation, ResourceLocation> importWorldIds, ArchiveExtractor archiveExtractor) throws CommandSyntaxException {
        MinecraftServer server = source.getServer();
        Fantasy fantasy = Fantasy.get(server);

        for (Map.Entry<ResourceLocation, ResourceLocation> entry : importWorldIds.entrySet()) {
            ResourceLocation originalId = entry.getKey();
            ResourceLocation newId = entry.getValue();

            ResourceKey<Level> resourceKey = ResourceKey.create(Registries.DIMENSION, newId);
            ServerLevel level = server.getLevel(resourceKey);
            if (level != null) continue; // world already exists

            WorldConfig config = worldConfigs.get(originalId);
            if (config == null) continue; // shouldn't happen

            LevelStorageSource.LevelStorageAccess storageSource = ((MinecraftServerAccessor) server).getStorageSource();
            Path levelDirectory = storageSource.getLevelDirectory().path();
            Path sourcePath = storageSource.getDimensionPath(ResourceKey.create(Registries.DIMENSION, originalId));
            Path targetRootPath = storageSource.getDimensionPath(ResourceKey.create(Registries.DIMENSION, newId));

            try {
                archiveExtractor.extract(levelDirectory.relativize(sourcePath), targetRootPath, server);
            } catch (IOException e) {
                WorldManager.LOGGER.error("Failed to extract world", e);
                throw IO_EXCEPTION.create(newId.toString());
            }

            RuntimeWorldHandle handle = fantasy.getOrOpenPersistentWorld(newId, config.toRuntimeWorldConfig());
            WorldManagerSavedData savedData = WorldManagerSavedData.getSavedData(server);
            savedData.addWorld(newId, config, handle);
        }
        source.sendSuccess(() -> Component.empty().append(Component.literal("Successfully imported " + importWorldIds.size() + " worlds.")), false);
        try {
            archiveExtractor.close();
        } catch (IOException e) {
            WorldManager.LOGGER.error("Failed to close archive extractor", e);
        }
        return 1;
    }

    //? if >= 1.21.5 {
    public static Map<ResourceLocation, WorldConfig> parseWorldConfig(InputStream is, MinecraftServer server) throws IOException {
        CompoundTag tag = NbtIo.readCompressed(is, NbtAccounter.unlimitedHeap());

        Optional<CompoundTag> unfixedData = tag.getCompound("Data");
        if (unfixedData.isEmpty()) return Collections.emptyMap();
//        CompoundTag data = fixLevelData(unfixedData.get()).orElse(unfixedData.get());
        CompoundTag data = unfixedData.get();

        var spawnX = data.getIntOr("SpawnX", 0);
        var spawnY = data.getIntOr("SpawnY", 0);
        var spawnZ = data.getIntOr("SpawnZ", 0);
        var spawnAngle = data.getFloatOr("SpawnAngle", 0);
        return data.getCompound("WorldGenSettings")
            .flatMap(worldGenSettings -> {
                long seed = worldGenSettings.getLongOr("seed", 0);
                return worldGenSettings.getCompound("dimensions")
                    .map(compoundTag -> {
                        Stream<Map.Entry<ResourceLocation, Optional<WorldConfig>>> stream = compoundTag.entrySet().stream().map(stringTagEntry -> {
                            String dimensionKey = stringTagEntry.getKey();
                            ResourceLocation id = ResourceLocation.tryParse(dimensionKey);
                            return Map.entry(id, createWorldConfig(server, stringTagEntry.getValue(), spawnX, spawnY, spawnZ, spawnAngle, seed));
                        }).filter(entry -> entry.getKey() != null && entry.getValue().isPresent());
                        return stream.collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().orElseThrow()));
                    });
            }).orElse(Collections.emptyMap());
    }
    //?} else {
    /*public static Map<ResourceLocation, WorldConfig> parseWorldConfig(InputStream is, MinecraftServer server) throws IOException {
        Map<ResourceLocation, WorldConfig> configs = new HashMap<>();
        CompoundTag tag = NbtIo.readCompressed(is, NbtAccounter.unlimitedHeap());
        var unfixedData = tag.getCompound("Data");
//        var data = fixLevelData(unfixedData).orElse(unfixedData);
        var data = unfixedData;

        var spawnX = data.getInt("SpawnX");
        var spawnY = data.getInt("SpawnY");
        var spawnZ = data.getInt("SpawnZ");
        var spawnAngle = data.getFloat("SpawnAngle");
        var worldGenSettings = data.getCompound("WorldGenSettings");
        long seed = worldGenSettings.getLong("seed");
        CompoundTag dimensions = worldGenSettings.getCompound("dimensions");
        for (String dimensionKey : dimensions.getAllKeys()) {
            var dimension = dimensions.getCompound(dimensionKey);
            Optional<WorldConfig> worldConfig = createWorldConfig(server, dimension, spawnX, spawnY, spawnZ, spawnAngle, seed);
            ResourceLocation resourceLocation = ResourceLocation.tryParse(dimensionKey);

            if (resourceLocation != null && worldConfig.isPresent()) {
                configs.put(resourceLocation, worldConfig.get());
            }
        }
        return configs;
    }
    *///?}

    public static Optional<CompoundTag> fixLevelData(CompoundTag levelData) {
        int dataVersion = NbtUtils.getDataVersion(levelData, -1);
        DataFixer dataFixer = DataFixers.getDataFixer();

        Dynamic<Tag> dynamic = DataFixTypes.LEVEL.updateToCurrentVersion(dataFixer, new Dynamic<>(NbtOps.INSTANCE, levelData), dataVersion)
            .update("Player", playerDynamic ->
                DataFixTypes.PLAYER.updateToCurrentVersion(dataFixer, playerDynamic, dataVersion))
            .update("WorldGenSettings", worldGenDynamic ->
                DataFixTypes.WORLD_GEN_SETTINGS.updateToCurrentVersion(dataFixer, worldGenDynamic, dataVersion));

        return dynamic.getValue() instanceof CompoundTag compoundTag ?
            Optional.of(compoundTag) :
            Optional.empty();
    }

    private static Optional<WorldConfig> createWorldConfig(MinecraftServer server, Tag overworld, int spawnX, int spawnY, int spawnZ, float spawnAngle, long seed) {
        try {
            WorldConfig config = WorldConfig.CODEC.decode(RegistryOps.create(NbtOps.INSTANCE, server.registryAccess()), overworld)
                .getOrThrow(IllegalStateException::new)
                .getFirst();
            WorldData worldData = new WorldData();
            worldData.spawnLocation = Optional.of(new Location(new Vec3(spawnX, spawnY, spawnZ), new Vec2(spawnAngle, 0)));
            config.seed = seed;
            config.data = worldData;
            return Optional.of(config);
        } catch (IllegalStateException e) {
            WorldManager.LOGGER.error("Failed to decode level.dat", e);
            return Optional.empty();
        }
    }

}
