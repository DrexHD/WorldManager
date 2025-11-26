package me.drex.worldmanager.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import me.drex.worldmanager.save.WorldManagerSavedData;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.resources.ResourceKey;

import static me.drex.message.api.LocalizedMessage.localized;
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

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext commandBuildContext) {
        var root = dispatcher.register(
            literal("worldmanager")
                .requires(Permissions.require("worldmanager.command.worldmanager", 2))
                .then(DeleteCommand.build())
                .then(SpawnCommand.build())
                .then(SetIconCommand.build(commandBuildContext))
                .then(SetSpawnCommand.build())
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
