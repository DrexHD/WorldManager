package me.drex.worldmanager.mixin.portal;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import me.drex.worldmanager.save.PortalBehaviour;
import me.drex.worldmanager.save.WorldConfig;
import me.drex.worldmanager.save.WorldManagerSavedData;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EndPortalBlock;
import net.minecraft.world.level.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EndPortalBlock.class)
public abstract class EndPortalBlockMixin {
    @ModifyArg(
        method = "getPortalDestination",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/MinecraftServer;getLevel(Lnet/minecraft/resources/ResourceKey;)Lnet/minecraft/server/level/ServerLevel;"
        ),
        index = 0
    )
    public ResourceKey<?> addEndPortalBehaviour(ResourceKey<?> original, @Local(argsOnly = true) ServerLevel serverLevel) {
        WorldConfig config = WorldManagerSavedData.getConfig(serverLevel);
        if (config != null) {
            Identifier destination = config.data.portals.get(PortalBehaviour.END_PORTAL_ID);
            return ResourceKey.create(Registries.DIMENSION, destination);
        }
        return original;
    }

    @WrapOperation(
        method = "getPortalDestination",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;dimension()Lnet/minecraft/resources/ResourceKey;"
        )
    )
    public ResourceKey<Level> deHardCodeEndDimension(ServerLevel instance, Operation<ResourceKey> original) {
        DimensionType dimensionType = instance.dimensionType();
        //? if >= 1.21.11 {
        if (dimensionType.skybox() == DimensionType.Skybox.END) {
            return Level.END;
        }
        //? } else {
        /*if (!dimensionType.bedWorks() && !dimensionType.respawnAnchorWorks()) {
            return Level.END;
        }
        *///? }
        return original.call(instance);

    }
}
