package br.edu.unipampa.varzinho.application;

import br.edu.unipampa.varzinho.domain.capture.Camera;
import br.edu.unipampa.varzinho.domain.capture.ClipAssembler;
import br.edu.unipampa.varzinho.domain.capture.FixedCamera;
import br.edu.unipampa.varzinho.domain.capture.Frame;
import br.edu.unipampa.varzinho.domain.capture.StreamCamera;
import br.edu.unipampa.varzinho.domain.highlight.Highlight;
import br.edu.unipampa.varzinho.domain.structure.Court;
import br.edu.unipampa.varzinho.domain.structure.Gym;
import br.edu.unipampa.varzinho.enums.Resolution;
import br.edu.unipampa.varzinho.exception.ClipAssemblyException;
import br.edu.unipampa.varzinho.exception.NoActiveCameraException;
import br.edu.unipampa.varzinho.infrastructure.persistence.InMemoryHighlightRepository;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CaptureHighlightTest {
    private static final int COURT = 3;
    private static final Instant PRESSED_AT = Instant.parse("2026-11-09T20:14:33Z");

    private final InMemoryHighlightRepository repository = new InMemoryHighlightRepository();
    private final List<Path> written = new ArrayList<>();
    private final Gym gym = new Gym("Arena", "Rua A, 10");
    private final Court court =
            new Court(COURT, Clock.fixed(PRESSED_AT, ZoneOffset.UTC), () -> "highlight-1");
    private final CaptureHighlight captureHighlight = new CaptureHighlight(gym, repository);

    @BeforeEach
    void openTheCourt() {
        gym.addCourt(court);
    }

    @Test
    void fixedCameraCaptureCanBeArchivedImmediately() throws Exception {
        recording(fixedCamera());

        captureHighlight.capture(COURT).archive();

        assertEquals(1, repository.findAll().size());
        assertTrue(written.isEmpty());
    }

    @Test
    void streamCaptureCompletesItsWriteBeforeItCanBeArchived() throws Exception {
        recording(streamCamera((window, target) -> {
            assertTrue(repository.findAll().isEmpty());
            written.add(target);
        }));

        PendingCapture capture = captureHighlight.capture(COURT);

        assertEquals(List.of(Path.of(capture.view().getClipPath())), written);
        assertTrue(repository.findAll().isEmpty());
        HighlightView archived = capture.archive();
        assertEquals(capture.view().getClipPath(), archived.getClipPath());
        assertEquals(1, repository.findAll().size());
    }

    @Test
    void aCaptureOutsideThePanelWritesItsClipEvenWhenNobodyArchivesIt() {
        recording(streamCamera((window, target) -> written.add(target)));

        Highlight captured = court.triggerCapture();

        assertEquals(List.of(Path.of(captured.getClip().getFilePath())), written);
        assertTrue(repository.findAll().isEmpty());
    }

    @Test
    void discardingACaptureLeavesNoWriteForTheNextCaptureToCollect() {
        recording(streamCamera((window, target) -> written.add(target)));

        PendingCapture first = captureHighlight.capture(COURT);
        PendingCapture second = captureHighlight.capture(COURT);

        assertEquals(List.of(Path.of(first.view().getClipPath()), Path.of(second.view().getClipPath())), written);
        assertTrue(repository.findAll().isEmpty());
    }

    @Test
    void failedWriteRefusesTheCaptureAndLeavesNothingArchived() {
        ClipAssemblyException failure = new ClipAssemblyException("ffmpeg exited with 1");
        recording(streamCamera((window, target) -> { throw failure; }));

        assertSame(failure, assertThrows(ClipAssemblyException.class, () -> captureHighlight.capture(COURT)));
        assertTrue(repository.findAll().isEmpty());
    }

    @Test
    void archiveReturnsWhatTheArchiveTableShows() throws Exception {
        recording(fixedCamera());

        HighlightView archived = captureHighlight.capture(COURT).archive();

        assertEquals(PRESSED_AT, archived.getCapturedAt());
        assertEquals(COURT, archived.getCourtNumber());
        assertEquals("fixed-1", archived.getCameraId());
        assertEquals(30, archived.getDurationSeconds());
        assertEquals(repository.findAll().get(0).getClip().getFilePath(), archived.getClipPath());
    }

    @Test
    void courtWithNoActiveCameraRefusesTheCapture() {
        court.installCamera(fixedCamera());

        assertThrows(NoActiveCameraException.class, () -> captureHighlight.capture(COURT));
    }

    @Test
    void courtTheGymDoesNotHaveRefusesTheCapture() {
        assertThrows(IllegalArgumentException.class, () -> captureHighlight.capture(99));
    }

    private void recording(Camera camera) {
        court.installCamera(camera);
        camera.startRecording();
        for (int second = 1; second <= 30; second++) {
            camera.record(new Frame(PRESSED_AT.minusSeconds(30L - second), second));
        }
    }

    private static FixedCamera fixedCamera() {
        return new FixedCamera("fixed-1", "Model", Resolution.HD, 30, 90);
    }

    private static StreamCamera streamCamera(ClipAssembler writer) {
        return new StreamCamera("phone-1", "Phone", Resolution.HD, 30,
                "http://192.168.0.42:8080/video", writer);
    }
}
