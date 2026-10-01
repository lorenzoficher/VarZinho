package br.edu.unipampa.varzinho.stream;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
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

    /**
     * How long FFmpeg waits on a stream that sends nothing before it gives up and exits.
     * A phone that leaves the Wi-Fi closes no connection, so without this the read
     * would block forever and nobody would learn the phone is gone.
     */
    public static final long READ_TIMEOUT_MICROSECONDS = 5_000_000;

    private static final long STOP_GRACE_SECONDS = 3;

    private static final Set<String> STREAM_PROTOCOLS = Set.of("http", "https", "rtsp");

    private final String streamUrl;
    private final Path bufferFolder;
    private Process process;
    private volatile boolean stopRequested;

    /**
     * @throws IllegalArgumentException if the address is not an http, https or rtsp
     *         address with a host, or the folder is missing
     */
    public FfmpegRecorder(String streamUrl, Path bufferFolder) {
        if (streamUrl == null || streamUrl.isBlank()) {
            throw new IllegalArgumentException("a recorder needs the address it records from");
        }
        if (!isStreamAddress(streamUrl)) {
            throw new IllegalArgumentException("\"" + streamUrl + "\" is not a stream address; it needs"
                    + " the protocol and the host, as in http://192.168.0.42:8080/video");
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
        stopRequested = false;
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
        stopRequested = true;
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
     * Runs the action if FFmpeg exits without being asked to — typically the phone
     * dropping off the network. An exit {@link #stop} asked for is not reported. The
     * action runs on a background thread.
     *
     * @throws IllegalStateException if the recorder was never started
     */
    public void onExit(Runnable action) {
        if (process == null) {
            throw new IllegalStateException("the recorder has not started");
        }
        process.onExit().thenRun(() -> {
            if (!stopRequested) {
                action.run();
            }
        });
    }

    public Path segmentList() {
        return bufferFolder.resolve("segments.csv");
    }

    List<String> command() {
        // No -nostdin here, unlike the assembler: stdin is how stop() asks for a clean exit.
        return List.of("ffmpeg", "-hide_banner", "-loglevel", "warning",
                "-rw_timeout", String.valueOf(READ_TIMEOUT_MICROSECONDS),
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

    // Without a protocol FFmpeg reads the address as a file name, fails at once and
    // leaves the phone's court silent with nothing on screen to say why.
    private static boolean isStreamAddress(String streamUrl) {
        try {
            URI address = new URI(streamUrl.strip());
            return address.getScheme() != null
                    && STREAM_PROTOCOLS.contains(address.getScheme().toLowerCase(Locale.ROOT))
                    && address.getHost() != null;
        } catch (URISyntaxException notAnAddress) {
            return false;
        }
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
