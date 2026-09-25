package br.edu.unipampa.varzinho.domain.structure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import br.edu.unipampa.varzinho.domain.capture.FixedCamera;
import br.edu.unipampa.varzinho.domain.capture.Frame;
import br.edu.unipampa.varzinho.domain.capture.PtzCamera;
import br.edu.unipampa.varzinho.enums.Resolution;
import br.edu.unipampa.varzinho.exception.EmptyBufferException;
import br.edu.unipampa.varzinho.exception.NoActiveCameraException;

class CourtTest {

    @Test
    void createCourt() {
        Court court = new Court(1);
        assertEquals(1, court.getNumber());
    }

    @Test
    void rejectInvalidNumber() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new Court(0)
        );

        assertThrows(
            IllegalArgumentException.class,
            () -> new Court(-1)
        );
    }

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

    @Test
    void rejectsCaptureWhenThereIsNoActiveCamera() {
        Court court = new Court(1);
        court.installCamera(new FixedCamera("fixed-1", "Fixed", Resolution.HD, 30, 45));

        assertThrows(NoActiveCameraException.class, court::triggerCapture);
    }

    @Test
    void letsTheCameraReportAnEmptyBuffer() {
        Court court = new Court(1);
        FixedCamera camera = new FixedCamera("fixed-1", "Fixed", Resolution.HD, 30, 45);
        court.installCamera(camera);
        camera.startRecording();

        assertThrows(EmptyBufferException.class, court::triggerCapture);
    }

    @Test
    void capturesTheConfiguredRecentWindow() {
        Court court = recordingCourtWithBufferedFrames(3);

        var highlight = court.triggerCapture();

        assertEquals(3, highlight.getCourtNumber());
        assertEquals("fixed-1", highlight.getCameraId());
        assertEquals(Court.DEFAULT_CAPTURE_SECONDS, highlight.getClip().getDurationSeconds());
    }

    @Test
    void createsADifferentIdentifierForEveryCapture() {
        Court court = recordingCourtWithBufferedFrames(1);

        var first = court.triggerCapture();
        var second = court.triggerCapture();

        assertNotEquals(first.getId(), second.getId());
    }

    private Court recordingCourtWithBufferedFrames(int number) {
        Court court = new Court(number);
        FixedCamera camera = new FixedCamera("fixed-1", "Fixed", Resolution.HD, 30, 45);
        court.installCamera(camera);
        camera.startRecording();
        for (int second = 0; second < Court.DEFAULT_CAPTURE_SECONDS; second++) {
            court.record(new Frame(Instant.EPOCH.plusSeconds(second), second));
        }
        return court;
    }
}
