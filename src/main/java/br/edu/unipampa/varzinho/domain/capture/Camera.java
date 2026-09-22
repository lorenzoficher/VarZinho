package br.edu.unipampa.varzinho.domain.capture;

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

    private final String id;
    private final String model;
    private final Resolution resolution;
    private final CircularBuffer buffer;
    private CameraStatus status;

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
     * @throws IllegalStateException if the camera is under maintenance — equipment on
     *         the bench is not on a court, and nothing but a technician takes it off
     */
    public void startRecording() {
        if (status == CameraStatus.MAINTENANCE) {
            throw new IllegalStateException(
                    "camera " + id + " is under maintenance and cannot record");
        }
        status = CameraStatus.ACTIVE;
    }

    public void sendToMaintenance() {
        status = CameraStatus.MAINTENANCE;
    }

    /**
     * Takes the camera off duty. What it already holds stays in the buffer; it simply
     * stops being fed.
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
     * @param seconds how many of the most recent seconds to read
     * @return the window, oldest first
     * @throws EmptyBufferException if that many seconds have not been recorded yet
     */
    protected final List<Frame> recordedWindow(int seconds) {
        return buffer.extractLastSeconds(seconds);
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
}
