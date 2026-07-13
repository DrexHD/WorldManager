package me.drex.worldmanager.extractor;

import com.github.junrar.Archive;
import com.github.junrar.exception.RarException;
import com.github.junrar.exception.UnsupportedRarV5Exception;
import com.github.junrar.rarfile.FileHeader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.server.MinecraftServer;
import org.apache.commons.io.FilenameUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static me.drex.worldmanager.command.ImportCommand.*;

public class RarArchiveExtractor extends ArchiveExtractor {
    private Archive archive;

    @Override
    public boolean supports(Path path) {
        String extension = FilenameUtils.getExtension(path.toString());
        return extension.equalsIgnoreCase("rar");
    }

    @Override
    public void open(Path rarPath, MinecraftServer server) throws IOException, CommandSyntaxException {
        try {
            archive = new Archive(Files.newInputStream(rarPath));
            List<FileHeader> fileHeaders = archive.getFileHeaders();
            for (FileHeader fileHeader : fileHeaders) {
                String fileName = fileHeader.getFileName().replace('\\', '/');
                Path path = Paths.get(fileName);
                readFile(path, () -> archive.getInputStream(fileHeader), server);
            }
        } catch (RarException e) {
            if (e instanceof UnsupportedRarV5Exception) {
                throw RAR5.create(rarPath);
            } else {
                throw new IOException(e);
            }
        }
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
