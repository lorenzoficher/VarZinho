package br.edu.unipampa.varzinho.stream;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * The FFmpeg process that records the phone's stream into a ring of one-second
 * segment files, for as long as the application runs.
 *
 * <p>The ring is larger than any buffer reads from it: the spare segments are what
 * keep FFmpeg from overwriting a second while a clip is still copying it.
 */
public final class FfmpegRecorder {

    public static final int RING_SEGMENTS = 40;

    private static final long STOP_GRACE_SECONDS = 3;

    private final String streamUrl;
    private final Path bufferFolder;
    private Process process;

    public FfmpegRecorder(String streamUrl, Path bufferFolder) {
        if (streamUrl == null || streamUrl.isBlank()) {
            throw new IllegalArgumentException("a recorder needs the address it records from");
        }
        if (bufferFolder == null) {
            throw new IllegalArgumentException("a recorder needs a folder for its segments");
        }
        this.streamUrl = streamUrl;
        this.bufferFolder = bufferFolder;
    }

    /**
     * Empties the buffer folder and starts recording into it. Segments left from an
     * earlier run belong to a moment nobody is watching any more.
     *
     * @throws IllegalStateException if the recorder is already running
     * @throws IOException if the folder cannot be cleared or FFmpeg cannot be started
     */
    public void start() throws IOException {
        if (process != null && process.isAlive()) {
            throw new IllegalStateException("the recorder is already running");
        }
        clearBufferFolder();
        // FFmpeg's output goes to a file: an unread pipe fills up and freezes it.
        process = new ProcessBuilder(command())
                .redirectErrorStream(true)
                .redirectOutput(bufferFolder.resolve("recorder.log").toFile())
                .start();
        Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
    }

    /**
     * Asks FFmpeg to finish cleanly, and kills it if it does not. Safe to call more than
     * once, and on a recorder that never started.
     */
    public void stop() {
        if (process == null || !process.isAlive()) {
            return;
        }
        try (OutputStream input = process.getOutputStream()) {
            input.write("q".getBytes(StandardCharsets.US_ASCII));
            input.flush();
        } catch (IOException alreadyGone) {
            process.destroy();
        }
        try {
            if (!process.waitFor(STOP_GRACE_SECONDS, TimeUnit.SECONDS)) {
                process.destroyForcibly();
            }
        } catch (InterruptedException interrupted) {
            process.destroyForcibly();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Runs the action once FFmpeg exits, for whatever reason — typically the phone
     * dropping off the network. It runs on a background thread.
     *
     * @throws IllegalStateException if the recorder was never started
     */
    public void onExit(Runnable action) {
        if (process == null) {
            throw new IllegalStateException("the recorder has not started");
        }
        process.onExit().thenRun(action);
    }

    public Path segmentList() {
        return bufferFolder.resolve("segments.csv");
    }

    List<String> command() {
        // No -nostdin here, unlike the assembler: stdin is how stop() asks for a clean exit.
        return List.of("ffmpeg", "-hide_banner", "-loglevel", "warning",
                "-i", streamUrl,
                "-an", "-r", "30", "-c:v", "libx264", "-preset", "veryfast", "-tune", "zerolatency",
                "-pix_fmt", "yuv420p",
                "-force_key_frames", "expr:gte(t,n_forced*1)",
                "-f", "segment", "-segment_time", "1", "-segment_wrap", String.valueOf(RING_SEGMENTS),
                "-reset_timestamps", "1",
                "-segment_list", segmentList().toString(), "-segment_list_type", "csv",
                "-segment_list_size", "0",
                bufferFolder.resolve("seg-%02d.ts").toString());
    }

    private void clearBufferFolder() throws IOException {
        if (Files.exists(bufferFolder)) {
            try (Stream<Path> files = Files.walk(bufferFolder)) {
                for (Path file : (Iterable<Path>) files.sorted(Comparator.reverseOrder())::iterator) {
                    Files.delete(file);
                }
            }
        }
        Files.createDirectories(bufferFolder);
    }
}
