package me.drex.worldmanager.mixin.portal;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import me.drex.worldmanager.save.PortalBehaviour;
import me.drex.worldmanager.save.WorldConfig;
import me.drex.worldmanager.save.WorldManagerSavedData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(BaseFireBlock.class)
public abstract class BaseFireBlockMixin {
    @ModifyReturnValue(method = "inPortalDimension", at = @At("RETURN"))
    private static boolean addNetherPortalBehaviour(boolean original, @Local(argsOnly = true) Level level) {
        if (level instanceof ServerLevel serverLevel) {
            WorldConfig config = WorldManagerSavedData.getConfig(serverLevel);
            if (config != null && config.data.portals.containsKey(PortalBehaviour.NETHER_PORTAL_ID)) {
                return true;
            }
        }
        return original;
    }
}
