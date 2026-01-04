package me.drex.worldmanager.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import me.drex.worldmanager.save.PortalBehaviour;
import me.drex.worldmanager.save.WorldConfig;
import me.drex.worldmanager.save.WorldManagerSavedData;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;

import static me.drex.worldmanager.command.WorldManagerCommand.WORLD_SUGGESTIONS;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class SetPortalCommand {
    public static final SuggestionProvider<CommandSourceStack> PORTAL_SUGGESTIONS = (context, builder) ->
        SharedSuggestionProvider.suggestResource(PortalBehaviour.getPortals(), builder);
    public static final SimpleCommandExceptionType UNKNOWN_PORTAL = new SimpleCommandExceptionType(Component.literal("Unknown portal id!"));

    public static LiteralArgumentBuilder<CommandSourceStack> build() {
        return literal("setportal")
            .requires(Permissions.require("worldmanager.command.worldmanager.setportal", 2))
            .then(
                argument("portal", IdentifierArgument.id())
                    .suggests(PORTAL_SUGGESTIONS)
                    .executes(context -> setPortal(context.getSource(), IdentifierArgument.getId(context, "portal"), null))
                    .then(
                        argument("destination", IdentifierArgument.id())
                            .suggests(WORLD_SUGGESTIONS)
                            .executes(context -> setPortal(context.getSource(), IdentifierArgument.getId(context, "portal"), IdentifierArgument.getId(context, "destination")))
                    )
            );
    }

    public static int setPortal(CommandSourceStack source, Identifier portal, Identifier destination) throws CommandSyntaxException {
        if (!PortalBehaviour.getPortals().contains(portal)) {
            throw UNKNOWN_PORTAL.create();
        }
        ServerLevel level = source.getLevel();
        WorldConfig config = WorldManagerSavedData.getConfig(level);
        if (config == null) {
            throw WorldManagerCommand.NOT_SUPPORTED.create();
        }
        config.data.portals.put(portal, destination);
        if (destination != null) {
            source.sendSuccess(() -> Component.literal("Set the " + portal + " destination to " + destination), false);
        } else {
            source.sendSuccess(() -> Component.literal("Reset the " + portal + " destination"), false);
        }
        return 1;
    }

}
