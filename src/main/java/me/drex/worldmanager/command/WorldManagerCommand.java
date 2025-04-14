package me.drex.worldmanager.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import me.drex.message.api.LocalizedMessage;
import me.drex.worldmanager.gui.create.ConfigureWorld;
import me.drex.worldmanager.gui.list.WorldList;
import me.drex.worldmanager.save.WorldConfig;
import me.drex.worldmanager.save.WorldManagerSavedData;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.CompoundTagArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.DimensionTransition;
import xyz.nucleoid.fantasy.Fantasy;
import xyz.nucleoid.fantasy.RuntimeWorldHandle;

import static me.drex.message.api.LocalizedMessage.builder;
import static me.drex.message.api.LocalizedMessage.localized;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class WorldManagerCommand {

    public static final SuggestionProvider<CommandSourceStack> WORLD_SUGGESTIONS = (context, builder) ->
        SharedSuggestionProvider.suggestResource(
            context.getSource().getServer().levelKeys().stream().map(ResourceKey::location), builder
        );

    public static final SuggestionProvider<CommandSourceStack> CUSTOM_WORLD_SUGGESTIONS = (context, builder) ->
        SharedSuggestionProvider.suggestResource(
            WorldManagerSavedData.getSavedData(context.getSource().getServer()).getWorlds().keySet(), builder
        );

    public static final SimpleCommandExceptionType UNKNOWN_WORLD = new SimpleCommandExceptionType(localized("worldmanager.command.exception.unknown_world"));

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var root = dispatcher.register(
            literal("worldmanager")
                .requires(Permissions.require("worldmanager.command.worldmanager", 2))
                .executes(context -> {
                    new WorldList(context.getSource().getPlayerOrException()).open();
                    return 1;
                })
                .then(
                    literal("delete")
                        .requires(Permissions.require("worldmanager.command.worldmanager.delete", 2))
                        .then(
                            argument("id", ResourceLocationArgument.id())
                                .suggests(CUSTOM_WORLD_SUGGESTIONS)
                                .executes(context -> delete(context.getSource(), ResourceLocationArgument.getId(context, "id")))
                        )
                )
                .then(
                    literal("tp")
                        .requires(Permissions.require("worldmanager.command.worldmanager.teleport", 2))
                        .then(
                            argument("id", ResourceLocationArgument.id())
                                .suggests(WORLD_SUGGESTIONS)
                                .executes(context -> teleport(context.getSource(), ResourceLocationArgument.getId(context, "id")))
                        )

                ).then(
                    literal("create")
                        .requires(Permissions.require("worldmanager.command.worldmanager.create", 2))
                        .then(
                            argument("id", ResourceLocationArgument.id())
                                .executes(context -> {
                                    ResourceLocation id = ResourceLocationArgument.getId(context, "id");
                                    ResourceKey<Level> resourceKey = ResourceKey.create(Registries.DIMENSION, id);
                                    MinecraftServer server = context.getSource().getServer();
                                    ServerLevel level = server.getLevel(resourceKey);
                                    if (level != null) {
                                        throw new SimpleCommandExceptionType(localized("worldmanager.command.exception.world_already_exists")).create();
                                    }

                                    new ConfigureWorld(context.getSource().getPlayerOrException(), id).open();
                                    return 1;
                                }).then(
                                    argument("nbt", CompoundTagArgument.compoundTag())
                                        .executes(context -> {
                                            ResourceLocation id = ResourceLocationArgument.getId(context, "id");
                                            ResourceKey<Level> resourceKey = ResourceKey.create(Registries.DIMENSION, id);
                                            MinecraftServer server = context.getSource().getServer();
                                            ServerLevel level = server.getLevel(resourceKey);
                                            if (level != null) {
                                                throw new SimpleCommandExceptionType(localized("worldmanager.command.exception.world_already_exists")).create();
                                            }

                                            CompoundTag tag = CompoundTagArgument.getCompoundTag(context, "nbt");
                                            var config = WorldConfig.CODEC.decode(RegistryOps.create(NbtOps.INSTANCE, server.registryAccess()), tag)
                                                .getOrThrow(s -> new SimpleCommandExceptionType(Component.literal(s)).create())
                                                .getFirst();

                                            Fantasy fantasy = Fantasy.get(server);

                                            RuntimeWorldHandle handle = fantasy.getOrOpenPersistentWorld(id, config.toRuntimeWorldConfig());

                                            WorldManagerSavedData savedData = WorldManagerSavedData.getSavedData(server);
                                            savedData.addWorld(id, config, handle);
                                            context.getSource().sendSuccess(() -> builder("worldmanager.command.create").addPlaceholder("id", id.toString()).build(), false);
                                            return 1;
                                        })
                                )
                        )
                )

        );

        dispatcher.register(
            literal("wm")
                .requires(Permissions.require("worldmanager.command.worldmanager", 2))
                .executes(context -> {
                    new WorldList(context.getSource().getPlayerOrException()).open();
                    return 1;
                })
                .redirect(root)
        );
    }

    public static int teleport(CommandSourceStack source, ResourceLocation id) throws CommandSyntaxException {
        var player = source.getPlayerOrException();
        ResourceKey<Level> resourceKey = ResourceKey.create(Registries.DIMENSION, id);
        ServerLevel serverLevel = source.getServer().getLevel(resourceKey);
        if (serverLevel == null) {
            throw UNKNOWN_WORLD.create();
        }

        DimensionTransition teleportTransition = DimensionTransition.missingRespawnBlock(serverLevel, player, DimensionTransition.DO_NOTHING);
        var pos = teleportTransition.pos();
        player.teleportTo(teleportTransition.newLevel(), pos.x, pos.y, pos.z, teleportTransition.yRot(), teleportTransition.xRot());
        source.sendSuccess(() -> builder("worldmanager.command.teleport").addPlaceholder("id", id.toString()).build(), false);
        return 1;
    }

    public static int delete(CommandSourceStack source, ResourceLocation id) throws CommandSyntaxException {
        WorldManagerSavedData savedData = WorldManagerSavedData.getSavedData(source.getServer());
        boolean success = savedData.removeWorld(id);

        if (!success) {
            throw UNKNOWN_WORLD.create();
        }
        source.sendSuccess(() -> LocalizedMessage.builder("worldmanager.command.delete").addPlaceholder("id", id.toString()).build(), false);
        return 1;
    }

}
