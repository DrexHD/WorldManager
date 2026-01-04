package me.drex.worldmanager.mixin.portal;

import com.llamalad7.mixinextras.sugar.Local;
import me.drex.worldmanager.save.PortalBehaviour;
import me.drex.worldmanager.save.WorldConfig;
import me.drex.worldmanager.save.WorldManagerSavedData;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.NetherPortalBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(NetherPortalBlock.class)
public abstract class NetherPortalBlockMixin {
    @ModifyArg(
        method = "getPortalDestination",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/MinecraftServer;getLevel(Lnet/minecraft/resources/ResourceKey;)Lnet/minecraft/server/level/ServerLevel;"
        ),
        index = 0
    )
    public ResourceKey<?> addNetherPortalBehaviour(ResourceKey<?> original, @Local(argsOnly = true) ServerLevel serverLevel) {
        WorldConfig config = WorldManagerSavedData.getConfig(serverLevel);
        if (config != null) {
            Identifier destination = config.data.portals.get(PortalBehaviour.NETHER_PORTAL_ID);
            return ResourceKey.create(Registries.DIMENSION, destination);
        }
        return original;
    }

    @ModifyArg(
        method = "getPortalDestination",
        at = @At(
            value = "INVOKE",
            //? if >= 1.21.2 {
            target = "Lnet/minecraft/world/level/block/NetherPortalBlock;getExitPortal(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/BlockPos;ZLnet/minecraft/world/level/border/WorldBorder;)Lnet/minecraft/world/level/portal/TeleportTransition;"
            //? } else {
            /*target = "Lnet/minecraft/world/level/block/NetherPortalBlock;getExitPortal(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/BlockPos;ZLnet/minecraft/world/level/border/WorldBorder;)Lnet/minecraft/world/level/portal/DimensionTransition;"
            *///? }
        ),
        index = 4
    )
    public boolean adjustSearchRange(boolean original, @Local(argsOnly = true) ServerLevel serverLevel) {
        return serverLevel.dimensionType().coordinateScale() >= 8;
    }
}
