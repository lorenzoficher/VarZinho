package br.edu.unipampa.varzinho.domain.capture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import br.edu.unipampa.varzinho.enums.CameraStatus;
import br.edu.unipampa.varzinho.enums.Resolution;
import br.edu.unipampa.varzinho.exception.EmptyBufferException;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

class CameraTest {

    @Test
    void startsInactiveWhenInstalled() {
        Camera camera = camera(30);

        assertEquals(CameraStatus.INACTIVE, camera.getStatus());
    }

    @Test
    void becomesActiveWhenRecordingStarts() {
        Camera camera = camera(30);

        camera.startRecording();

        assertEquals(CameraStatus.ACTIVE, camera.getStatus());
    }

    @Test
    void becomesInactiveWhenRecordingStops() {
        Camera camera = recordingCamera(30);

        camera.stopRecording();

        assertEquals(CameraStatus.INACTIVE, camera.getStatus());
        assertFalse(camera.isRecording());
    }

    @Test
    void refusesToStartRecordingWhileUnderMaintenance() {
        Camera camera = camera(30);
        camera.sendToMaintenance();

        assertThrows(IllegalStateException.class, camera::startRecording);
        assertEquals(CameraStatus.MAINTENANCE, camera.getStatus());
    }

    @Test
    void staysUnderMaintenanceWhenRecordingIsStopped() {
        Camera camera = camera(30);
        camera.sendToMaintenance();

        assertThrows(IllegalStateException.class, camera::stopRecording);
        assertEquals(CameraStatus.MAINTENANCE, camera.getStatus());
    }

    @Test
    void storesTheFramesItIsFedWhileRecording() {
        TestCamera camera = recordingCamera(3);

        camera.record(frame(1));
        camera.record(frame(2));

        assertEquals(List.of(frame(1), frame(2)), camera.window(2));
    }

    @Test
    void dropsTheFramesItIsFedWhileNotRecording() {
        TestCamera camera = camera(3);

        camera.record(frame(1));

        assertThrows(EmptyBufferException.class, () -> camera.window(1));
    }

    @Test
    void dropsTheFramesFedAfterRecordingStopped() {
        TestCamera camera = recordingCamera(3);
        camera.record(frame(1));
        camera.stopRecording();

        camera.record(frame(2));

        assertEquals(List.of(frame(1)), camera.window(1));
    }

    @Test
    void rejectsABlankIdentifier() {
        assertThrows(IllegalArgumentException.class,
                () -> new TestCamera("  ", "Generic", Resolution.HD, 30));
    }

    @Test
    void rejectsAMissingResolution() {
        assertThrows(IllegalArgumentException.class,
                () -> new TestCamera("CAM-1", "Generic", null, 30));
    }

    private static TestCamera camera(int bufferSeconds) {
        return new TestCamera("CAM-1", "Generic", Resolution.HD, bufferSeconds);
    }

    private static TestCamera recordingCamera(int bufferSeconds) {
        TestCamera camera = camera(bufferSeconds);
        camera.startRecording();
        return camera;
    }

    /**
     * One frame per second, numbered from the same value as its instant, so a frame
     * built twice is the same frame and a window can be compared by value.
     */
    private static Frame frame(int sequence) {
        return new Frame(Instant.ofEpochSecond(sequence), sequence);
    }

    /**
     * Stands in for the real subclasses, which arrive with their clip-producing
     * behaviour in their own issue. What is under test here belongs to {@link Camera}.
     */
    private static final class TestCamera extends Camera {

        private TestCamera(String id, String model, Resolution resolution, int bufferSeconds) {
            super(id, model, resolution, bufferSeconds);
        }

        private List<Frame> window(int seconds) {
            return recordedWindow(seconds);
        }
    }
}
