package br.edu.unipampa.varzinho.ui;

import br.edu.unipampa.varzinho.domain.capture.StreamCamera;
import br.edu.unipampa.varzinho.enums.Resolution;
import br.edu.unipampa.varzinho.stream.FfmpegClipAssembler;
import br.edu.unipampa.varzinho.stream.FfmpegRecorder;
import br.edu.unipampa.varzinho.stream.SegmentIndex;
import br.edu.unipampa.varzinho.stream.SegmentLog;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * A phone whose stream FFmpeg is already recording, with the camera that captures from
 * it. This is the one place that knows the phone is reached through FFmpeg; the window
 * only sees a camera and a feed.
 */
final class PhoneConnection {
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
     * @param bufferSeconds how many seconds the phone's camera keeps, and so how long its clips are
     * @param pendingWrites where the camera leaves its clip writes for the capture panel
     * @param onFailure told why, in words for the operator, when an address was given but
     *     the phone could not be reached
     * @return the connected phone; empty when no address was given, or when it failed
     */
    static Optional<PhoneConnection> open(String streamUrl, Path segmentFolder, int bufferSeconds,
                                          PendingClipWrites pendingWrites, Consumer<String> onFailure) {
        if (streamUrl == null || streamUrl.isBlank()) {
            return Optional.empty();
        }
        FfmpegRecorder recorder;
        try {
            recorder = new FfmpegRecorder(streamUrl, segmentFolder);
            recorder.start();
        } catch (IOException | IllegalArgumentException cause) {
            onFailure.accept("The phone stream could not start (" + cause.getMessage()
                    + "). Opening with the simulated cameras only.");
            return Optional.empty();
        }
        SegmentIndex index = new SegmentIndex(FfmpegRecorder.RING_SEGMENTS);
        StreamCamera camera = new StreamCamera("phone-1", "Phone", Resolution.HD, bufferSeconds,
                streamUrl, pendingWrites.deferring(new FfmpegClipAssembler(index)));
        return Optional.of(new PhoneConnection(streamUrl, recorder, index, camera));
    }

    StreamCamera camera() {
        return camera;
    }

    String streamUrl() {
        return streamUrl;
    }

    /**
     * Builds the feed that moves the phone's new segments into its camera. Call it once:
     * every feed reads the same segment list on its own, so a second one would hand the
     * camera each segment twice.
     *
     * @param onSilence run when the phone's segments stop arriving
     * @return the feed that moves the phone's new segments into its camera
     */
    Feed feed(Runnable onSilence) {
        return new SegmentFeed(camera, new SegmentLog(recorder.segmentList()), index, onSilence);
    }

    /**
     * @param action run, on FFmpeg's own thread, once the recorder exits for any reason
     *     other than being stopped
     */
    void onDrop(Runnable action) {
        recorder.onExit(action);
    }
}
