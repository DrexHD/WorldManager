package me.drex.worldmanager.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import me.drex.worldmanager.save.Location;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;

public class PlayerData {
    public static final Codec<PlayerData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.unboundedMap(ResourceKey.codec(Registries.DIMENSION), Location.CODEC).fieldOf("locations").forGetter(PlayerData::locations)
    ).apply(instance, instance.stable(PlayerData::new)));

    private final Map<ResourceKey<Level>, Location> locations;

    public PlayerData(Map<ResourceKey<Level>, Location> locations) {
        this.locations = new HashMap<>(locations);
    }

    public Map<ResourceKey<Level>, Location> locations() {
        return locations;
    }
}
