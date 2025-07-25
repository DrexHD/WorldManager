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
import me.drex.worldmanager.gui.ImportWorld;
import me.drex.worldmanager.mixin.MinecraftServerAccessor;
import me.drex.worldmanager.save.Location;
import me.drex.worldmanager.save.WorldConfig;
import me.drex.worldmanager.save.WorldData;
import me.drex.worldmanager.save.WorldManagerSavedData;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.util.datafix.DataFixers;
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

import static me.drex.message.api.LocalizedMessage.builder;
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

    public static final DynamicCommandExceptionType MISSING_LEVEL_DAT = new DynamicCommandExceptionType((file) -> builder("worldmanager.command.import.exception.level_dat").addPlaceholder("file", file.toString()).build());
    public static final DynamicCommandExceptionType RAR5 = new DynamicCommandExceptionType((file) -> builder("worldmanager.command.import.exception.rar5").addPlaceholder("file", file.toString()).build());
    public static final Dynamic2CommandExceptionType UNKNOWN_EXTENSION = new Dynamic2CommandExceptionType((file, extension) -> builder("worldmanager.command.import.exception.unknown_extension")
        .addPlaceholder("file", file.toString())
        .addPlaceholder("extension", extension.toString())
        .build());
    public static final DynamicCommandExceptionType IO_EXCEPTION = new DynamicCommandExceptionType((file) -> builder("worldmanager.command.import.exception.ioexception").addPlaceholder("file", file.toString()).build());

    public static LiteralArgumentBuilder<CommandSourceStack> build() {
        return literal("import")
            .requires(Permissions.require("worldmanager.command.worldmanager.import", 4))
            .then(
                argument("id", ResourceLocationArgument.id())
                    .then(
                        argument("path", StringArgumentType.string())
                            .suggests(PATHS)
                            .executes(context -> importWorld(context.getSource(), ResourceLocationArgument.getId(context, "id"), StringArgumentType.getString(context, "path"), false))
                            .then(
                                Commands.literal("--custom-config")
                                    .executes(context -> importWorld(context.getSource(), ResourceLocationArgument.getId(context, "id"), StringArgumentType.getString(context, "path"), true))
                            )
                    )
            );
    }

    public static int importWorld(CommandSourceStack source, ResourceLocation id, String localPath, boolean customConfig) throws CommandSyntaxException {
        MinecraftServer server = source.getServer();
        CreateCommand.validLevelId(id, server);

        Fantasy fantasy = Fantasy.get(server);
        LevelStorageSource.LevelStorageAccess storageSource = ((MinecraftServerAccessor) server).getStorageSource();
        Path targetPath = storageSource.getDimensionPath(ResourceKey.create(Registries.DIMENSION, id));

        Path fullPath = FabricLoader.getInstance().getGameDir().resolve(localPath);
        WorldConfig config;
        try {
            Optional<ArchiveExtractor> extractor = EXTRACTORS.stream()
                .filter(e -> e.supports(fullPath))
                .findFirst();
            if (extractor.isEmpty()) {
                throw UNKNOWN_EXTENSION.create(fullPath, FilenameUtils.getExtension(fullPath.toString()));
            }
            config = extractor.get().extract(fullPath, targetPath, server);
        } catch (IOException e) {
            throw IO_EXCEPTION.create(fullPath);
        }
        if (customConfig) {
            new ImportWorld(source.getPlayerOrException(), id).open();
        } else {
            RuntimeWorldHandle handle = fantasy.getOrOpenPersistentWorld(id, config.toRuntimeWorldConfig());
            WorldManagerSavedData savedData = WorldManagerSavedData.getSavedData(server);
            savedData.addWorld(id, config, handle);
            source.sendSuccess(() -> builder("worldmanager.command.import").addPlaceholder("id", id.toString()).build(), false);
        }

        return 1;
    }

    //? if >= 1.21.5 {
    public static Optional<WorldConfig> parseWorldConfig(InputStream is, MinecraftServer server) throws IOException {
        CompoundTag tag = NbtIo.readCompressed(is, NbtAccounter.unlimitedHeap());

        Optional<CompoundTag> unfixedData = tag.getCompound("Data");
        if (unfixedData.isEmpty()) return Optional.empty();
        CompoundTag data = fixLevelData(unfixedData.get()).orElse(unfixedData.get());

        var spawnX = data.getIntOr("SpawnX", 0);
        var spawnY = data.getIntOr("SpawnY", 0);
        var spawnZ = data.getIntOr("SpawnZ", 0);
        var spawnAngle = data.getFloatOr("SpawnAngle", 0);
        return data.getCompound("WorldGenSettings")
            .flatMap(worldGenSettings -> {
                long seed = worldGenSettings.getLongOr("seed", 0);
                return worldGenSettings.getCompound("dimensions")
                    // TODO add option to pick dimension
                    .flatMap(dimensions -> dimensions.getCompound("minecraft:overworld")
                        .flatMap(overworld -> {
                            return createWorldConfig(server, overworld, spawnX, spawnY, spawnZ, spawnAngle, seed);
                        }));
            });
    }
    //?} else {
    /*public static Optional<WorldConfig> parseWorldConfig(InputStream is, MinecraftServer server) throws IOException {
        CompoundTag tag = NbtIo.readCompressed(is, NbtAccounter.unlimitedHeap());
        var unfixedData = tag.getCompound("Data");
        var data = fixLevelData(unfixedData).orElse(unfixedData);

        var spawnX = data.getInt("SpawnX");
        var spawnY = data.getInt("SpawnY");
        var spawnZ = data.getInt("SpawnZ");
        var spawnAngle = data.getFloat("SpawnAngle");
        var worldGenSettings = data.getCompound("WorldGenSettings");
        long seed = worldGenSettings.getLong("seed");
        // TODO add option to pick dimension
        var overworld = worldGenSettings.getCompound("dimensions").getCompound("minecraft:overworld");
        return createWorldConfig(server, overworld, spawnX, spawnY, spawnZ, spawnAngle, seed);
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

    private static Optional<WorldConfig> createWorldConfig(MinecraftServer server, CompoundTag overworld, int spawnX, int spawnY, int spawnZ, float spawnAngle, long seed) {
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
