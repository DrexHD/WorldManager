package me.drex.worldmanager.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.drex.worldmanager.save.Location;
import me.drex.worldmanager.save.WorldConfig;
import me.drex.worldmanager.save.WorldManagerSavedData;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

import static me.drex.worldmanager.command.WorldManagerCommand.UNKNOWN_WORLD;
import static net.minecraft.commands.Commands.literal;

public class SetSpawnCommand {
    public static LiteralArgumentBuilder<CommandSourceStack> build() {
        return literal("setspawn")
            .requires(Permissions.require("worldmanager.command.worldmanager.setspawn", 2))
            .executes(context -> setSpawn(context.getSource()));
    }

    public static int setSpawn(CommandSourceStack source) throws CommandSyntaxException {
        MinecraftServer server = source.getServer();
        WorldManagerSavedData savedData = WorldManagerSavedData.getSavedData(server);
        ResourceLocation id = source.getLevel().dimension().location();
        WorldConfig config = savedData.getConfig(id);

        if (config == null) {
            throw UNKNOWN_WORLD.create();
        }
        var location = new Location(source);
        config.data.spawnLocation = Optional.of(location);
        savedData.setDirty();

        Vec3 position = location.position();
        source.sendSuccess(() -> Component.literal("Set world spawn point in " + id + " to " + position.x + " " + position.y + " " + position.z), false);
        return 1;
    }
}
