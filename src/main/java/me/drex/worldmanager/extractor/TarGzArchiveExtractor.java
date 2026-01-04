package me.drex.worldmanager.extractor;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.drex.worldmanager.save.WorldConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.io.FilenameUtils;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;

import static me.drex.worldmanager.command.ImportCommand.*;
import static net.minecraft.world.level.storage.LevelResource.LEVEL_DATA_FILE;

public class TarGzArchiveExtractor implements ArchiveExtractor {
    private Path root = Path.of(".");
    private Path tarGzPath;

    @Override
    public boolean supports(Path path) {
        String name = FilenameUtils.getName(path.toString().toLowerCase());
        return name.endsWith(".tar.gz") || name.endsWith(".tgz");
    }

    @Override
    public Map<ResourceLocation, WorldConfig> open(Path tarGzPath, MinecraftServer server) throws IOException, CommandSyntaxException {
        this.tarGzPath = tarGzPath;
        Map<ResourceLocation, WorldConfig> configs = Collections.emptyMap();
        try (InputStream fi = Files.newInputStream(tarGzPath);
             InputStream bi = new BufferedInputStream(fi);
             InputStream gzi = new GzipCompressorInputStream(bi);
             TarArchiveInputStream tarIn = new TarArchiveInputStream(gzi)) {

            TarArchiveEntry entry;
            while ((entry = tarIn.getNextEntry()) != null) {
                Path entryPath = Paths.get(entry.getName());
                if (entryPath.getFileName().toString().equals(LEVEL_DATA_FILE.getId())) {
                    Path parent = entryPath.getParent();
                    if (parent != null) root = parent;

                    configs = parseWorldConfig(tarIn, server);
                    break;
                }
            }
        }
        return configs;
    }

    @Override
    public void extract(Path relativeSourcePath, Path targetRootPath, MinecraftServer server) throws IOException {
        try (InputStream fi2 = Files.newInputStream(tarGzPath);
             InputStream bi2 = new BufferedInputStream(fi2);
             InputStream gzi2 = new GzipCompressorInputStream(bi2);
             TarArchiveInputStream tarIn2 = new TarArchiveInputStream(gzi2)) {

            TarArchiveEntry entry;
            while ((entry = tarIn2.getNextEntry()) != null) {
                Path entryPath = Paths.get(entry.getName());
                Path relativeToRoot = root.relativize(entryPath);
                ArchiveExtractor.extractFile(relativeSourcePath, targetRootPath, relativeToRoot, entry.isDirectory(), tarIn2);

            }
        }
    }

    @Override
    public void close() throws IOException {

    }
}
