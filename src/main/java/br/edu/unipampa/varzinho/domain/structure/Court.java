package br.edu.unipampa.varzinho.domain.structure;

import br.edu.unipampa.varzinho.domain.capture.Camera;

import java.util.ArrayList;
import java.util.List;

public final class Court {
    private final int number;
    private final List<Camera> cameras = new ArrayList<>();

    public Court(int number) {
        if (number <= 0) throw new IllegalArgumentException("a court number must be positive");
        this.number = number;
    }

    public void installCamera(Camera camera) {
        if (camera == null) throw new IllegalArgumentException("a court cannot install a null camera");
        if (cameras.stream().anyMatch(item -> item.getId().equals(camera.getId()))) {
            return;
        }
        cameras.add(camera);
    }

    public void removeCamera(Camera camera) {
        if (camera == null || !cameras.remove(camera)) {
            throw new IllegalArgumentException("the camera is not installed on court " + number);
        }
    }

    public boolean hasActiveCamera() {
        return cameras.stream().anyMatch(Camera::isRecording);
    }

    public int getNumber() { return number; }
    public List<Camera> getCameras() { return List.copyOf(cameras); }
}
