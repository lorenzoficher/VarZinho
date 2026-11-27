package br.edu.unipampa.varzinho.infrastructure.stream;

import br.edu.unipampa.varzinho.application.Feed;
import br.edu.unipampa.varzinho.domain.capture.StreamCamera;
import br.edu.unipampa.varzinho.enums.Resolution;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * A phone whose stream FFmpeg is already recording, with the camera that captures from
 * it. This is the one place that knows the phone is reached through FFmpeg; the window
 * only sees a camera and a feed.
 */
public final class PhoneConnection {
    private final String streamUrl;
    private final FfmpegRecorder recorder;
    private final SegmentIndex index;
    private final StreamCamera camera;

    private PhoneConnection(String streamUrl, FfmpegRecorder recorder, SegmentIndex index, StreamCamera camera) {
        this.streamUrl = streamUrl;
        this.recorder = recorder;
        this.index = index;
        this.camera = camera;
    }

    /**
     * Starts recording the phone at {@code streamUrl} into {@code segmentFolder}.
     *
     * @param streamUrl the phone's stream address; blank or missing when there is no phone
     * @param segmentFolder the folder the recorder's ring of segments is written to
     * @param bufferSeconds how many seconds the phone's camera keeps, and so how long its clips are
     * @param onFailure told why, in words for the operator, when an address was given but
     *     the phone could not be reached
     * @return the connected phone; empty when no address was given, or when it failed
     */
    public static Optional<PhoneConnection> open(String streamUrl, Path segmentFolder, int bufferSeconds,
                                                 Consumer<String> onFailure) {
        if (streamUrl == null || streamUrl.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(start(streamUrl, segmentFolder, bufferSeconds));
        } catch (IOException | IllegalArgumentException cause) {
            onFailure.accept("The phone stream could not start (" + cause.getMessage()
                    + "). Opening with the simulated cameras only.");
            return Optional.empty();
        }
    }

    /**
     * Starts recording the phone, failing loudly instead of falling back to no phone.
     *
     * @param streamUrl the phone's stream address
     * @param segmentFolder the folder the recorder's ring of segments is written to
     * @param bufferSeconds how many seconds the phone's camera keeps
     * @return the phone, recording; its footage may not have arrived yet
     * @throws IllegalArgumentException if the address, the folder or the buffer length is invalid
     * @throws IOException if FFmpeg is missing, the folder cannot be cleared, or no loopback
     *         port is free
     */
    public static PhoneConnection start(String streamUrl, Path segmentFolder, int bufferSeconds) throws IOException {
        FfmpegRecorder recorder = new FfmpegRecorder(streamUrl, segmentFolder, freeLoopbackUdpPort());
        recorder.start();
        SegmentIndex index = new SegmentIndex(FfmpegRecorder.RING_SEGMENTS);
        StreamCamera camera = cameraFor(streamUrl, bufferSeconds, index);
        return new PhoneConnection(streamUrl, recorder, index, camera);
    }

    static StreamCamera cameraFor(String streamUrl, int bufferSeconds, SegmentIndex index) {
        return new StreamCamera("phone-1", "Phone", Resolution.HD, bufferSeconds,
                streamUrl, new FfmpegClipAssembler(index));
    }

    /**
     * The camera that captures from this phone.
     *
     * @return the phone's camera
     */
    public StreamCamera camera() {
        return camera;
    }

    /**
     * The address this phone is recorded from.
     *
     * @return the stream address the connection was opened with
     */
    public String streamUrl() {
        return streamUrl;
    }

    /**
     * Where to read the phone's live picture without opening another connection to the
     * phone. See {@link FfmpegRecorder#liveRelay()}.
     *
     * @return the loopback address of the recorder's relay
     */
    public String liveRelay() {
        return recorder.liveRelay();
    }

    // The port is released before FFmpeg binds it; another program taking it in between
    // costs only the broadcast, never the recording, since the relay output may fail.
    private static int freeLoopbackUdpPort() throws IOException {
        try (DatagramSocket probe = new DatagramSocket(0, InetAddress.getLoopbackAddress())) {
            return probe.getLocalPort();
        }
    }

    /**
     * Waits up to fifteen seconds for the first finished segment. Only a completed segment
     * proves that FFmpeg could read the supplied address. On failure the recorder is stopped.
     *
     * @throws IllegalStateException if no segment arrives in time or FFmpeg exits first
     * @throws java.io.UncheckedIOException if the phone refused the connection; its cause
     *         names only the host and port, never the full address
     */
    public void awaitFootage() {
        long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(15);
        var log = new SegmentLog(recorder.segmentList());
        try {
            while (recorder.isRunning() && System.nanoTime() < deadline) {
                if (!log.readNew().isEmpty()) return;
                Thread.sleep(150);
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        } catch (IOException unreadable) {
            recorder.stop();
            throw new IllegalStateException("could not read camera fragments", unreadable);
        }
        boolean stillReading = recorder.isRunning();
        recorder.stop();
        if (stillReading) throw new IllegalStateException("no completed video segment");
        throw recorder.startupFailure();
    }

    /** Stops recording the phone. Safe to call more than once. */
    public void disconnect() { recorder.stop(); }

    /**
     * Builds the feed that moves the phone's new segments into its camera. Call it once:
     * every feed reads the same segment list on its own, so a second one would hand the
     * camera each segment twice.
     *
     * @param onSilence run when the phone's segments stop arriving
     * @return the feed that moves the phone's new segments into its camera
     */
    public Feed feed(Runnable onSilence) {
        return new SegmentFeed(camera, new SegmentLog(recorder.segmentList()), index, onSilence);
    }

    /**
     * Reports the phone dropping out, which ends the recording without anyone asking.
     *
     * @param action run, on FFmpeg's own thread, once the recorder exits for any reason
     *     other than being stopped
     */
    public void onDrop(Runnable action) {
        recorder.onExit(action);
    }
}
