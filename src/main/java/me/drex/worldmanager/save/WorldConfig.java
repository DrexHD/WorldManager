package me.drex.worldmanager.save;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import me.drex.worldmanager.util.CodecUtil;
import net.minecraft.core.Holder;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.DimensionType;
import xyz.nucleoid.fantasy.RuntimeWorldConfig;

// TODO Save world time
public final class WorldConfig {

    public static final Codec<WorldConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        DimensionType.CODEC.fieldOf("type").forGetter(wc -> wc.type),
        ChunkGenerator.CODEC.fieldOf("generator").forGetter(wc -> wc.generator),
        Codec.LONG.optionalFieldOf("seed", 0L).forGetter(wc -> wc.seed),
        Codec.BOOL.optionalFieldOf("tick_time", true).forGetter(wc -> wc.tickTime),
        CodecUtil.optionalFieldOf(WorldData.CODEC, "data", WorldData::new, false).forGetter(wc -> wc.data)
    ).apply(instance, instance.stable(WorldConfig::new)));
    public final Holder<DimensionType> type;
    public final ChunkGenerator generator;
    public long seed;
    public final boolean tickTime;
    public WorldData data;

    public WorldConfig(Holder<DimensionType> type, ChunkGenerator generator, long seed, boolean tickTime, WorldData data) {
        this.type = type;
        this.generator = generator;
        this.seed = seed;
        this.tickTime = tickTime;
        this.data = data;
    }

    public RuntimeWorldConfig toRuntimeWorldConfig() {
        return new RuntimeWorldConfig()
            .setGenerator(generator)
            .setDimensionType(type)
            .setSeed(seed)
            .setMirrorOverworldGameRules(true)
            .setMirrorOverworldDifficulty(true)
            .setShouldTickTime(tickTime);
    }
}
