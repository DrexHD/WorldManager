package me.drex.worldmanager.extractor;

import me.drex.worldmanager.save.WorldConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import org.apache.commons.io.FilenameUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static me.drex.worldmanager.command.ImportCommand.*;
import static net.minecraft.world.level.storage.LevelResource.LEVEL_DATA_FILE;

public class ZipArchiveExtractor implements ArchiveExtractor {

    private Path root = Path.of(".");
    private ZipFile zipFile;

    @Override
    public boolean supports(Path path) {
        String extension = FilenameUtils.getExtension(path.toString());
        return extension.equalsIgnoreCase("zip");
    }

    @Override
    public Map<ResourceLocation, WorldConfig> open(Path zipPath, MinecraftServer server) throws IOException {
        Map<ResourceLocation, WorldConfig> configs = Collections.emptyMap();
        zipFile = new ZipFile(zipPath.toFile());
        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        while (entries.hasMoreElements()) {
            ZipEntry zipEntry = entries.nextElement();
            Path path = Path.of(zipEntry.getName());
            if (path.getFileName().toString().equals(LEVEL_DATA_FILE.getId())) {
                try (var is = zipFile.getInputStream(zipEntry)) {
                    configs = parseWorldConfig(is, server);
                    Path parent = path.getParent();
                    if (parent != null) root = parent;
                }
                break;
            }
        }
        return configs;
    }

    @Override
    public void extract(Path relativeSourcePath, Path targetRootPath, MinecraftServer server) throws IOException {
        // Copy files
        Iterator<? extends ZipEntry> iterator = zipFile.entries().asIterator();
        while (iterator.hasNext()) {
            ZipEntry zipEntry = iterator.next();
            Path entryPath = Paths.get(zipEntry.getName());
            Path relativeToRoot = root.relativize(entryPath);
            ArchiveExtractor.extractFile(relativeSourcePath, targetRootPath, relativeToRoot, zipEntry.isDirectory(), zipFile.getInputStream(zipEntry));
        }
    }

    @Override
    public void close() throws IOException {
        zipFile.close();
    }
}
