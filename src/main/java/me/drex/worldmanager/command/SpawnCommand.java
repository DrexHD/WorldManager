package me.drex.worldmanager.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.drex.worldmanager.save.Location;
import me.drex.worldmanager.save.WorldConfig;
import me.drex.worldmanager.save.WorldLocation;
import me.drex.worldmanager.save.WorldManagerSavedData;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static me.drex.worldmanager.command.WorldManagerCommand.UNKNOWN_WORLD;
import static me.drex.worldmanager.command.WorldManagerCommand.WORLD_SUGGESTIONS;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class SpawnCommand {
    public static LiteralArgumentBuilder<CommandSourceStack> build() {
        return literal("spawn")
            .requires(Permissions.require("worldmanager.command.worldmanager.spawn", 2))
            .then(
                argument("id", ResourceLocationArgument.id())
                    .suggests(WORLD_SUGGESTIONS)
                    .executes(context -> spawn(context.getSource(), ResourceLocationArgument.getId(context, "id"), List.of(context.getSource().getPlayerOrException())))
                    .then(
                        argument("targets", EntityArgument.players())
                            .executes(context -> spawn(context.getSource(), ResourceLocationArgument.getId(context, "id"), EntityArgument.getPlayers(context, "targets")))
                    )
            );
    }

    public static int spawn(CommandSourceStack source, ResourceLocation id, Collection<ServerPlayer> targets) throws CommandSyntaxException {
        ResourceKey<Level> resourceKey = ResourceKey.create(Registries.DIMENSION, id);
        MinecraftServer server = source.getServer();
        WorldManagerSavedData savedData = WorldManagerSavedData.getSavedData(server);
        WorldConfig config = savedData.getConfig(id);
        ServerLevel serverLevel = source.getServer().getLevel(resourceKey);
        if (serverLevel == null) {
            throw UNKNOWN_WORLD.create();
        }
        for (ServerPlayer player : targets) {
            spawn(player, config, id);
        }
        source.sendSuccess(() -> Component.literal("Teleporting to world spawn of " + id), false);
        return 1;
    }

    public static boolean spawn(ServerPlayer player, WorldConfig config, ResourceLocation id) {
        ResourceKey<Level> resourceKey = ResourceKey.create(Registries.DIMENSION, id);
        ServerLevel serverLevel = player.level().getServer().getLevel(resourceKey);
        if (serverLevel == null) return false;

        var spawnLocation = Optional.<Location>empty();
        if (config != null) {
            spawnLocation = config.data.spawnLocation;
        }
        WorldLocation worldLocation = spawnLocation
            .map(location -> location.toWorldLocation(serverLevel))
            .orElseGet(() -> WorldLocation.findSpawn(serverLevel, player));
        worldLocation.teleport(player);
        return true;
    }
}
