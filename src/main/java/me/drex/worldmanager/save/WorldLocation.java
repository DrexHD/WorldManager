package me.drex.worldmanager.save;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

import java.util.Collections;

public record WorldLocation(ServerLevel level, Location location) {
    public static WorldLocation findSpawn(ServerLevel level, Entity entity) {
        //? if >= 26.2 {
        Vec3 vec3 = Vec3.atBottomCenterOf(entity.adjustSpawnLocation(level, level.getRespawnData().pos()));
        //? } else {
        //Vec3 vec3 = entity.adjustSpawnLocation(level, level.getRespawnData().pos()).getBottomCenter();
        //? }
        return new WorldLocation(level, new Location(vec3, Vec2.ZERO));
    }

    public void teleport(Entity entity) {
        Vec3 position = location.position();
        Vec2 rotation = location.rotation();
        entity.teleportTo(level, position.x, position.y, position.z, Collections.emptySet(), rotation.y, rotation.x, true);
    }
}
