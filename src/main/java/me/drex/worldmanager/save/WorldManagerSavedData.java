package me.drex.worldmanager.save;

import com.mojang.serialization.Codec;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
//? if >= 1.21.5 {
import net.minecraft.world.level.saveddata.SavedDataType;
//?}
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.fantasy.Fantasy;
import xyz.nucleoid.fantasy.RuntimeWorldHandle;

import java.util.HashMap;
import java.util.Map;

public class WorldManagerSavedData extends SavedData {

    private final Map<Identifier, WorldConfig> worlds;
    private final Map<Identifier, RuntimeWorldHandle> worldHandles = new HashMap<>();
    private static final Codec<WorldManagerSavedData> CODEC = Codec.unboundedMap(Identifier.CODEC, WorldConfig.CODEC)
        .xmap(WorldManagerSavedData::new, worldManagerSavedData -> worldManagerSavedData.worlds);

    //? if >= 1.21.5 {
    public static final SavedDataType<WorldManagerSavedData> TYPE = new SavedDataType<>("worldmanager", WorldManagerSavedData::new, CODEC, null);
    //?}
    
    private WorldManagerSavedData() {
        this.worlds = new HashMap<>();
    }

    private WorldManagerSavedData(Map<Identifier, WorldConfig> worlds) {
        this.worlds = new HashMap<>(worlds);
    }

    //? if < 1.21.5 {
    /*@Override
    public @NotNull CompoundTag save(CompoundTag compoundTag, HolderLookup.Provider provider) {
        return (CompoundTag) CODEC.encode(this, RegistryOps.create(NbtOps.INSTANCE, provider), compoundTag).getOrThrow();
    }

    public static SavedData.Factory<WorldManagerSavedData> factory() {
        return new SavedData.Factory<>(WorldManagerSavedData::new, WorldManagerSavedData::load, null);
    }

    public static WorldManagerSavedData load(CompoundTag compoundTag, HolderLookup.Provider provider) {
        return CODEC.decode(RegistryOps.create(NbtOps.INSTANCE, provider), compoundTag).getOrThrow().getFirst();
    }
    *///?}

    public static WorldManagerSavedData getSavedData(MinecraftServer server) {
        //? if >= 1.21.5 {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
        //?} else {
        /*return server.overworld().getDataStorage().computeIfAbsent(WorldManagerSavedData.factory(), "worldmanager");
        *///?}
    }

    @Nullable
    public static WorldConfig getConfig(ServerLevel level) {
        WorldManagerSavedData savedData = getSavedData(level.getServer());
        return savedData.getConfig(level.dimension().identifier());
    }

    public void loadWorlds(MinecraftServer server) {
        this.worlds.forEach((id, worldConfig) -> {
            RuntimeWorldHandle handle = Fantasy.get(server).getOrOpenPersistentWorld(id, worldConfig.toRuntimeWorldConfig());
            worldHandles.put(id, handle);
        });
    }

    public void addWorld(Identifier id, WorldConfig config, RuntimeWorldHandle handle) {
        worlds.put(id, config);
        worldHandles.put(id, handle);
        setDirty();
    }

    public boolean removeWorld(Identifier id) {
        WorldConfig config = worlds.remove(id);
        if (config != null) {
            RuntimeWorldHandle handle = worldHandles.remove(id);
            handle.delete();
            setDirty();
            return true;
        }
        return false;
    }

    @Nullable
    public WorldConfig getConfig(Identifier id) {
        return worlds.get(id);
    }

    public Map<Identifier, WorldConfig> getWorlds() {
        return new HashMap<>(worlds);
    }

}
