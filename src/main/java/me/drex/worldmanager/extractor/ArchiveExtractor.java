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
import java.util.Map;

import static me.drex.worldmanager.command.ImportCommand.DIMENSION_PREFIXES;

public interface ArchiveExtractor {
    boolean supports(Path path);

    // TODO support reading world configs from datapacks
    Map<Identifier, WorldConfig> open(Path archive, MinecraftServer server) throws IOException, CommandSyntaxException;

    void extract(Path relativeSourcePath, Path targetRootPath, MinecraftServer server) throws IOException;

    void close() throws IOException;

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
}
