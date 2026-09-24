package br.edu.unipampa.varzinho.domain.structure;

import br.edu.unipampa.varzinho.domain.capture.FixedCamera;
import br.edu.unipampa.varzinho.domain.capture.Frame;
import br.edu.unipampa.varzinho.enums.Resolution;
import br.edu.unipampa.varzinho.exception.EmptyBufferException;
import br.edu.unipampa.varzinho.exception.NoActiveCameraException;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CourtTest {
    @Test
    void rejectsCaptureWhenThereIsNoActiveCamera() {
        Court court = new Court(1);
        court.installCamera(new FixedCamera("cam-1", "model", Resolution.HD, 30, 45));

        assertThrows(NoActiveCameraException.class, court::triggerCapture);
    }

    @Test
    void letsTheCameraReportAnEmptyBuffer() {
        Court court = new Court(1);
        FixedCamera camera = new FixedCamera("cam-1", "model", Resolution.HD, 30, 45);
        court.installCamera(camera);
        camera.startRecording();

        assertThrows(EmptyBufferException.class, court::triggerCapture);
    }

    @Test
    void capturesTheConfiguredRecentWindow() {
        Court court = new Court(3);
        FixedCamera camera = new FixedCamera("cam-1", "model", Resolution.HD, 30, 45);
        court.installCamera(camera);
        camera.startRecording();
        for (int second = 0; second < Court.DEFAULT_CAPTURE_SECONDS; second++) {
            court.record(new Frame(Instant.EPOCH.plusSeconds(second), second));
        }

        var highlight = court.triggerCapture();

        assertEquals(3, highlight.getCourtNumber());
        assertEquals("cam-1", highlight.getCameraId());
        assertEquals(Court.DEFAULT_CAPTURE_SECONDS, highlight.getClip().getDurationSeconds());
    }
}
