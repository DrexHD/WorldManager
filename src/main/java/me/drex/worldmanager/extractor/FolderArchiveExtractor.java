package me.drex.worldmanager.extractor;

import me.drex.worldmanager.save.WorldConfig;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static me.drex.worldmanager.command.ImportCommand.*;
import static net.minecraft.world.level.storage.LevelResource.LEVEL_DATA_FILE;

public class FolderArchiveExtractor implements ArchiveExtractor {
    private Path root = Path.of(".");

    @Override
    public boolean supports(Path path) {
        return Files.isDirectory(path);
    }

    @Override
    public Map<Identifier, WorldConfig> open(Path folderPath, MinecraftServer server) throws IOException {
        Map<Identifier, WorldConfig> configs = Collections.emptyMap();
        try (Stream<Path> pathStream = Files.find(folderPath, 10, (path, basicFileAttributes) -> path.getFileName().toString().equals(LEVEL_DATA_FILE.getId()))) {
            Optional<Path> first = pathStream.findFirst();
            if (first.isPresent()) {
                Path parent = first.get().getParent();
                if (parent != null) root = parent;

                configs = parseWorldConfig(Files.newInputStream(first.get()), server);
            }
        }
        return configs;
    }

    @Override
    public void extract(Path relativeSourcePath, Path targetRootPath, MinecraftServer server) throws IOException {
        try (Stream<Path> files = Files.walk(root)) {
            for (Path path : files.toList()) {
                Path relativeToRoot = root.relativize(path);
                ArchiveExtractor.extractFile(relativeSourcePath, targetRootPath, relativeToRoot, Files.isDirectory(path), Files.newInputStream(path));
            }
        }
    }

    @Override
    public void close() throws IOException {
    }
}
