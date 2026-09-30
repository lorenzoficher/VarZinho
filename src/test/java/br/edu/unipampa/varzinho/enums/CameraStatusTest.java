package br.edu.unipampa.varzinho.enums;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CameraStatusTest {

    @Test
    void anActiveCameraCanRecord() {
        assertTrue(CameraStatus.ACTIVE.canRecord());
    }

    @Test
    void anInactiveCameraCannotRecord() {
        assertFalse(CameraStatus.INACTIVE.canRecord());
    }

    @Test
    void aCameraInMaintenanceCannotRecord() {
        assertFalse(CameraStatus.MAINTENANCE.canRecord());
    }
}
