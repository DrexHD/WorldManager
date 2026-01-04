package me.drex.worldmanager.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.drex.worldmanager.gui.list.ManageWorld;
import me.drex.worldmanager.save.WorldConfig;
import me.drex.worldmanager.save.WorldManagerSavedData;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

import static me.drex.worldmanager.command.WorldManagerCommand.*;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class ManageCommand {
    public static LiteralArgumentBuilder<CommandSourceStack> build() {
        return literal("manage")
            .requires(Permissions.require("worldmanager.command.worldmanager.manage", 2))
            .executes(context -> manage(context.getSource().getPlayerOrException(), context.getSource().getLevel().dimension().identifier()))
            .then(
                argument("id", IdentifierArgument.id())
                    .suggests(CUSTOM_WORLD_SUGGESTIONS)
                    .executes(context -> manage(context.getSource().getPlayerOrException(), IdentifierArgument.getId(context, "id")))
            );
    }

    public static int manage(ServerPlayer player, Identifier id) throws CommandSyntaxException {
        WorldManagerSavedData savedData = WorldManagerSavedData.getSavedData(player.level().getServer());
        WorldConfig config = savedData.getConfig(id);
        if (config == null) {
            throw UNKNOWN_WORLD.create();
        }

        new ManageWorld(player, id, config, null).open();
        return 1;
    }

}
