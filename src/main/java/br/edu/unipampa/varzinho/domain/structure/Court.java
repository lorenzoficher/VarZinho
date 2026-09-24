package br.edu.unipampa.varzinho.domain.structure;

import br.edu.unipampa.varzinho.domain.capture.Camera;
import br.edu.unipampa.varzinho.domain.capture.Frame;
import br.edu.unipampa.varzinho.domain.highlight.Highlight;
import br.edu.unipampa.varzinho.domain.highlight.VideoClip;
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

    public void installCamera(Camera camera) {
        if (camera == null) throw new IllegalArgumentException("a court cannot install a null camera");
        if (cameras.stream().anyMatch(item -> item.getId().equals(camera.getId()))) {
            throw new IllegalArgumentException("camera " + camera.getId() + " is already installed");
        }
        cameras.add(camera);
    }

    public void record(Frame frame) {
        cameras.forEach(camera -> camera.record(frame));
    }

    public Highlight triggerCapture() {
        return triggerCapture(DEFAULT_CAPTURE_SECONDS);
    }

    public Highlight triggerCapture(int seconds) {
        Camera camera = cameras.stream().filter(Camera::isRecording).findFirst()
                .orElseThrow(() -> new NoActiveCameraException(
                        "Court " + number + " has no active camera."));
        VideoClip clip = camera.captureLastSeconds(seconds);
        return new Highlight("h-" + UUID.randomUUID(), Instant.now(), number, camera.getId(), clip);
    }

    public boolean hasActiveCamera() {
        return cameras.stream().anyMatch(Camera::isRecording);
    }

    public int getNumber() { return number; }
    public List<Camera> getCameras() { return List.copyOf(cameras); }
}
