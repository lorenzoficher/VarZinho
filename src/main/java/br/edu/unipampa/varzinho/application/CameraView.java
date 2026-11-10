package br.edu.unipampa.varzinho.application;

import br.edu.unipampa.varzinho.domain.capture.Aimable;
import br.edu.unipampa.varzinho.domain.capture.Camera;

/** The immutable camera data an interface may display. */
public final class CameraView {

    private final String id;
    private final String description;
    private final boolean aimable;

    private CameraView(String id, String description, boolean aimable) {
        this.id = id;
        this.description = description;
        this.aimable = aimable;
    }

    static CameraView from(Camera camera) {
        return new CameraView(camera.getId(), camera.describe(), camera instanceof Aimable);
    }

    public String getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public boolean isAimable() {
        return aimable;
    }
}
