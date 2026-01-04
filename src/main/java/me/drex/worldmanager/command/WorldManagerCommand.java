package me.drex.worldmanager.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import me.drex.worldmanager.save.WorldManagerSavedData;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;

import static net.minecraft.commands.Commands.literal;

public class WorldManagerCommand {

    public static final SuggestionProvider<CommandSourceStack> WORLD_SUGGESTIONS = (context, builder) ->
        SharedSuggestionProvider.suggestResource(
            context.getSource().getServer().levelKeys().stream().map(resourceKey -> resourceKey.identifier()), builder
        );

    public static final SuggestionProvider<CommandSourceStack> CUSTOM_WORLD_SUGGESTIONS = (context, builder) ->
        SharedSuggestionProvider.suggestResource(
            WorldManagerSavedData.getSavedData(context.getSource().getServer()).getWorlds().keySet(), builder
        );

    public static final SimpleCommandExceptionType UNKNOWN_WORLD = new SimpleCommandExceptionType(Component.literal("Unknown world id!"));
    public static final SimpleCommandExceptionType ALREADY_EXISTS = new SimpleCommandExceptionType(Component.literal("A world with that id already exists!"));
    public static final SimpleCommandExceptionType NOT_SUPPORTED = new SimpleCommandExceptionType(Component.literal("This world doesn't support this command!"));

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext commandBuildContext) {
        var root = dispatcher.register(
            literal("worldmanager")
                .requires(Permissions.require("worldmanager.command.worldmanager", 2))
                .then(DeleteCommand.build())
                .then(SpawnCommand.build())
                .then(SetIconCommand.build(commandBuildContext))
                .then(SetSpawnCommand.build())
                .then(SetPortalCommand.build())
                .then(TeleportCommand.build())
                .then(CreateCommand.build())
                .then(ImportCommand.build())
                .then(ExportCommand.build())
                .then(ManageCommand.build())
                .then(ListCommand.build())
        );

        dispatcher.register(
            literal("wm")
                .requires(Permissions.require("worldmanager.command.worldmanager", 2))
                .redirect(root)
        );
    }
}
