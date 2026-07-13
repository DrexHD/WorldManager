package me.drex.worldmanager.extractor;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
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

public class TarGzArchiveExtractor extends ArchiveExtractor {
    private Path tarGzPath;

    @Override
    public boolean supports(Path path) {
        String name = FilenameUtils.getName(path.toString().toLowerCase());
        return name.endsWith(".tar.gz") || name.endsWith(".tgz");
    }

    @Override
    public void open(Path tarGzPath, MinecraftServer server) throws IOException, CommandSyntaxException {
        this.tarGzPath = tarGzPath;
        try (InputStream fi = Files.newInputStream(tarGzPath);
             InputStream bi = new BufferedInputStream(fi);
             InputStream gzi = new GzipCompressorInputStream(bi);
             TarArchiveInputStream tarIn = new TarArchiveInputStream(gzi)) {

            TarArchiveEntry entry;
            while ((entry = tarIn.getNextEntry()) != null) {
                Path entryPath = Paths.get(entry.getName());
                readFile(entryPath, () -> tarIn, server);
            }
        }
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
