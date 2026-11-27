package br.edu.unipampa.varzinho.application;

import br.edu.unipampa.varzinho.domain.capture.Aimable;
import br.edu.unipampa.varzinho.domain.capture.Camera;
import br.edu.unipampa.varzinho.domain.capture.PtzCamera;
import br.edu.unipampa.varzinho.enums.CameraStatus;
import br.edu.unipampa.varzinho.enums.Resolution;

import java.util.OptionalInt;

/**
 * The immutable camera data an interface may display: a snapshot taken when the view was
 * built, which does not follow later changes to the camera.
 */
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

    /** {@return the camera's identifier, unique in the gym} */
    public String getId() {
        return id;
    }

    /** {@return the camera's model, as entered when it was installed} */
    public String getModel() { return model; }

    /** {@return whether the camera was recording when the snapshot was taken} */
    public boolean isRecording() { return status.canRecord(); }

    /** {@return whether the camera was out of service for maintenance} */
    public boolean isUnderMaintenance() {
        return status == CameraStatus.MAINTENANCE;
    }

    /** {@return a one-line description of the camera for display} */
    public String getDescription() {
        return description;
    }

    /** {@return whether the camera can be aimed with pan, tilt and zoom} */
    public boolean isAimable() {
        return aimable;
    }

    /** {@return the kind of camera} */
    public CameraKind getKind() {
        return kind;
    }

    /** {@return the resolution the camera records at} */
    public Resolution getResolution() {
        return resolution;
    }

    /** {@return the camera's state when the snapshot was taken} */
    public CameraStatus getStatus() {
        return status;
    }

    /** {@return the PTZ pan, or empty for a camera without PTZ coordinates} */
    public OptionalInt getPan() {
        return pan;
    }

    /** {@return the PTZ tilt, or empty for a camera without PTZ coordinates} */
    public OptionalInt getTilt() {
        return tilt;
    }

    /** {@return the PTZ zoom, or empty for a camera without PTZ coordinates} */
    public OptionalInt getZoom() {
        return zoom;
    }
}
