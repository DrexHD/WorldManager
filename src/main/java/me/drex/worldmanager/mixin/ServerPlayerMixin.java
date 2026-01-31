package me.drex.worldmanager.mixin;

import eu.pb4.playerdata.api.PlayerDataApi;
import me.drex.worldmanager.WorldManager;
import me.drex.worldmanager.data.PlayerData;
import me.drex.worldmanager.save.Location;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collections;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {
    @Shadow
    public abstract ServerLevel /*? if >= 1.21.6 {*/ level() /*?} else {*/ /*serverLevel() *//*?}*/;

    @Inject(method = "doTick", at = @At("HEAD"))
    public void saveLocation(CallbackInfo ci) {
        var player = (ServerPlayer) (Object) this;
        PlayerData playerData = PlayerDataApi.getCustomDataFor(player, WorldManager.STORAGE);
        if (playerData == null) {
            playerData = new PlayerData(Collections.emptyMap());
            PlayerDataApi.setCustomDataFor(player, WorldManager.STORAGE, playerData);
        }
        playerData.locations().put(/*? if >= 1.21.6 {*/ level() /*?} else {*/ /*serverLevel() *//*?}*/.dimension(), new Location(player));
    }
}
