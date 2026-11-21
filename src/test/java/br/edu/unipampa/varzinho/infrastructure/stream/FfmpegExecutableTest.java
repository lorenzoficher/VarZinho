package br.edu.unipampa.varzinho.infrastructure.stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FfmpegExecutableTest {
    @TempDir Path local;

    @Test
    void findsTheWinGetPackageWithoutAWorkingPathOrAlias() throws Exception {
        Path bin = local.resolve("Microsoft/WinGet/Packages/Gyan.FFmpeg_Microsoft.Winget.Source_8wekyb3d8bbwe/ffmpeg-9.1-full_build/bin");
        Files.createDirectories(bin);
        Path exe = Files.createFile(bin.resolve("ffmpeg.exe"));
        exe.toFile().setExecutable(true);
        assertEquals(exe, FfmpegExecutable.findWindows("", List.of(local)).orElseThrow());
        Path preferred = Files.createDirectories(local.resolve("tools")).resolve("ffmpeg.exe");
        Files.copy(exe, preferred);
        preferred.toFile().setExecutable(true);
        assertEquals(preferred, FfmpegExecutable.findWindows(preferred.getParent().toString(), List.of(local)).orElseThrow());
    }

    @Test
    void reportsAnAbsentInstallation() {
        assertTrue(FfmpegExecutable.findWindows("", List.of(local)).isEmpty());
    }
}
