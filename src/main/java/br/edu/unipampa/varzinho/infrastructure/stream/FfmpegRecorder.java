package br.edu.unipampa.varzinho.infrastructure.stream;

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
 *
 * <p>This is the only process that reads the phone. Some phone apps (DroidCam) serve
 * their video to a single client, so anything else that wants the live picture — the
 * spectator broadcast — reads the {@link #liveRelay() relay} this process publishes on
 * the loopback instead of asking the phone a second time.
 */
public final class FfmpegRecorder {

    public static final int RING_SEGMENTS = 40;

    private static final String SEGMENT_LIST = "segments.csv";
    private static final String SEGMENT_PATTERN = "seg-%02d.ts";

    /**
     * How long FFmpeg waits on a stream that sends nothing before it gives up and exits.
     * A phone that leaves the Wi-Fi closes no connection, so without this the read
     * would block forever and nobody would learn the phone is gone.
     */
    public static final long READ_TIMEOUT_MICROSECONDS = 5_000_000;

    private static final long STOP_GRACE_SECONDS = 3;

    private static final Set<String> STREAM_PROTOCOLS = Set.of("http", "https", "rtsp");

    private static final String LOOPBACK = "127.0.0.1";

    /** MPEG-TS packets fit seven to a datagram this size, under any Ethernet MTU. */
    private static final int RELAY_PACKET_BYTES = 1316;

    /** Enough for a few seconds of 1080p, so a busy reader drops nothing on the loopback. */
    private static final int RELAY_READER_FIFO_PACKETS = 100_000;

    private static final int HIGHEST_PORT = 65_535;

    private final String streamUrl;
    private final Path bufferFolder;
    private final int relayPort;
    private Process process;
    private volatile boolean stopRequested;

    /**
     * @param relayPort the loopback UDP port the recorded video is also published on
     * @throws IllegalArgumentException if the address is not an http, https or rtsp
     *         address with a host, the folder is missing, or the port is not a valid one
     */
    public FfmpegRecorder(String streamUrl, Path bufferFolder, int relayPort) {
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
        if (relayPort < 1 || relayPort > HIGHEST_PORT) {
            throw new IllegalArgumentException("relay port " + relayPort + " is outside 1-" + HIGHEST_PORT);
        }
        this.streamUrl = streamUrl;
        this.bufferFolder = bufferFolder;
        this.relayPort = relayPort;
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
        Path executable = FfmpegExecutable.find().orElseThrow(() -> new java.io.FileNotFoundException("ffmpeg"));
        clearBufferFolder();
        // FFmpeg's output goes to a file: an unread pipe fills up and freezes it.
        // It runs inside the buffer folder because the tee outputs name their files
        // relative to it; see command().
        process = new ProcessBuilder(command(executable.toString()))
                .directory(bufferFolder.toFile())
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
        return bufferFolder.resolve(SEGMENT_LIST);
    }

    /**
     * The address another FFmpeg reads to receive the video this recorder encodes, as
     * it is encoded. Nobody listening costs nothing: the datagrams are simply dropped.
     * A reader gives up after {@link #READ_TIMEOUT_MICROSECONDS} without a packet, which
     * is how a dropped phone reaches it too.
     */
    public String liveRelay() {
        return "udp://" + LOOPBACK + ":" + relayPort + "?timeout=" + READ_TIMEOUT_MICROSECONDS
                + "&fifo_size=" + RELAY_READER_FIFO_PACKETS + "&overrun_nonfatal=1";
    }

    boolean isRunning() { return process != null && process.isAlive(); }

    RuntimeException startupFailure() {
        var failure = new IllegalStateException("recorder exited before footage");
        // Report the configured host only. FFmpeg diagnostics can contain credentials.
        try (Stream<String> log = Files.lines(bufferFolder.resolve("recorder.log"), StandardCharsets.ISO_8859_1)) {
            if (log.anyMatch(line -> line.startsWith("[tcp @") && line.contains("Connection to tcp://")
                    && line.contains(" failed:"))) {
                URI address = URI.create(streamUrl.strip());
                String endpoint = address.getHost();
                if (address.getPort() >= 0) endpoint += ":" + address.getPort();
                return new java.io.UncheckedIOException(new java.net.ConnectException(endpoint));
            }
        } catch (IOException | java.io.UncheckedIOException unreadable) {
            failure.addSuppressed(unreadable);
        }
        return failure;
    }

    List<String> command() {
        return command(FfmpegExecutable.command());
    }

    private List<String> command(String executable) {
        // No -nostdin here, unlike the assembler: stdin is how stop() asks for a clean exit.
        return List.of(executable, "-hide_banner", "-loglevel", "warning",
                "-rw_timeout", String.valueOf(READ_TIMEOUT_MICROSECONDS),
                "-i", streamUrl,
                "-an", "-r", "30", "-c:v", "libx264", "-preset", "veryfast", "-tune", "zerolatency",
                "-pix_fmt", "yuv420p",
                "-force_key_frames", "expr:gte(t,n_forced*1)",
                "-map", "0:v", "-f", "tee", teeOutputs());
    }

    // One encode, two outputs. The ring's files are named relative to the working
    // directory because the tee syntax gives ':' and the backslash a meaning of their own,
    // so a Windows folder path, or any path with them, would need escaping.
    // onfail=ignore keeps the ring recording whatever happens to the relay.
    private String teeOutputs() {
        String ring = "[f=segment:segment_time=1:segment_wrap=" + RING_SEGMENTS + ":reset_timestamps=1"
                + ":segment_list=" + SEGMENT_LIST + ":segment_list_type=csv:segment_list_size=0]" + SEGMENT_PATTERN;
        String relay = "[f=mpegts:onfail=ignore]udp://" + LOOPBACK + ":" + relayPort
                + "?pkt_size=" + RELAY_PACKET_BYTES;
        return ring + "|" + relay;
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
