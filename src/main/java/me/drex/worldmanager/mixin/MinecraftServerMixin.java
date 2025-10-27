package me.drex.worldmanager.mixin;

import me.drex.worldmanager.save.WorldManagerSavedData;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin {
    @Inject(method = "createLevels", at = @At(value = "TAIL"))
    public void loadCustomLevels(CallbackInfo ci) {
        var this_ = (MinecraftServer) (Object) this;
        WorldManagerSavedData savedData = WorldManagerSavedData.getSavedData(this_);
        savedData.loadWorlds(this_);
    }
}
