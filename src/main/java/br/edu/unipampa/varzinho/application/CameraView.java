package br.edu.unipampa.varzinho.application;

import br.edu.unipampa.varzinho.domain.capture.Aimable;
import br.edu.unipampa.varzinho.domain.capture.Camera;
import br.edu.unipampa.varzinho.domain.capture.PtzCamera;
import br.edu.unipampa.varzinho.enums.CameraStatus;
import br.edu.unipampa.varzinho.enums.Resolution;

import java.util.OptionalInt;

/** The immutable camera data an interface may display. */
public final class CameraView {

    private final String id;
    private final String model;
    private final String description;
    private final boolean aimable;
    private final CameraKind kind;
    private final Resolution resolution;
    private final CameraStatus status;
    private final OptionalInt pan;
    private final OptionalInt tilt;
    private final OptionalInt zoom;

    private CameraView(Camera camera) {
        this.id = camera.getId();
        this.description = camera.describe();
        this.aimable = camera instanceof Aimable;
        this.model = camera.getModel();
        this.kind = CameraKind.from(camera);
        this.resolution = camera.getResolution();
        this.status = camera.getStatus();
        if (camera instanceof PtzCamera ptz) {
            this.pan = OptionalInt.of(ptz.getPan());
            this.tilt = OptionalInt.of(ptz.getTilt());
            this.zoom = OptionalInt.of(ptz.getZoom());
        } else {
            this.pan = OptionalInt.empty();
            this.tilt = OptionalInt.empty();
            this.zoom = OptionalInt.empty();
        }
    }

    static CameraView from(Camera camera) {
        return new CameraView(camera);
    }

    public String getId() {
        return id;
    }

    public String getModel() { return model; }
    public boolean isRecording() { return status.canRecord(); }
    public boolean isUnderMaintenance() {
        return status == CameraStatus.MAINTENANCE;
    }

    public String getDescription() {
        return description;
    }

    public boolean isAimable() {
        return aimable;
    }

    public CameraKind getKind() {
        return kind;
    }

    public Resolution getResolution() {
        return resolution;
    }

    public CameraStatus getStatus() {
        return status;
    }

    /** @return the PTZ pan, or empty for a camera without PTZ coordinates */
    public OptionalInt getPan() {
        return pan;
    }

    /** @return the PTZ tilt, or empty for a camera without PTZ coordinates */
    public OptionalInt getTilt() {
        return tilt;
    }

    /** @return the PTZ zoom, or empty for a camera without PTZ coordinates */
    public OptionalInt getZoom() {
        return zoom;
    }
}
