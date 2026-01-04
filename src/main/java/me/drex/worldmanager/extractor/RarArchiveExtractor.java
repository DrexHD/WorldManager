package me.drex.worldmanager.extractor;

import com.github.junrar.Archive;
import com.github.junrar.exception.RarException;
import com.github.junrar.exception.UnsupportedRarV5Exception;
import com.github.junrar.rarfile.FileHeader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.drex.worldmanager.save.WorldConfig;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import org.apache.commons.io.FilenameUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static me.drex.worldmanager.command.ImportCommand.*;
import static net.minecraft.world.level.storage.LevelResource.LEVEL_DATA_FILE;

public class RarArchiveExtractor implements ArchiveExtractor {
    private Path root = Path.of(".");
    private Archive archive;

    @Override
    public boolean supports(Path path) {
        String extension = FilenameUtils.getExtension(path.toString());
        return extension.equalsIgnoreCase("rar");
    }

    @Override
    public Map<Identifier, WorldConfig> open(Path rarPath, MinecraftServer server) throws IOException, CommandSyntaxException {
        Map<Identifier, WorldConfig> configs = Collections.emptyMap();
        try {
            archive = new Archive(Files.newInputStream(rarPath));
            List<FileHeader> fileHeaders = archive.getFileHeaders();
            for (FileHeader fileHeader : fileHeaders) {
                String fileName = fileHeader.getFileName().replace('\\', '/');
                Path path = Paths.get(fileName);
                if (path.getFileName().toString().equals(LEVEL_DATA_FILE.getId())) {
                    Path parent = path.getParent();
                    if (parent != null) root = parent;

                    try (var is = archive.getInputStream(fileHeader)) {
                        configs = parseWorldConfig(is, server);
                    }
                    break;
                }
            }
        } catch (RarException e) {
            if (e instanceof UnsupportedRarV5Exception) {
                throw RAR5.create(rarPath);
            } else {
                throw new IOException(e);
            }
        }
        return configs;
    }

    @Override
    public void extract(Path relativeSourcePath, Path targetRootPath, MinecraftServer server) throws IOException {
        List<FileHeader> fileHeaders = archive.getFileHeaders();
        for (FileHeader fileHeader : fileHeaders) {
            Path entryPath = Paths.get(fileHeader.getFileName().replace('\\', '/'));
            Path relativeToRoot = root.relativize(entryPath);

            ArchiveExtractor.extractFile(relativeSourcePath, targetRootPath, relativeToRoot, fileHeader.isDirectory(), archive.getInputStream(fileHeader));
        }
    }

    @Override
    public void close() throws IOException {
        archive.close();
    }
}
