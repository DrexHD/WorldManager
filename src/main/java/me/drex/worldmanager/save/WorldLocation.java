package me.drex.worldmanager.save;

import net.minecraft.core.BlockPos;
//? if <= 1.21.8 {
/*import net.minecraft.server.level.PlayerRespawnLogic;
*///?}
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

import java.util.Collections;

public record WorldLocation(ServerLevel level, Location location) {
    public static WorldLocation findSpawn(ServerLevel level, Entity entity) {
        //? if >= 1.21 {
        Vec3 vec3 = entity.adjustSpawnLocation(level, level./*? if >= 1.21.9 {*/getRespawnData().pos() /*?} else {*/ /*getSharedSpawnPos() *//*?}*/).getBottomCenter();
        return new WorldLocation(level, new Location(vec3, Vec2.ZERO));
        //?} else {
        /*BlockPos spawnPos = level.getSharedSpawnPos();
        BlockPos pos = PlayerRespawnLogic.getSpawnPosInChunk(level, new ChunkPos(spawnPos));
        if (pos == null) pos = spawnPos;
        return new WorldLocation(level, new Location(pos.getCenter(), Vec2.ZERO));
        *///?}
    }

    public void teleport(Entity entity) {
        Vec3 position = location.position();
        Vec2 rotation = location.rotation();
        entity.teleportTo(level, position.x, position.y, position.z, Collections.emptySet(), rotation.y, rotation.x/*? if >= 1.21.2 {*/, true /*?}*/);
    }
}
