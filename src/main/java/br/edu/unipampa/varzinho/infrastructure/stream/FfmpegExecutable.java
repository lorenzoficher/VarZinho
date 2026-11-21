package br.edu.unipampa.varzinho.infrastructure.stream;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** WinGet can install the package while an already-open IDE still has the old PATH. */
public final class FfmpegExecutable {
    private FfmpegExecutable() { }

    public static Optional<Path> find() {
        String path = java.util.Objects.toString(System.getenv("PATH"), "");
        if (File.separatorChar != '\\') return onPath(path, ":", "ffmpeg");
        List<Path> roots = new ArrayList<>();
        String local = System.getenv("LOCALAPPDATA");
        if (local != null && !local.isBlank()) roots.add(Path.of(local));
        roots.add(Path.of(System.getProperty("user.home"), "AppData", "Local"));
        return findWindows(path, roots);
    }

    public static String command() {
        return find().map(Path::toString).orElse("ffmpeg");
    }

    static Optional<Path> findWindows(String path, List<Path> localRoots) {
        Optional<Path> configured = onPath(path, ";", "ffmpeg.exe");
        if (configured.isPresent()) return configured;
        for (Path root : localRoots) {
            Path winGet = root.resolve("Microsoft/WinGet");
            Path link = winGet.resolve("Links/ffmpeg.exe");
            if (executable(link)) return Optional.of(link);
            Path pkg = winGet.resolve("Packages/Gyan.FFmpeg_Microsoft.Winget.Source_8wekyb3d8bbwe");
            if (!Files.isDirectory(pkg)) continue;
            try (var versions = Files.list(pkg)) {
                var binary = versions.map(version -> version.resolve("bin/ffmpeg.exe"))
                        .filter(FfmpegExecutable::executable)
                        .max(Comparator.comparingLong(file -> file.toFile().lastModified()));
                if (binary.isPresent()) return binary;
            } catch (IOException inaccessible) {
                // A redirected profile can be unreadable while the regular user profile is accessible.
                continue;
            }
        }
        return Optional.empty();
    }

    private static Optional<Path> onPath(String path, String separator, String name) {
        for (String entry : path.split(separator)) {
            try {
                Path dir = Path.of(entry.replace("\"", ""));
                Path file = dir.resolve(name);
                if (dir.isAbsolute() && executable(file)) return Optional.of(file);
            } catch (InvalidPathException malformed) {
                continue;
            }
        }
        return Optional.empty();
    }

    private static boolean executable(Path file) {
        return Files.isRegularFile(file) && Files.isExecutable(file);
    }
}
