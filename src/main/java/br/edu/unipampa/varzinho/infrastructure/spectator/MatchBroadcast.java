package br.edu.unipampa.varzinho.infrastructure.spectator;

import br.edu.unipampa.varzinho.infrastructure.stream.FfmpegExecutable;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/** The player and the clip assembler use the same fragment clock. */
final class MatchBroadcast implements AutoCloseable {
    static final int SEGMENT_MS = 1000;
    private final Path folder;
    private final Process encoder;
    private final String session = UUID.randomUUID().toString();

    MatchBroadcast(Path parent, List<String> input) throws IOException {
        folder = Files.createTempDirectory(Files.createDirectories(parent), "match-");
        List<String> command = new ArrayList<>(List.of(FfmpegExecutable.command(), "-nostdin", "-hide_banner", "-loglevel", "error"));
        command.addAll(input);
        command.addAll(List.of("-an", "-vf", "scale=960:540:force_original_aspect_ratio=decrease:force_divisible_by=2,fps=25",
                "-c:v", "libx264", "-profile:v", "baseline", "-level", "3.1", "-pix_fmt", "yuv420p",
                "-preset", "veryfast", "-tune", "zerolatency", "-g", "25", "-keyint_min", "25", "-sc_threshold", "0", "-bf", "0",
                "-maxrate", "1700k", "-bufsize", "1700k", "-f", "hls", "-hls_time", "1", "-hls_list_size", "150",
                "-hls_flags", "delete_segments+temp_file+independent_segments", "-hls_segment_type", "fmp4",
                "-hls_segment_filename", "part-%d.m4s", "match.m3u8"));
        encoder = new ProcessBuilder(command).directory(folder.toFile())
                .redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.DISCARD).start();
    }

    String session() { return session; }

    private List<Integer> available() throws IOException {
        List<Integer> segments = new ArrayList<>();
        for (String line : Files.readAllLines(folder.resolve("match.m3u8"))) {
            if (line.matches("part-[0-9]{1,9}\\.m4s"))
                segments.add(Integer.valueOf(line.substring(5, line.length() - 4)));
        }
        if (segments.isEmpty()) throw new IOException("no footage available");
        return segments;
    }

    int latest() throws IOException {
        if (!encoder.isAlive()) throw new IOException("broadcast ended");
        List<Integer> segments = available();
        return segments.get(segments.size() - 1);
    }

    Path media(String name) {
        if (!name.equals("init.mp4") && !name.matches("part-[0-9]{1,9}\\.m4s"))
            throw new IllegalArgumentException("invalid segment");
        return folder.resolve(name);
    }

    void reserve(long endMs, Path target) throws IOException {
        if (endMs < 30_000 || endMs > Integer.MAX_VALUE * (long) SEGMENT_MS)
            throw new IllegalArgumentException("invalid playback time");
        long start = endMs - 30_000;
        int first = (int) (start / SEGMENT_MS);
        int last = (int) ((endMs - 1) / SEGMENT_MS);
        List<Integer> parts = available();
        if (!parts.contains(first) || !parts.contains(last)) throw new IOException("footage no longer available");
        Path input = Files.createTempFile(folder, "reserve-", ".mp4");
        try {
            try (OutputStream out = Files.newOutputStream(input)) {
                Files.copy(media("init.mp4"), out);
                for (int part = first; part <= last; part++) Files.copy(media("part-" + part + ".m4s"), out);
            }
            Process export = new ProcessBuilder(FfmpegExecutable.command(), "-nostdin", "-hide_banner", "-loglevel", "error", "-y",
                    "-i", input.toString(), "-ss", String.format(Locale.ROOT, "%.3f", (start % SEGMENT_MS) / 1000.0),
                    "-t", "30", "-an", "-c:v", "libx264", "-preset", "ultrafast", "-movflags", "+faststart",
                    target.toAbsolutePath().toString()).redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.DISCARD).start();
            try {
                if (!export.waitFor(45, TimeUnit.SECONDS) || export.exitValue() != 0) throw new IOException("clip export failed");
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw new IOException("clip export interrupted", interrupted);
            } finally { export.destroyForcibly(); }
        } finally { Files.deleteIfExists(input); }
    }

    public void close() throws IOException {
        encoder.destroyForcibly();
        try { encoder.onExit().join(); }
        finally {
            try (Stream<Path> files = Files.list(folder)) {
                for (Path file : files.toList()) Files.deleteIfExists(file);
            }
            Files.deleteIfExists(folder);
        }
    }
}
