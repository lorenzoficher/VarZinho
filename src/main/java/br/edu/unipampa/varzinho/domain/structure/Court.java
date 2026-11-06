package br.edu.unipampa.varzinho.domain.structure;

import br.edu.unipampa.varzinho.domain.capture.Camera;
import br.edu.unipampa.varzinho.domain.capture.Frame;
import br.edu.unipampa.varzinho.domain.highlight.Highlight;
import br.edu.unipampa.varzinho.domain.highlight.VideoClip;
import br.edu.unipampa.varzinho.exception.DuplicateCameraException;
import br.edu.unipampa.varzinho.exception.NoActiveCameraException;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * A playing area of the gym, with the cameras mounted on it and the button that turns
 * their recent footage into a highlight.
 *
 * <p>The court decides which camera a capture uses; it does not keep the highlights it
 * produces, and it knows nothing of the sport played on it.
 */
public final class Court {
    public static final int DEFAULT_CAPTURE_SECONDS = 30;

    private final int number;
    private final Clock clock;
    private final Supplier<String> highlightIds;
    private final List<Camera> cameras = new ArrayList<>();

    public Court(int number) {
        this(number, Clock.systemUTC(), () -> "h-" + UUID.randomUUID());
    }

    /**
     * Builds a court that stamps its highlights with the given clock and ids, so the
     * moment and identity of a capture can be chosen rather than taken from the machine.
     *
     * @param highlightIds hands out one id per capture, in the order captures happen
     * @throws IllegalArgumentException if the number is not positive or a source is null
     */
    public Court(int number, Clock clock, Supplier<String> highlightIds) {
        if (number <= 0) throw new IllegalArgumentException("a court number must be positive");
        if (clock == null) throw new IllegalArgumentException("a court needs a clock");
        if (highlightIds == null) throw new IllegalArgumentException("a court needs a source of highlight ids");
        this.number = number;
        this.clock = clock;
        this.highlightIds = highlightIds;
    }

    /**
     * Mounts a camera on this court.
     *
     * @throws DuplicateCameraException if a camera with the same id is already here
     */
    public void installCamera(Camera camera) {
        if (camera == null) throw new IllegalArgumentException("a court cannot install a null camera");
        if (hasCamera(camera.getId())) {
            throw new DuplicateCameraException(
                    "camera " + camera.getId() + " is already installed on court " + number);
        }
        cameras.add(camera);
    }

    public void removeCamera(Camera camera) {
        if (camera == null || !cameras.remove(camera)) {
            throw new IllegalArgumentException("the camera is not installed on court " + number);
        }
    }

    public void record(Frame frame) {
        cameras.forEach(camera -> camera.record(frame));
    }

    public Highlight triggerCapture() {
        Camera camera = cameras.stream().filter(Camera::isRecording).findFirst()
                .orElseThrow(() -> new NoActiveCameraException(
                        "Court " + number + " has no active camera."));
        VideoClip clip = camera.captureLastSeconds(DEFAULT_CAPTURE_SECONDS);
        return new Highlight(highlightIds.get(), Instant.now(clock), number, camera.getId(), clip);
    }

    public boolean hasCamera(String cameraId) {
        return cameras.stream().anyMatch(camera -> camera.getId().equals(cameraId));
    }

    /**
     * @return how many seconds a trigger could draw on right now: what the camera a
     *     capture would use has recorded, or 0 when no camera is recording
     */
    public int secondsRecorded() {
        return cameras.stream().filter(Camera::isRecording).findFirst()
                .map(Camera::getSecondsRecorded).orElse(0);
    }

    public boolean hasActiveCamera() {
        return cameras.stream().anyMatch(Camera::isRecording);
    }

    public int getNumber() { return number; }
    public List<Camera> getCameras() { return List.copyOf(cameras); }
}
