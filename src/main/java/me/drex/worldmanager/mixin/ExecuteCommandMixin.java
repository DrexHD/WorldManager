package me.drex.worldmanager.mixin;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import me.drex.worldmanager.command.WorldManagerCommand;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.server.commands.ExecuteCommand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ExecuteCommand.class)
public abstract class ExecuteCommandMixin {

    @Definition(id = "argument", method = "Lnet/minecraft/commands/Commands;argument(Ljava/lang/String;Lcom/mojang/brigadier/arguments/ArgumentType;)Lcom/mojang/brigadier/builder/RequiredArgumentBuilder;")
    @Definition(id = "dimension", method = "Lnet/minecraft/commands/arguments/DimensionArgument;dimension()Lnet/minecraft/commands/arguments/DimensionArgument;")
    @Expression("argument(?, dimension())")
    @WrapOperation(
        method = "register",
        at = @At("MIXINEXTRAS:EXPRESSION")
    )
    private static <T> RequiredArgumentBuilder<CommandSourceStack, T> useServerLevels(String string, ArgumentType<T> argumentType, Operation<RequiredArgumentBuilder<CommandSourceStack, T>> original) {
        return original.call(string, IdentifierArgument.id()).suggests(WorldManagerCommand.WORLD_SUGGESTIONS);
    }

}
