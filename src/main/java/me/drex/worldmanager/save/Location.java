package me.drex.worldmanager.save;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public record Location(Vec3 position, Vec2 rotation) {

    public static final Codec<Location> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Vec3.CODEC.fieldOf("position").forGetter(Location::position),
        Vec2.CODEC.fieldOf("rotation").forGetter(Location::rotation)
    ).apply(instance, instance.stable(Location::new)));

    public Location(Entity entity) {
        this(entity.position(), entity.getRotationVector());
    }

    public Location(CommandSourceStack source) {
        this(source.getPosition(), source.getRotation());
    }

    public WorldLocation toWorldLocation(ServerLevel level) {
        return new WorldLocation(level, this);
    }

}
