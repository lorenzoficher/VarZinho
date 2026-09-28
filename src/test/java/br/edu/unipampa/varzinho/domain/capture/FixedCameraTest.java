package br.edu.unipampa.varzinho.domain.capture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import br.edu.unipampa.varzinho.domain.highlight.VideoClip;
import br.edu.unipampa.varzinho.enums.Resolution;
import br.edu.unipampa.varzinho.exception.EmptyBufferException;

import java.time.Instant;
import java.util.Set;

import org.junit.jupiter.api.Test;

class FixedCameraTest {

    @Test
    void producesAClipCarryingItsResolutionAndTheSecondsAskedFor() {
        FixedCamera camera = recordingCamera(30);
        feed(camera, 10);

        VideoClip clip = camera.captureLastSeconds(5);

        assertEquals(Resolution.FULL_HD, clip.getResolution());
        assertEquals(5, clip.getDurationSeconds());
    }

    @Test
    void refusesToCaptureOnceRecordingHasStopped() {
        FixedCamera camera = recordingCamera(30);
        feed(camera, 10);
        camera.stopRecording();

        assertThrows(EmptyBufferException.class, () -> camera.captureLastSeconds(5));
    }

    @Test
    void describesItselfWithoutMentioningAnAimItCannotChange() {
        FixedCamera camera = recordingCamera(30);

        String line = camera.describe().toLowerCase();

        assertFalse(line.contains("pan"));
        assertFalse(line.contains("tilt"));
        assertFalse(line.contains("zoom"));
    }

    @Test
    void describesADifferentLineForEachStatus() {
        FixedCamera camera = new FixedCamera("CAM-1", "Generic", Resolution.FULL_HD, 30, 45);
        String inactive = camera.describe();
        camera.startRecording();
        String active = camera.describe();
        camera.sendToMaintenance();
        String maintenance = camera.describe();

        assertEquals(3, Set.of(inactive, active, maintenance).size());
    }

    @Test
    void describesItselfDifferentlyOnceRecordingStops() {
        FixedCamera camera = recordingCamera(30);
        String recording = camera.describe();

        camera.stopRecording();

        assertNotEquals(recording, camera.describe());
    }

    private static FixedCamera recordingCamera(int bufferSeconds) {
        FixedCamera camera = new FixedCamera("CAM-1", "Generic", Resolution.FULL_HD, bufferSeconds, 45);
        camera.startRecording();
        return camera;
    }

    private static void feed(Camera camera, int seconds) {
        for (int sequence = 1; sequence <= seconds; sequence++) {
            camera.record(new Frame(Instant.ofEpochSecond(sequence), sequence));
        }
    }
}
