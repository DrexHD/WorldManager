package me.drex.worldmanager.save;

import com.mojang.serialization.Codec;
import me.drex.worldmanager.WorldManager;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.fantasy.Fantasy;
import xyz.nucleoid.fantasy.RuntimeLevelHandle;

import java.util.HashMap;
import java.util.Map;

public class WorldManagerSavedData extends SavedData {

    private final Map<Identifier, WorldConfig> worlds;
    private final Map<Identifier, RuntimeLevelHandle> worldHandles = new HashMap<>();
    private static final Codec<WorldManagerSavedData> CODEC = Codec.unboundedMap(Identifier.CODEC, WorldConfig.CODEC)
        .xmap(WorldManagerSavedData::new, worldManagerSavedData -> worldManagerSavedData.worlds);

    public static final SavedDataType<WorldManagerSavedData> TYPE = new SavedDataType<>(WorldManager.id("worlds"), WorldManagerSavedData::new, CODEC, null);

    private WorldManagerSavedData() {
        this.worlds = new HashMap<>();
    }

    private WorldManagerSavedData(Map<Identifier, WorldConfig> worlds) {
        this.worlds = new HashMap<>(worlds);
    }

    public static WorldManagerSavedData getSavedData(MinecraftServer server) {
        return server.getDataStorage().computeIfAbsent(TYPE);
    }

    @Nullable
    public static WorldConfig getConfig(ServerLevel level) {
        WorldManagerSavedData savedData = getSavedData(level.getServer());
        return savedData.getConfig(level.dimension().identifier());
    }

    public void loadWorlds(MinecraftServer server) {
        this.worlds.forEach((id, worldConfig) -> {
            RuntimeLevelHandle handle = Fantasy.get(server).getOrOpenPersistentLevel(id, worldConfig.toRuntimeLevelConfig());
            worldHandles.put(id, handle);
        });
    }

    public void addWorld(Identifier id, WorldConfig config, RuntimeLevelHandle handle) {
        worlds.put(id, config);
        worldHandles.put(id, handle);
        setDirty();
    }

    public boolean removeWorld(Identifier id) {
        WorldConfig config = worlds.remove(id);
        if (config != null) {
            RuntimeLevelHandle handle = worldHandles.remove(id);
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
