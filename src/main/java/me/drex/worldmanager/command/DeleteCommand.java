package me.drex.worldmanager.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.drex.worldmanager.save.WorldManagerSavedData;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import static me.drex.worldmanager.command.WorldManagerCommand.CUSTOM_WORLD_SUGGESTIONS;
import static me.drex.worldmanager.command.WorldManagerCommand.UNKNOWN_WORLD;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class DeleteCommand {
    public static LiteralArgumentBuilder<CommandSourceStack> build() {
        return literal("delete")
            .requires(Permissions.require("worldmanager.command.worldmanager.delete", 2))
            .then(
                argument("id", IdentifierArgument.id())
                    .suggests(CUSTOM_WORLD_SUGGESTIONS)
                    .executes(context -> delete(context.getSource(), IdentifierArgument.getId(context, "id")))
            );
    }

    public static int delete(CommandSourceStack source, Identifier id) throws CommandSyntaxException {
        WorldManagerSavedData savedData = WorldManagerSavedData.getSavedData(source.getServer());
        boolean success = savedData.removeWorld(id);

        if (!success) {
            throw UNKNOWN_WORLD.create();
        }
        source.sendSuccess(() -> Component.literal("World " + id + " has been deleted successfully!"), false);
        return 1;
    }
}
