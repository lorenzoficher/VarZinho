package br.edu.unipampa.varzinho.domain.capture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import br.edu.unipampa.varzinho.domain.highlight.VideoClip;
import br.edu.unipampa.varzinho.enums.Resolution;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class PtzCameraTest {

    @Test
    void producesAClipCarryingItsResolutionAndTheSecondsAskedFor() {
        PtzCamera camera = recordingCamera(30);
        feed(camera, 10);

        VideoClip clip = camera.captureLastSeconds(5);

        assertEquals(Resolution.ULTRA_HD, clip.getResolution());
        assertEquals(5, clip.getDurationSeconds());
    }

    @Test
    void takesTheAimItIsMovedTo() {
        PtzCamera camera = recordingCamera(30);

        camera.moveTo(180, -45, 4);

        assertEquals(180, camera.getPan());
        assertEquals(-45, camera.getTilt());
        assertEquals(4, camera.getZoom());
    }

    @Test
    void rejectsAPanBeyondAFullTurn() {
        PtzCamera camera = recordingCamera(30);

        assertThrows(IllegalArgumentException.class, () -> camera.moveTo(360, 0, 1));
    }

    @Test
    void rejectsATiltPastTheVertical() {
        PtzCamera camera = recordingCamera(30);

        assertThrows(IllegalArgumentException.class, () -> camera.moveTo(0, -91, 1));
    }

    @Test
    void rejectsAZoomBelowOne() {
        PtzCamera camera = recordingCamera(30);

        assertThrows(IllegalArgumentException.class, () -> camera.moveTo(0, 0, 0));
    }

    @Test
    void keepsItsPreviousAimWhenAMoveIsRejected() {
        PtzCamera camera = recordingCamera(30);
        camera.moveTo(90, 10, 2);

        assertThrows(IllegalArgumentException.class, () -> camera.moveTo(90, 10, 99));
        assertEquals(2, camera.getZoom());
    }

    private static PtzCamera recordingCamera(int bufferSeconds) {
        PtzCamera camera = new PtzCamera("CAM-2", "Domo", Resolution.ULTRA_HD, bufferSeconds);
        camera.startRecording();
        return camera;
    }

    private static void feed(Camera camera, int seconds) {
        for (int sequence = 1; sequence <= seconds; sequence++) {
            camera.record(new Frame(Instant.ofEpochSecond(sequence), sequence));
        }
    }
}
