package br.edu.unipampa.varzinho.infrastructure.persistence;

import java.util.List;

/** The formats used when the caller does not provide its own registry. */
public final class CameraCsvFormats {
    private CameraCsvFormats() { }

    /**
     * The formats for every camera class the domain ships with.
     *
     * @return the fixed and PTZ formats, in an unmodifiable list
     */
    public static List<CameraCsvFormat<?>> standard() {
        return List.of(new FixedCameraCsvFormat(), new PtzCameraCsvFormat());
    }
}
