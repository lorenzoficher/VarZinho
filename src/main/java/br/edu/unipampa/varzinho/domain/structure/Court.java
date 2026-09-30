package br.edu.unipampa.varzinho.domain.structure;

import br.edu.unipampa.varzinho.domain.capture.Camera;
import br.edu.unipampa.varzinho.domain.capture.Frame;
import br.edu.unipampa.varzinho.domain.highlight.Highlight;
import br.edu.unipampa.varzinho.domain.highlight.VideoClip;
import br.edu.unipampa.varzinho.exception.DuplicateCameraException;
import br.edu.unipampa.varzinho.exception.NoActiveCameraException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class Court {
    public static final int DEFAULT_CAPTURE_SECONDS = 5;

    private final int number;
    private final List<Camera> cameras = new ArrayList<>();

    public Court(int number) {
        if (number <= 0) throw new IllegalArgumentException("a court number must be positive");
        this.number = number;
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
        return new Highlight("h-" + UUID.randomUUID(), Instant.now(), number, camera.getId(), clip);
    }

    public boolean hasCamera(String cameraId) {
        return cameras.stream().anyMatch(camera -> camera.getId().equals(cameraId));
    }

    public boolean hasActiveCamera() {
        return cameras.stream().anyMatch(Camera::isRecording);
    }

    public int getNumber() { return number; }
    public List<Camera> getCameras() { return List.copyOf(cameras); }
}
