package br.edu.unipampa.varzinho.domain.capture;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.edu.unipampa.varzinho.enums.Resolution;

import org.junit.jupiter.api.Test;

class AimableTest {

    @Test
    void aPtzCameraCanBeAimed() {
        Camera camera = new PtzCamera("ptz-1", "Domo", Resolution.HD, 30);

        assertTrue(camera instanceof Aimable);
    }

    @Test
    void aFixedCameraIsNeverAskedToAim() {
        Camera camera = new FixedCamera("fixed-1", "Generic", Resolution.HD, 30, 45);

        assertFalse(camera instanceof Aimable);
    }

    @Test
    void aStreamCameraIsNeverAskedToAim() {
        Camera camera = new StreamCamera("phone-1", "Phone", Resolution.HD, 30,
                "http://192.168.0.42:8080/video", (window, target) -> { });

        assertFalse(camera instanceof Aimable);
    }
}
