package me.drex.worldmanager.extractor;

import net.minecraft.server.MinecraftServer;
import org.apache.commons.io.FilenameUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ZipArchiveExtractor extends ArchiveExtractor {
    private ZipFile zipFile;

    @Override
    public boolean supports(Path path) {
        String extension = FilenameUtils.getExtension(path.toString());
        return extension.equalsIgnoreCase("zip");
    }

    @Override
    public void open(Path zipPath, MinecraftServer server) throws IOException {
        zipFile = new ZipFile(zipPath.toFile());
        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        while (entries.hasMoreElements()) {
            ZipEntry zipEntry = entries.nextElement();
            Path path = Path.of(zipEntry.getName());
            readFile(path, () -> zipFile.getInputStream(zipEntry), server);
        }
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
