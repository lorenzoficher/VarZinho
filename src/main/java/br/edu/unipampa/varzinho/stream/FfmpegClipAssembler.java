package br.edu.unipampa.varzinho.stream;

import br.edu.unipampa.varzinho.domain.capture.ClipAssembler;
import br.edu.unipampa.varzinho.domain.capture.Frame;
import br.edu.unipampa.varzinho.exception.ClipAssemblyException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Writes a clip by joining the segment files of its seconds with FFmpeg, without
 * re-encoding them.
 *
 * <p>The segments are copied aside before FFmpeg starts: the recorder keeps
 * overwriting the ring meanwhile, and a copy is the only way to be sure the clip holds
 * the seconds that were asked for.
 */
public final class FfmpegClipAssembler implements ClipAssembler {

    private static final long TIMEOUT_SECONDS = 10;
    private static final int OUTPUT_LINES_KEPT = 5;

    private final SegmentIndex index;

    public FfmpegClipAssembler(SegmentIndex index) {
        if (index == null) {
            throw new IllegalArgumentException("an assembler needs to know where the seconds are");
        }
        this.index = index;
    }

    @Override
    public void assemble(List<Frame> window, Path target) {
        List<Path> segments = window.stream().map(index::segmentOf).collect(Collectors.toList());
        Path workspace = null;
        try {
            workspace = Files.createTempDirectory("varzinho-clip-");
            List<Path> frozen = freeze(segments, workspace);
            Path list = Files.writeString(workspace.resolve("list.txt"), concatList(frozen), StandardCharsets.UTF_8);
            Path parent = target.toAbsolutePath().getParent();
            Files.createDirectories(parent);
            join(list, target, workspace.resolve("ffmpeg.log"));
        } catch (IOException cause) {
            throw new ClipAssemblyException("could not write " + target, cause);
        } catch (InterruptedException cause) {
            Thread.currentThread().interrupt();
            throw new ClipAssemblyException("interrupted while writing " + target, cause);
        } finally {
            discard(workspace);
        }
    }

    /**
     * The input FFmpeg's concat demuxer reads: one quoted line per file, in order.
     *
     * <p>Forward slashes and FFmpeg's quote escaping keep a path with spaces or quotes —
     * a Windows home folder, typically — in one piece.
     */
    static String concatList(List<Path> parts) {
        StringBuilder list = new StringBuilder();
        for (Path part : parts) {
            String path = part.toAbsolutePath().toString().replace('\\', '/');
            list.append("file '").append(path.replace("'", "'\\''")).append("'\n");
        }
        return list.toString();
    }

    private static List<Path> freeze(List<Path> segments, Path workspace) throws IOException {
        List<Path> frozen = new ArrayList<>();
        for (int position = 0; position < segments.size(); position++) {
            Path copy = workspace.resolve(String.format("part-%03d.ts", position));
            Files.copy(segments.get(position), copy);
            frozen.add(copy);
        }
        return frozen;
    }

    private static void join(Path list, Path target, Path log) throws IOException, InterruptedException {
        Process ffmpeg = new ProcessBuilder("ffmpeg", "-hide_banner", "-loglevel", "error", "-nostdin", "-y",
                "-f", "concat", "-safe", "0", "-i", list.toString(), "-c", "copy", target.toString())
                .redirectErrorStream(true)
                .redirectOutput(log.toFile())
                .start();
        if (!ffmpeg.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            ffmpeg.destroyForcibly();
            throw new ClipAssemblyException("ffmpeg took longer than " + TIMEOUT_SECONDS + " s to write " + target);
        }
        if (ffmpeg.exitValue() != 0) {
            throw new ClipAssemblyException("ffmpeg could not write " + target + ": " + lastLines(log));
        }
    }

    private static String lastLines(Path log) throws IOException {
        List<String> lines = Files.readAllLines(log);
        return String.join(" | ", lines.subList(Math.max(0, lines.size() - OUTPUT_LINES_KEPT), lines.size()));
    }

    // Leftovers in the temp folder must not turn a written clip into a failed capture,
    // so what cannot be deleted now is left for the JVM to delete on exit.
    private static void discard(Path workspace) {
        if (workspace == null) {
            return;
        }
        try (Stream<Path> files = Files.walk(workspace)) {
            files.sorted(Comparator.reverseOrder()).forEach(FfmpegClipAssembler::deleteOrDefer);
        } catch (IOException unreadable) {
            workspace.toFile().deleteOnExit();
        }
    }

    private static void deleteOrDefer(Path file) {
        try {
            Files.delete(file);
        } catch (IOException locked) {
            file.toFile().deleteOnExit();
        }
    }
}
