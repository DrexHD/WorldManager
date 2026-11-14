package me.drex.worldmanager.save;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.clock.PackedClockStates;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class WorldData {

    public Optional<Location> spawnLocation = Optional.empty();
    public ItemStack icon = ItemStack.EMPTY;
    public Map<Identifier, Identifier> portals = new HashMap<>();
    public PackedClockStates clocks = PackedClockStates.EMPTY;

    public static final Codec<WorldData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Location.CODEC.optionalFieldOf("spawn_location").forGetter(worldData -> worldData.spawnLocation),
        ItemStack.CODEC.optionalFieldOf("icon", ItemStack.EMPTY).forGetter(worldData -> worldData.icon),
        Codec.unboundedMap(Identifier.CODEC, Identifier.CODEC).optionalFieldOf("portals", Collections.emptyMap()).forGetter(worldData -> worldData.portals),
        PackedClockStates.CODEC.optionalFieldOf("clocks", PackedClockStates.EMPTY).forGetter(worldData -> worldData.clocks)
    ).apply(instance, instance.stable((location, icon, portals, clocks) -> {
        WorldData data = new WorldData();
        data.spawnLocation = location;
        data.icon = icon;
        data.portals = new HashMap<>(portals);
        data.clocks = clocks;
        return data;
    })));

    public GuiElementBuilder iconGuiElement() {
        if (icon.isEmpty()) {
            return new GuiElementBuilder(Items.PLAYER_HEAD)
                .setProfileSkinTexture("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYmFkYzA0OGE3Y2U3OGY3ZGFkNzJhMDdkYTI3ZDg1YzA5MTY4ODFlNTUyMmVlZWQxZTNkYWYyMTdhMzhjMWEifX19");
        } else {
            return new GuiElementBuilder(icon);
        }
    }
}
