package me.drex.worldmanager.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.drex.worldmanager.save.WorldConfig;
import me.drex.worldmanager.save.WorldManagerSavedData;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;

import static me.drex.worldmanager.command.WorldManagerCommand.UNKNOWN_WORLD;
import static me.drex.worldmanager.command.WorldManagerCommand.WORLD_SUGGESTIONS;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class SetIconCommand {
    public static LiteralArgumentBuilder<CommandSourceStack> build(CommandBuildContext commandBuildContext) {
        return literal("seticon")
            .requires(Permissions.require("worldmanager.command.worldmanager.seticon", 2))
            .then(
                argument("id", IdentifierArgument.id())
                    .suggests(WORLD_SUGGESTIONS)
                    .then(
                        Commands.argument("icon", ItemArgument.item(commandBuildContext))
                            .executes(context -> setIcon(context.getSource(), IdentifierArgument.getId(context, "id"), ItemArgument.getItem(context, "icon").createItemStack(1, false)))
                    ).executes(context -> setIcon(context.getSource(), IdentifierArgument.getId(context, "id"), context.getSource().getPlayerOrException().getMainHandItem()))
            );
    }

    public static int setIcon(CommandSourceStack source, Identifier id, ItemStack icon) throws CommandSyntaxException {
        MinecraftServer server = source.getServer();
        WorldManagerSavedData savedData = WorldManagerSavedData.getSavedData(server);
        WorldConfig config = savedData.getConfig(id);

        if (config == null) {
            throw UNKNOWN_WORLD.create();
        }
        config.data.icon = icon;
        savedData.setDirty();

        source.sendSuccess(() -> Component.literal("Set world icon for " + id + " to ").append(icon.getDisplayName()), false);
        return 1;
    }
}
