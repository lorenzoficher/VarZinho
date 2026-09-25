package br.edu.unipampa.varzinho.domain.structure;

import br.edu.unipampa.varzinho.domain.capture.FixedCamera;
import br.edu.unipampa.varzinho.domain.capture.PtzCamera;
import br.edu.unipampa.varzinho.enums.Resolution;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CourtTest {
    @Test
    void installsEachCameraOnlyOnceAndRemovesInstalledEquipment() {
        Court court = new Court(1);
        FixedCamera camera = new FixedCamera("fixed-1", "Fixed", Resolution.HD, 30, 45);

        court.installCamera(camera);
        court.installCamera(camera);

        assertEquals(1, court.getCameras().size());
        assertThrows(UnsupportedOperationException.class, () -> court.getCameras().clear());
        court.removeCamera(camera);
        assertTrue(court.getCameras().isEmpty());
        assertThrows(IllegalArgumentException.class, () -> court.removeCamera(camera));
    }

    @Test
    void reportsAnActiveCameraOnlyWhileOneIsRecording() {
        Court court = new Court(1);
        FixedCamera inactive = new FixedCamera("fixed-1", "Fixed", Resolution.HD, 30, 45);
        PtzCamera active = new PtzCamera("ptz-1", "PTZ", Resolution.HD, 30);
        court.installCamera(inactive);
        court.installCamera(active);

        assertFalse(court.hasActiveCamera());
        inactive.sendToMaintenance();
        assertFalse(court.hasActiveCamera());
        active.startRecording();
        assertTrue(court.hasActiveCamera());
    }
}
