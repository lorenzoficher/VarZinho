package br.edu.unipampa.varzinho.domain.capture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import br.edu.unipampa.varzinho.domain.highlight.VideoClip;
import br.edu.unipampa.varzinho.enums.Resolution;
import br.edu.unipampa.varzinho.exception.EmptyBufferException;

import java.time.Instant;

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
