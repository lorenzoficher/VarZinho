package br.edu.unipampa.varzinho.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.edu.unipampa.varzinho.domain.capture.Camera;
import br.edu.unipampa.varzinho.domain.capture.ClipAssembler;
import br.edu.unipampa.varzinho.domain.capture.FixedCamera;
import br.edu.unipampa.varzinho.domain.capture.Frame;
import br.edu.unipampa.varzinho.domain.capture.StreamCamera;
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

class CaptureHighlightTest {

    private static final int COURT = 3;
    private static final Instant PRESSED_AT = Instant.parse("2026-11-09T20:14:33Z");

    private final InMemoryHighlightRepository repository = new InMemoryHighlightRepository();
    private final PendingClipWrites pendingWrites = new PendingClipWrites();
    private final List<Path> written = new ArrayList<>();
    private final Gym gym = new Gym("Arena", "Rua A, 10");
    private final Court court =
            new Court(COURT, Clock.fixed(PRESSED_AT, ZoneOffset.UTC), () -> "highlight-1");
    private final CaptureHighlight captureHighlight =
            new CaptureHighlight(gym, repository, pendingWrites);

    @BeforeEach
    void openTheCourt() {
        gym.addCourt(court);
    }

    @Test
    void fixedCameraCaptureArchivesWithoutAWrite() throws Exception {
        recording(fixedCamera());

        captureHighlight.capture(COURT).archive();

        assertEquals(1, repository.findAll().size());
    }

    @Test
    void writingTheClipOfACameraThatWritesNoFileChangesNothing() throws Exception {
        recording(fixedCamera());
        PendingCapture capture = captureHighlight.capture(COURT);

        capture.writeClip();
        capture.archive();

        assertEquals(1, repository.findAll().size());
        assertTrue(written.isEmpty());
    }

    @Test
    void streamCaptureCannotBeArchivedBeforeItsClipIsWritten() throws Exception {
        recording(streamCamera((window, target) -> written.add(target)));

        PendingCapture capture = captureHighlight.capture(COURT);

        assertThrows(IllegalStateException.class, capture::archive);
        assertTrue(repository.findAll().isEmpty());
        assertTrue(written.isEmpty());
    }

    @Test
    void streamCaptureIsArchivedOnceItsClipIsWritten() throws Exception {
        recording(streamCamera((window, target) -> written.add(target)));
        PendingCapture capture = captureHighlight.capture(COURT);

        capture.writeClip();
        HighlightView archived = capture.archive();

        assertEquals(List.of(Path.of(archived.getClipPath())), written);
        assertEquals(1, repository.findAll().size());
    }

    @Test
    void failedWriteLeavesNothingArchived() throws Exception {
        ClipAssemblyException failure = new ClipAssemblyException("ffmpeg exited with 1");
        recording(streamCamera((window, target) -> { throw failure; }));
        PendingCapture capture = captureHighlight.capture(COURT);

        assertThrows(ClipAssemblyException.class, capture::writeClip);

        assertThrows(IllegalStateException.class, capture::archive);
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

    private StreamCamera streamCamera(ClipAssembler writer) {
        return new StreamCamera("phone-1", "Phone", Resolution.HD, 30,
                "http://192.168.0.42:8080/video", pendingWrites.deferring(writer));
    }
}
