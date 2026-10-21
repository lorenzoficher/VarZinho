package br.edu.unipampa.varzinho.stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import br.edu.unipampa.varzinho.domain.capture.Frame;
import br.edu.unipampa.varzinho.exception.ClipAssemblyException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FfmpegClipAssemblerTest {

    @TempDir
    Path folder;

    @Test
    void listsTheSegmentsInTheOrderOfTheWindow() {
        Path first = folder.resolve("part-000.ts");
        Path second = folder.resolve("part-001.ts");

        String list = FfmpegClipAssembler.concatList(List.of(first, second));

        assertEquals("file '" + slashed(first) + "'\nfile '" + slashed(second) + "'\n", list);
    }

    @Test
    void keepsPathsWithSpacesAndQuotesInOnePiece() {
        Path awkward = folder.resolve("Lorenzo Ficher").resolve("it's.ts");

        String list = FfmpegClipAssembler.concatList(List.of(awkward));

        String escaped = slashed(awkward).replace("'", "'\\''");
        assertEquals("file '" + escaped + "'\n", list);
    }

    @Test
    void refusesAWindowWithASecondItHasNoSegmentFor() {
        FfmpegClipAssembler assembler = new FfmpegClipAssembler(new SegmentIndex(40));
        Path target = folder.resolve("clips").resolve("clip.mp4");

        assertThrows(ClipAssemblyException.class,
                () -> assembler.assemble(List.of(frame(1)), target));
        assertFalse(Files.exists(target));
    }

    @Test
    void leavesNoFileAtTheTargetWhenFfmpegFailsHalfway() throws IOException {
        Path fakeFfmpeg = script("ffmpeg-fails", "echo half > \"$last\"\necho broken segment\nexit 1");
        Path target = folder.resolve("clips").resolve("clip.mp4");

        ClipAssemblyException failure = assertThrows(ClipAssemblyException.class,
                () -> assemblerRunning(fakeFfmpeg, Duration.ofSeconds(10)).assemble(List.of(frame(1)), target));

        assertTrue(failure.getMessage().contains("broken segment"));
        assertFalse(Files.exists(target));
        assertNothingButTheTargetIn(target.getParent());
    }

    @Test
    void leavesNoFileAtTheTargetWhenFfmpegTimesOut() throws IOException {
        Path fakeFfmpeg = script("ffmpeg-hangs", "echo half > \"$last\"\nexec sleep 30");
        Path target = folder.resolve("clips").resolve("clip.mp4");

        assertThrows(ClipAssemblyException.class,
                () -> assemblerRunning(fakeFfmpeg, Duration.ofMillis(500)).assemble(List.of(frame(1)), target));

        assertFalse(Files.exists(target));
        assertNothingButTheTargetIn(target.getParent());
    }

    @Test
    void movesTheFinishedFileToTheTarget() throws IOException {
        Path fakeFfmpeg = script("ffmpeg-works", "echo clip > \"$last\"");
        Path target = folder.resolve("clips").resolve("clip.mp4");

        assemblerRunning(fakeFfmpeg, Duration.ofSeconds(10)).assemble(List.of(frame(1)), target);

        assertEquals(List.of("clip"), Files.readAllLines(target));
        assertNothingButTheTargetIn(target.getParent());
    }

    @Test
    void writesOnePlayableFileFromRealSegments() throws IOException, InterruptedException {
        assumeTrue(ffmpegAvailable(), "ffmpeg is not on PATH");
        SegmentIndex index = new SegmentIndex(40);
        for (int sequence = 1; sequence <= 2; sequence++) {
            Path segment = folder.resolve("seg-0" + sequence + ".ts");
            generateOneSecond(segment);
            index.register(sequence, segment);
        }
        Path target = folder.resolve("clips").resolve("clip.mp4");

        new FfmpegClipAssembler(index).assemble(List.of(frame(1), frame(2)), target);

        assertTrue(Files.size(target) > 0);
    }

    private FfmpegClipAssembler assemblerRunning(Path executable, Duration timeout) throws IOException {
        Path segment = Files.writeString(folder.resolve("seg-01.ts"), "one second");
        SegmentIndex index = new SegmentIndex(40);
        index.register(1, segment);
        return new FfmpegClipAssembler(index, executable.toString(), timeout);
    }

    // A stand-in for FFmpeg that writes into its last argument, the output, as the real one does.
    private Path script(String name, String body) throws IOException {
        assumeFalse(System.getProperty("os.name").startsWith("Windows"), "the fake ffmpeg is a POSIX shell script");
        Path script = folder.resolve(name);
        Files.writeString(script, "#!/bin/sh\nfor last; do :; done\n" + body + "\n");
        Files.setPosixFilePermissions(script, PosixFilePermissions.fromString("rwx------"));
        return script;
    }

    private static void assertNothingButTheTargetIn(Path clips) throws IOException {
        try (Stream<Path> files = Files.list(clips)) {
            assertEquals(List.of(), files.filter(file -> !file.getFileName().toString().equals("clip.mp4"))
                    .collect(Collectors.toList()));
        }
    }

    private static String slashed(Path path) {
        return path.toAbsolutePath().toString().replace('\\', '/');
    }

    private static Frame frame(int sequence) {
        return new Frame(Instant.ofEpochSecond(sequence), sequence);
    }

    private static void generateOneSecond(Path segment) throws IOException, InterruptedException {
        Process process = new ProcessBuilder("ffmpeg", "-hide_banner", "-loglevel", "error", "-nostdin", "-y",
                "-f", "lavfi", "-i", "testsrc=duration=1:size=64x64:rate=10",
                "-c:v", "libx264", "-pix_fmt", "yuv420p", "-f", "mpegts", segment.toString())
                .redirectErrorStream(true)
                .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                .start();
        assertTrue(process.waitFor(20, TimeUnit.SECONDS) && process.exitValue() == 0,
                "could not generate " + segment);
    }

    private static boolean ffmpegAvailable() {
        try {
            Process process = new ProcessBuilder("ffmpeg", "-version")
                    .redirectErrorStream(true)
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .start();
            return process.waitFor(10, TimeUnit.SECONDS) && process.exitValue() == 0;
        } catch (IOException notInstalled) {
            return false;
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
