package me.drex.worldmanager.command;

//? if >= 1.21.9 {
import com.google.gson.JsonArray;
//?}
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.serialization.JsonOps;
import me.drex.worldmanager.WorldManager;
import me.drex.worldmanager.gui.ExportWorlds;
import me.drex.worldmanager.mixin.MinecraftServerAccessor;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.WorldVersion;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.DataVersion;
import net.minecraft.world.level.storage.LevelStorageSource;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static me.drex.worldmanager.command.WorldManagerCommand.WORLD_SUGGESTIONS;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class ExportCommand {
    public static LiteralArgumentBuilder<CommandSourceStack> build() {
        return literal("export")
            .requires(Permissions.require("worldmanager.command.worldmanager.export", 4))
            .executes(context -> {
                new ExportWorlds(context.getSource().getPlayerOrException()).open();
                return 1;
            })
            .then(
                argument("id", IdentifierArgument.id())
                    .suggests(WORLD_SUGGESTIONS)
                    .executes(context -> exportWorlds(context.getSource(), Collections.singletonList(IdentifierArgument.getId(context, "id"))))
            );
    }

    public static int exportWorlds(CommandSourceStack source, Collection<Identifier> ids) {
        try {
            Path exportFile = FabricLoader.getInstance().getGameDir().resolve("export.zip");
            if (Files.exists(exportFile)) {
                // TODO Add option to choose file or overwrite
//                Files.delete(exportFile);
                source.sendFailure(Component.literal("An export.zip file already exists, please remove before exporting!"));
                return 0;
            }
            ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(exportFile));
            MinecraftServer server = source.getServer();
            CompoundTag dimensionsTag = new CompoundTag();

            for (Identifier id : ids) {
                ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, id);
                ServerLevel level = server.getLevel(key);

                if (level == null) {
                    continue;
                }
                Holder<DimensionType> type = level.dimensionTypeRegistration();
                ChunkGenerator generator = level.getChunkSource().getGenerator();
                source.sendSuccess(() -> Component.literal("Saving world " + id.toString() + " ..."), false);
                level.save(null, true, false);
                source.sendSuccess(() -> Component.literal("Exporting world " + id.toString() + " ..."), false);

                LevelStorageSource.LevelStorageAccess storageSource = ((MinecraftServerAccessor) server).getStorageSource();
                Path targetPath = storageSource.getDimensionPath(key);
                try (var pathStream = Files.walk(targetPath)) {
                    pathStream.forEach(path -> {
                        if (Files.isDirectory(path) || path.getNameCount() <= 2) return;
                        // strip "./world/" from path
                        Path subPath = path.subpath(2, path.getNameCount());
                        Path relativize = targetPath.relativize(path);
                        Path prefix = relativize.getName(0);
                        if (!ImportCommand.DIMENSION_PREFIXES.contains(prefix.toString())) return;

                        ZipEntry zipEntry = new ZipEntry(subPath.toString());
                        try {
                            out.putNextEntry(zipEntry);
                            out.write(Files.readAllBytes(path));
                            out.closeEntry();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                CompoundTag dimensionTag = new CompoundTag();
                RegistryOps<Tag> nbtOps = server.registryAccess().createSerializationContext(NbtOps.INSTANCE);
                dimensionTag.put("type", DimensionType.CODEC.encodeStart(nbtOps, type).getOrThrow());
                dimensionTag.put("generator", ChunkGenerator.CODEC.encodeStart(nbtOps, generator).getOrThrow());
                dimensionsTag.put(id.toString(), dimensionTag);

                // datapack
                // dimension json
                ZipEntry dimensionEntry = new ZipEntry("datapacks/worldmanager/data/" + id.getNamespace() + "/dimension/" + id.getPath() + ".json");
                RegistryOps<JsonElement> jsonOps = server.registryAccess().createSerializationContext(JsonOps.INSTANCE);
                JsonObject root = new JsonObject();
                root.add("type", DimensionType.CODEC.encodeStart(jsonOps, type).getOrThrow());
                root.add("generator", ChunkGenerator.CODEC.encodeStart(jsonOps, generator).getOrThrow());
                out.putNextEntry(dimensionEntry);
                out.write(root.toString().getBytes());
                out.closeEntry();
            }

            // datapack
            // pack.mcmeta
            ZipEntry packMcMetaEntry = new ZipEntry("datapacks/worldmanager/pack.mcmeta");
            JsonObject packMcMeta = new JsonObject();
            JsonObject pack = new JsonObject();
            //? if >= 1.21.9 {
            JsonArray format = new JsonArray();
            format.add(SharedConstants.DATA_PACK_FORMAT_MAJOR);
            format.add(SharedConstants.DATA_PACK_FORMAT_MINOR);
            pack.add("min_format", format);
            pack.add("max_format", format);
            //?} else {
            /*pack.addProperty("pack_format", SharedConstants.DATA_PACK_FORMAT);
            *///?}
            pack.addProperty("description", "Autogenerated WorldManager world export datapack");

            packMcMeta.add("pack", pack);

            out.putNextEntry(packMcMetaEntry);
            out.write(packMcMeta.toString().getBytes());
            out.closeEntry();

            // level.dat
            ZipEntry levelDatEntry = new ZipEntry("level.dat");
            out.putNextEntry(levelDatEntry);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            NbtIo.writeCompressed(writeLevelDat(dimensionsTag), baos);
            out.write(baos.toByteArray());
            out.closeEntry();

            out.close();
        } catch (IOException e) {
            source.sendFailure(Component.literal("Failed to export worlds, check console for details!"));
            WorldManager.LOGGER.error("Failed to export worlds", e);
        }
        source.sendSuccess(() -> Component.literal("Successfully exported " + ids.size() + " worlds to export.zip!").withStyle(ChatFormatting.GREEN), false);
        return 1;
    }

    private static CompoundTag writeLevelDat(CompoundTag dimensionsTag) {
        CompoundTag root = new CompoundTag();
        CompoundTag data = new CompoundTag();

        CompoundTag versionTag = new CompoundTag();
        WorldVersion worldVersion = SharedConstants.getCurrentVersion();
        DataVersion dataVersion = worldVersion./*? if > 1.21.5 {*/ dataVersion() /*?} else {*/ /*getDataVersion()*//*?}*/;

        versionTag.putString("Name", worldVersion./*? if > 1.21.5 {*/ name() /*?} else {*/ /*getName() *//*?}*/);
        versionTag.putInt("Id", dataVersion./*? if > 1.21.5 {*/ version() /*?} else {*/ /*getVersion() *//*?}*/);
        versionTag.putBoolean("Snapshot", !worldVersion./*? if > 1.21.5 {*/ stable() /*?} else {*/ /*isStable() *//*?}*/);
        versionTag.putString("Series", dataVersion./*? if > 1.21.5 {*/ series() /*?} else {*/ /*getSeries() *//*?}*/);

        data.put("Version", versionTag);
        data.putInt("version", net.minecraft.world.level.storage.WorldData.ANVIL_VERSION_ID);

        CompoundTag worldGenSettingsTag = new CompoundTag();
        worldGenSettingsTag.put("dimensions", dimensionsTag);

        data.put("WorldGenSettings", worldGenSettingsTag);
        root.put("Data", data);
        return root;
    }


}
