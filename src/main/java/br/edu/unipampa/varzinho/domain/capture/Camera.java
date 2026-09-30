package br.edu.unipampa.varzinho.domain.capture;

import br.edu.unipampa.varzinho.domain.highlight.VideoClip;
import br.edu.unipampa.varzinho.enums.CameraStatus;
import br.edu.unipampa.varzinho.enums.Resolution;
import br.edu.unipampa.varzinho.exception.EmptyBufferException;

import java.util.List;

/**
 * Equipment mounted on a court, recording continuously into its own buffer.
 *
 * <p>Its status decides whether footage is taken at all: a camera that is not
 * recording accepts frames and drops them, so the buffer never holds seconds the
 * camera was switched off for.
 */
public abstract class Camera {

    private static final double MB_PER_MEGAPIXEL_SECOND = 0.5;

    private final String id;
    private final String model;
    private final Resolution resolution;
    private CircularBuffer buffer;
    private CameraStatus status;
    private int clipsCut;

    protected Camera(String id, String model, Resolution resolution, int bufferSeconds) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("a camera needs an identifier");
        }
        if (model == null || model.isBlank()) {
            throw new IllegalArgumentException("a camera needs a model");
        }
        if (resolution == null) {
            throw new IllegalArgumentException("a camera needs a resolution");
        }
        this.id = id;
        this.model = model;
        this.resolution = resolution;
        this.buffer = new CircularBuffer(bufferSeconds);
        this.status = CameraStatus.INACTIVE;
    }

    /**
     * Puts the camera to work, filling its buffer second by second.
     *
     * <p>A camera that was not recording starts on an empty buffer: whatever it held from
     * before belongs to a moment, or a court, it is no longer watching, and a highlight
     * must never pass that off as the present. A camera already recording keeps going.
     *
     * @throws IllegalStateException if the camera is under maintenance — equipment on
     *         the bench is not on a court, and nothing but a technician takes it off
     */
    public void startRecording() {
        if (status == CameraStatus.MAINTENANCE) {
            throw new IllegalStateException(
                    "camera " + id + " is under maintenance and cannot record");
        }
        if (status != CameraStatus.ACTIVE) {
            buffer = new CircularBuffer(buffer.capacity());
        }
        status = CameraStatus.ACTIVE;
    }

    public void sendToMaintenance() {
        status = CameraStatus.MAINTENANCE;
    }

    /**
     * Takes the camera off duty. What it already holds stays in the buffer, unreadable,
     * until it is switched on again and starts afresh.
     *
     * @throws IllegalStateException if the camera is under maintenance — stopping it
     *         would silently report it as merely switched off
     */
    public void stopRecording() {
        if (status == CameraStatus.MAINTENANCE) {
            throw new IllegalStateException(
                    "camera " + id + " is under maintenance and is not recording");
        }
        status = CameraStatus.INACTIVE;
    }

    /**
     * Feeds one second of footage to the camera, which keeps it only while it is
     * recording. A camera that is off drops what it is sent rather than refusing it:
     * the gym goes on producing seconds whatever this camera is doing.
     */
    public void record(Frame frame) {
        if (!status.canRecord()) {
            return;
        }
        buffer.record(frame);
    }

    /**
     * The most recent window the camera holds, for a subclass to turn into a clip.
     *
     * <p>Protected because the buffer is the camera's own memory: a court asks the
     * camera for a capture, never for its frames.
     *
     * <p>The refusal to read from a camera that is off lives here rather than in each
     * subclass: a camera that stops recording keeps whatever it had, and handing that
     * stale window out would pass off the past as the present. A subclass cannot
     * forget a rule it never had to write.
     *
     * @param seconds how many of the most recent seconds to read
     * @return the window, oldest first
     * @throws EmptyBufferException if the camera is not recording, or has not recorded
     *         that many seconds yet
     */
    protected final List<Frame> recordedWindow(int seconds) {
        if (!isRecording()) {
            throw new EmptyBufferException("camera " + id + " is not recording");
        }
        return buffer.extractLastSeconds(seconds);
    }

    /**
     * Freezes the most recent seconds into a clip. Each kind of camera produces one
     * its own way, and a court triggers them all through this single call.
     *
     * @param seconds how much of the recent past to keep
     * @return the clip that stands for those seconds
     * @throws EmptyBufferException if the camera is not recording, or has not recorded
     *         that many seconds yet
     */
    public abstract VideoClip captureLastSeconds(int seconds);

    /**
     * The start of a clip's file name, unique for this camera: the id, the second the
     * window ends on, and how many clips the camera has cut so far. The count is what
     * keeps two triggers ending on the same second from naming the same file.
     *
     * @param last the most recent frame of the window being frozen
     */
    protected final String clipName(Frame last) {
        clipsCut++;
        return "clips/" + id + "-" + last.getTimestamp().getEpochSecond() + "-" + clipsCut;
    }

    /**
     * Builds the clip for a window this camera has already taken, stamping it with
     * what every camera knows about its own footage.
     *
     * <p>The size is an estimate, not a measurement: no file is written and no frame
     * is ever encoded, so megapixels times seconds is as close to the truth as this
     * model gets.
     *
     * @param filePath where the subclass decided the clip belongs
     * @param seconds the length of the window being frozen
     */
    protected final VideoClip clipFrom(String filePath, int seconds) {
        double megapixels = resolution.width() * resolution.height() / 1_000_000.0;
        return new VideoClip(filePath, seconds, resolution, megapixels * seconds * MB_PER_MEGAPIXEL_SECOND);
    }

    /**
     * One line a person reads to tell this camera apart from the others on the court.
     * Each kind says what only it can say, so a caller never has to ask which kind it
     * is holding.
     *
     * @return the camera's current state in one line; it changes as the camera does
     */
    public abstract String describe();

    /**
     * The part of the line every camera shares, in the same order for every kind, so
     * the lines of different cameras still read alike when listed together.
     *
     * @param kind how this kind of camera names itself
     */
    protected final String describeAs(String kind) {
        return id + " · " + kind + " · " + resolution + " · " + status;
    }

    public boolean isRecording() {
        return status.canRecord();
    }

    public CameraStatus getStatus() {
        return status;
    }

    public String getId() {
        return id;
    }

    public String getModel() {
        return model;
    }

    public Resolution getResolution() {
        return resolution;
    }

    public int getBufferSeconds() {
        return buffer.capacity();
    }

    /**
     * @return how many seconds the buffer holds since the camera was last switched on,
     *     never more than its length
     */
    public int getSecondsRecorded() {
        return buffer.size();
    }
}
