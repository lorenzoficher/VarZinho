package br.edu.unipampa.varzinho.application;

import br.edu.unipampa.varzinho.enums.Resolution;

/** The values entered when a camera is installed through the application. */
public final class CameraSpec {

    private final String id;
    private final String model;
    private final CameraKind kind;
    private final Resolution resolution;
    private final int bufferSeconds;
    private final int fixedAngle;

    public CameraSpec(String id, String model, CameraKind kind, Resolution resolution,
            int bufferSeconds, int fixedAngle) {
        if (kind == null) {
            throw new IllegalArgumentException("a camera specification needs a kind");
        }
        this.id = id;
        this.model = model;
        this.kind = kind;
        this.resolution = resolution;
        this.bufferSeconds = bufferSeconds;
        this.fixedAngle = fixedAngle;
    }

    public String getId() {
        return id;
    }

    public String getModel() {
        return model;
    }

    public CameraKind getKind() {
        return kind;
    }

    public Resolution getResolution() {
        return resolution;
    }

    public int getBufferSeconds() {
        return bufferSeconds;
    }

    public int getFixedAngle() {
        return fixedAngle;
    }
}
