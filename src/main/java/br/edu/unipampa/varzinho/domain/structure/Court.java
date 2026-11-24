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
    /**
     * How many seconds of footage one press of the button keeps. The button is pressed
     * after the play is over, so the clip has to reach back far enough to hold it.
     */
    public static final int DEFAULT_CAPTURE_SECONDS = 30;

    private final int number;
    private final Clock clock;
    private final Supplier<String> highlightIds;
    private final List<Camera> cameras = new ArrayList<>();

    /**
     * Builds a court that stamps its highlights with the machine's clock and random ids.
     *
     * @param number the court's number in its gym, starting at 1
     * @throws IllegalArgumentException if the number is not positive
     */
    public Court(int number) {
        this(number, Clock.systemUTC(), () -> "h-" + UUID.randomUUID());
    }

    /**
     * Builds a court that stamps its highlights with the given clock and ids, so the
     * moment and identity of a capture can be chosen rather than taken from the machine.
     *
     * @param number the court's number in its gym, starting at 1
     * @param clock where a capture reads its moment from
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
     * Mounts a camera on this court. The court checks only its own cameras; uniqueness
     * across the gym is {@link Gym#installCamera(int, Camera)}'s job.
     *
     * @param camera the camera to mount
     * @throws IllegalArgumentException if the camera is null
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

    /**
     * Takes a camera off this court. The camera keeps its state and its buffer.
     *
     * @param camera the camera to take off
     * @throws IllegalArgumentException if the camera is not installed on this court
     */
    public void removeCamera(Camera camera) {
        if (camera == null || !cameras.remove(camera)) {
            throw new IllegalArgumentException("the camera is not installed on court " + number);
        }
    }

    /**
     * Passes one second of footage to every camera on the court. Each camera decides for
     * itself whether to keep it; one that is off drops it.
     *
     * @param frame the second that just went by
     */
    public void record(Frame frame) {
        cameras.forEach(camera -> camera.record(frame));
    }

    /**
     * Presses the court's button: the first camera that is recording freezes its last
     * {@value #DEFAULT_CAPTURE_SECONDS} seconds into a clip, and the court stamps it as a
     * highlight. The court does not keep the highlight; storing it is the caller's job.
     *
     * @return the new highlight, with no name yet
     * @throws NoActiveCameraException if no camera on the court is recording
     * @throws br.edu.unipampa.varzinho.exception.EmptyBufferException if that camera has
     *         not yet recorded {@value #DEFAULT_CAPTURE_SECONDS} seconds
     * @throws br.edu.unipampa.varzinho.exception.ClipAssemblyException if a stream
     *         camera could not write the clip file
     */
    public Highlight triggerCapture() {
        Camera camera = cameras.stream().filter(Camera::isRecording).findFirst()
                .orElseThrow(() -> new NoActiveCameraException(
                        "Court " + number + " has no active camera."));
        VideoClip clip = camera.captureLastSeconds(DEFAULT_CAPTURE_SECONDS);
        return new Highlight(highlightIds.get(), Instant.now(clock), number, camera.getId(), clip);
    }

    /**
     * Tells whether this court holds a camera with the given id.
     *
     * @param cameraId the id to look for
     * @return whether a camera with that id is installed on this court
     */
    public boolean hasCamera(String cameraId) {
        return cameras.stream().anyMatch(camera -> camera.getId().equals(cameraId));
    }

    /**
     * {@return how many seconds a trigger could draw on right now: what the camera a
     * capture would use has recorded, or 0 when no camera is recording}
     */
    public int secondsRecorded() {
        return cameras.stream().filter(Camera::isRecording).findFirst()
                .map(Camera::getSecondsRecorded).orElse(0);
    }

    /** {@return whether a camera on the court is recording, so a trigger has one to use} */
    public boolean hasActiveCamera() {
        return cameras.stream().anyMatch(Camera::isRecording);
    }

    /** {@return the court's number in its gym, always positive} */
    public int getNumber() { return number; }

    /** {@return an unmodifiable copy of the cameras on the court, in installation order} */
    public List<Camera> getCameras() { return List.copyOf(cameras); }
}
