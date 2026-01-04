package me.drex.worldmanager.save;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Util;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record Location(Vec3 position, Vec2 rotation) {

    //? if < 1.21.5 {
    /*public static final Codec<Vec2> VEC_2_CODEC = Codec.FLOAT
                .listOf()
                .comapFlatMap(list -> Util.fixedSize(list, 2).map(listx -> new Vec2(listx.getFirst(), listx.get(1))), vec2 -> List.of(vec2.x, vec2.y));
    *///?}

    public static final Codec<Location> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Vec3.CODEC.fieldOf("position").forGetter(Location::position),
        //? if >= 1.21.5 {
        Vec2.CODEC.fieldOf("rotation").forGetter(Location::rotation)
        //?} else {
        /*VEC_2_CODEC.fieldOf("rotation").forGetter(Location::rotation)
         *///?}
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

    public Map<String, Component> placeholders() {
        return new HashMap<>() {{
            put("position_x", Component.literal(String.format("%.2f", position.x)));
            put("position_y", Component.literal(String.format("%.2f", position.y)));
            put("position_z", Component.literal(String.format("%.2f", position.z)));
            put("rotation_x", Component.literal(String.format("%.2f", rotation.x)));
            put("rotation_y", Component.literal(String.format("%.2f", rotation.y)));
        }};
    }
}
