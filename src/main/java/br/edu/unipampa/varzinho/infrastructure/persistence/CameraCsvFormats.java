package br.edu.unipampa.varzinho.infrastructure.persistence;

import java.util.List;

/** The camera kinds the gym file stores today. A new kind is added here, not in the repository. */
public final class CameraCsvFormats {

    private CameraCsvFormats() { }

    public static List<CameraCsvFormat<?>> standard() {
        return List.of(new FixedCameraCsvFormat(), new PtzCameraCsvFormat());
    }
}
