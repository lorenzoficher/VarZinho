package br.edu.unipampa.varzinho.domain.capture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import br.edu.unipampa.varzinho.domain.highlight.VideoClip;
import br.edu.unipampa.varzinho.enums.Resolution;
import br.edu.unipampa.varzinho.exception.ClipAssemblyException;
import br.edu.unipampa.varzinho.exception.EmptyBufferException;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

class StreamCameraTest {

    private static final String URL = "http://192.168.0.42:8080/video";

    @Test
    void handsTheRecordedWindowToTheAssembler() {
        RecordingAssembler assembler = new RecordingAssembler();
        StreamCamera camera = recordingCamera(assembler);
        feed(camera, 40);

        camera.captureLastSeconds(30);

        assertEquals(30, assembler.window.size());
        assertEquals(11, assembler.window.get(0).getSequence());
        assertEquals(40, assembler.window.get(29).getSequence());
    }

    @Test
    void returnsAClipAtThePathItAskedTheAssemblerToWrite() {
        RecordingAssembler assembler = new RecordingAssembler();
        StreamCamera camera = recordingCamera(assembler);
        feed(camera, 30);

        VideoClip clip = camera.captureLastSeconds(30);

        assertEquals(Path.of(clip.getFilePath()), assembler.target);
    }

    @Test
    void propagatesAssemblyFailureToTheCaller() {
        ClipAssemblyException failure =
                new ClipAssemblyException("ffmpeg exited with 1", new IOException("broken pipe"));
        StreamCamera camera = recordingCamera((window, target) -> { throw failure; });
        feed(camera, 30);

        ClipAssemblyException thrown =
                assertThrows(ClipAssemblyException.class, () -> camera.captureLastSeconds(30));

        assertSame(failure, thrown);
    }

    @Test
    void refusesToCaptureWhenNotRecordingWithoutCallingTheAssembler() {
        RecordingAssembler assembler = new RecordingAssembler();
        StreamCamera camera = recordingCamera(assembler);
        feed(camera, 30);
        camera.stopRecording();

        assertThrows(EmptyBufferException.class, () -> camera.captureLastSeconds(30));
        assertNull(assembler.window);
    }

    @Test
    void refusesABlankStreamUrl() {
        assertThrows(IllegalArgumentException.class,
                () -> new StreamCamera("phone-1", "Phone", Resolution.HD, 30, " ", new RecordingAssembler()));
    }

    @Test
    void refusesToExistWithoutAnAssembler() {
        assertThrows(IllegalArgumentException.class,
                () -> new StreamCamera("phone-1", "Phone", Resolution.HD, 30, URL, null));
    }

    private static StreamCamera recordingCamera(ClipAssembler assembler) {
        StreamCamera camera = new StreamCamera("phone-1", "Phone", Resolution.HD, 30, URL, assembler);
        camera.startRecording();
        return camera;
    }

    private static void feed(Camera camera, int seconds) {
        for (int sequence = 1; sequence <= seconds; sequence++) {
            camera.record(new Frame(Instant.ofEpochSecond(sequence), sequence));
        }
    }

    private static final class RecordingAssembler implements ClipAssembler {

        private List<Frame> window;
        private Path target;

        @Override
        public void assemble(List<Frame> window, Path target) {
            this.window = window;
            this.target = target;
        }
    }
}
