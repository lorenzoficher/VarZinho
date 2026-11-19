package br.edu.unipampa.varzinho.application;

import br.edu.unipampa.varzinho.domain.capture.Aimable;
import br.edu.unipampa.varzinho.domain.capture.Camera;

/** The immutable camera data an interface may display. */
public final class CameraView {

    private final String id;
    private final String model;
    private final int pan, tilt, zoom;
    private final br.edu.unipampa.varzinho.enums.CameraStatus status;
    private final String description;
    private final boolean aimable;

    private CameraView(Camera camera) {
        this.id = camera.getId();
        this.description = camera.describe();
        this.aimable = camera instanceof Aimable;
        this.model = camera.getModel();
        if (camera instanceof br.edu.unipampa.varzinho.domain.capture.PtzCamera ptz) {
            pan = ptz.getPan();
            tilt = ptz.getTilt();
            zoom = ptz.getZoom();
        } else {
            pan = tilt = 0;
            zoom = 1;
        }
        this.status = camera.getStatus();
    }

    static CameraView from(Camera camera) {
        return new CameraView(camera);
    }

    public String getId() {
        return id;
    }

    public String getModel() { return model; }
    public int getPan() { return pan; }
    public int getTilt() { return tilt; }
    public int getZoom() { return zoom; }
    public boolean isRecording() { return status.canRecord(); }
    public boolean isUnderMaintenance() {
        return status == br.edu.unipampa.varzinho.enums.CameraStatus.MAINTENANCE;
    }

    public String getDescription() {
        return description;
    }

    public boolean isAimable() {
        return aimable;
    }
}
