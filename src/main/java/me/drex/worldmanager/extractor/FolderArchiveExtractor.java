package me.drex.worldmanager.extractor;

import net.minecraft.server.MinecraftServer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

public class FolderArchiveExtractor extends ArchiveExtractor {

    @Override
    public boolean supports(Path path) {
        return Files.isDirectory(path);
    }

    @Override
    public void open(Path folderPath, MinecraftServer server) throws IOException {
        try (Stream<Path> pathStream = Files.walk(folderPath)) {
            for (Path path : pathStream.toList()) {
                readFile(path, () -> Files.newInputStream(path), server);
            }
        }
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
