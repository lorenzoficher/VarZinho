package br.edu.unipampa.varzinho.infrastructure.persistence;

import java.util.List;

/** The formats used when the caller does not provide its own registry. */
public final class CameraCsvFormats {
    private CameraCsvFormats() { }

    public static List<CameraCsvFormat<?>> standard() {
        return List.of(new FixedCameraCsvFormat(), new PtzCameraCsvFormat());
    }
}
