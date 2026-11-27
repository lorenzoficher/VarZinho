package br.edu.unipampa.varzinho.application;

import br.edu.unipampa.varzinho.enums.Resolution;

/**
 * The values entered when a camera is installed through the application.
 *
 * <p>Only the kind is checked here. The rest is checked by the camera built from it, so a
 * specification can exist that no camera accepts; installing it fails instead.
 */
public final class CameraSpec {

    private final String id;
    private final String model;
    private final CameraKind kind;
    private final Resolution resolution;
    private final int bufferSeconds;
    private final int fixedAngle;

    /**
     * Records what was entered for a new camera.
     *
     * @param id the camera's identifier
     * @param model the camera's model
     * @param kind which camera to build; only an {@linkplain CameraKind#installable()
     *     installable} kind
     * @param resolution the resolution it records at
     * @param bufferSeconds how many seconds its buffer holds
     * @param fixedAngle the angle of a fixed camera; ignored for any other kind
     * @throws IllegalArgumentException if the kind is missing or is not installable — a
     *     stream camera is connected with its stream, not installed from a form
     */
    public CameraSpec(String id, String model, CameraKind kind, Resolution resolution,
            int bufferSeconds, int fixedAngle) {
        if (kind == null) {
            throw new IllegalArgumentException("a camera specification needs a kind");
        }
        if (!CameraKind.installable().contains(kind)) {
            throw new IllegalArgumentException("a stream camera must be connected with its stream and assembler");
        }
        this.id = id;
        this.model = model;
        this.kind = kind;
        this.resolution = resolution;
        this.bufferSeconds = bufferSeconds;
        this.fixedAngle = fixedAngle;
    }

    /** {@return the camera's identifier, as entered} */
    public String getId() {
        return id;
    }

    /** {@return the camera's model, as entered} */
    public String getModel() {
        return model;
    }

    /** {@return which camera to build, always an installable kind} */
    public CameraKind getKind() {
        return kind;
    }

    /** {@return the resolution, as entered} */
    public Resolution getResolution() {
        return resolution;
    }

    /** {@return how many seconds the buffer should hold, as entered} */
    public int getBufferSeconds() {
        return bufferSeconds;
    }

    /** {@return the angle of a fixed camera, as entered} */
    public int getFixedAngle() {
        return fixedAngle;
    }
}
