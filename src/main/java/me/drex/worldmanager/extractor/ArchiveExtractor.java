package me.drex.worldmanager.extractor;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.drex.worldmanager.save.WorldConfig;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.Map;

import static me.drex.worldmanager.command.ImportCommand.*;

public abstract class ArchiveExtractor {
    Path root = Path.of(".");
    private Map<Identifier, WorldConfig> worldConfigs = Collections.emptyMap();

    // When world_gen_settings.dat is present, it has priority over level.dat
    private boolean parsedWorldGenSettings = false;

    public abstract boolean supports(Path path);

    // TODO support reading world configs from datapacks
    public abstract void open(Path archive, MinecraftServer server) throws IOException, CommandSyntaxException;

    public void readFile(Path path, IOSupplier<InputStream> inputStreamSupplier, MinecraftServer server) throws IOException {
        String fileName = path.getFileName().toString();
        switch (fileName) {
            case "level.dat" -> {
                Path parent = path.getParent();
                if (parent != null) root = parent;
                if (!parsedWorldGenSettings) {
                    worldConfigs = parseLevelDat(inputStreamSupplier.get(), server);
                }
            }
            case "world_gen_settings.dat" -> {
                worldConfigs = parseWorldGenSettings(inputStreamSupplier.get(), server);
                parsedWorldGenSettings = true;
            }
        }
    }

    public abstract void extract(Path relativeSourcePath, Path targetRootPath, MinecraftServer server) throws IOException;

    public abstract void close() throws IOException;

    public Map<Identifier, WorldConfig> worldConfigs() {
        return worldConfigs;
    }

    static void extractFile(Path relativeSourcePath, Path targetRootPath, Path relativeToRoot, boolean directory, InputStream inputStream) throws IOException {
        if (!relativeToRoot.toString().startsWith(relativeSourcePath.toString())) return;

        Path relativized = relativeSourcePath.relativize(relativeToRoot);
        String topDir = relativized.getName(0).toString();
        if (!DIMENSION_PREFIXES.contains(topDir)) return;

        Path resolvedPath = targetRootPath.resolve(relativized);

        if (directory) {
            Files.createDirectories(resolvedPath);
        } else {
            Files.createDirectories(resolvedPath.getParent());
            Files.copy(inputStream, resolvedPath, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    @FunctionalInterface
    public interface IOSupplier<T> {
        T get() throws IOException;
    }
}
