package br.edu.unipampa.varzinho.ui;

import br.edu.unipampa.varzinho.domain.capture.Camera;
import br.edu.unipampa.varzinho.domain.capture.ClipAssembler;
import br.edu.unipampa.varzinho.domain.capture.StreamCamera;
import br.edu.unipampa.varzinho.domain.highlight.Highlight;
import br.edu.unipampa.varzinho.domain.structure.Court;
import br.edu.unipampa.varzinho.domain.structure.Gym;
import br.edu.unipampa.varzinho.enums.Resolution;
import br.edu.unipampa.varzinho.exception.ClipAssemblyException;
import br.edu.unipampa.varzinho.repository.InMemoryHighlightRepository;
import org.junit.jupiter.api.Test;

import javax.swing.SwingUtilities;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CapturePanelTest {
    private static final ZoneId BRASILIA = ZoneId.of("America/Sao_Paulo");
    private static final Instant START = Instant.parse("2026-09-29T20:00:00Z");

    private final Gym gym = SampleGym.build();
    private final InMemoryHighlightRepository repository = new InMemoryHighlightRepository();
    private final List<String> errors = new ArrayList<>();
    private final List<Highlight> saved = new ArrayList<>();
    private final CourtPanel courtPanel = new CourtPanel(gym, 30, errors::add);
    private final PendingClipWrites pending = new PendingClipWrites();
    private final CapturePanel panel =
            new CapturePanel(courtPanel, repository, BRASILIA, pending, errors::add, saved::add);

    @Test
    void onePressSavesExactlyOneHighlightForTheSelectedCourt() {
        fillBuffers();
        courtPanel.selectCourt(2);

        panel.saveButton().doClick();

        assertEquals(1, repository.findAll().size());
        assertEquals(1, repository.findByCourt(2).size());
        assertTrue(errors.isEmpty());
    }

    @Test
    void confirmationNamesCourtCameraLocalTimeAndDuration() {
        fillBuffers();

        panel.saveButton().doClick();

        Highlight highlight = repository.findAll().get(0);
        String localTime = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                .withZone(BRASILIA).format(highlight.getCapturedAt());
        assertEquals("Saved: court 1 · camera fixed-1 · " + localTime + " · 30s", panel.confirmation());
        assertEquals(List.of(highlight), saved);
    }

    @Test
    void buttonLabelFollowsTheSelectedCourt() {
        assertEquals("Save highlight - court 1", panel.saveButton().getText());

        courtPanel.selectCourt(2);

        assertEquals("Save highlight - court 2", panel.saveButton().getText());
    }

    @Test
    void courtWithNoActiveCameraReportsTheDomainMessageAndSavesNothing() {
        fillBuffers();
        courtPanel.selectCourt(2);
        Camera onlyCamera = gym.findCourt(2).getCameras().get(0);
        onlyCamera.stopRecording();

        panel.saveButton().doClick();

        assertEquals(1, errors.size());
        assertTrue(repository.findAll().isEmpty());
        assertTrue(saved.isEmpty());
        assertEquals("", panel.confirmation());
    }

    @Test
    void failedSaveClearsThePreviousConfirmation() {
        fillBuffers();
        courtPanel.selectCourt(2);
        panel.saveButton().doClick();
        gym.findCourt(2).getCameras().get(0).stopRecording();

        panel.saveButton().doClick();

        assertEquals(1, errors.size());
        assertEquals("", panel.confirmation());
    }

    @Test
    void emptyBufferReportsTheDomainMessageAndSavesNothing() {
        panel.saveButton().doClick();

        assertEquals(1, errors.size());
        assertTrue(repository.findAll().isEmpty());
        assertTrue(saved.isEmpty());
    }

    @Test
    void phoneThatCannotWriteItsClipReportsTheFailureAndSavesNothing() {
        CapturePanel phonePanel = phonePanelWritingWith(
                (window, target) -> { throw new ClipAssemblyException("ffmpeg is not installed"); });

        phonePanel.saveButton().doClick();
        awaitSwingThread(() -> !errors.isEmpty());

        assertEquals(List.of("ffmpeg is not installed"), errors);
        assertTrue(repository.findAll().isEmpty());
        assertEquals("", phonePanel.confirmation());
    }

    @Test
    void pressReturnsWhileThePhoneClipIsStillBeingWritten() {
        CountDownLatch release = new CountDownLatch(1);
        CapturePanel phonePanel = phonePanelWritingWith((window, target) -> awaitQuietly(release));

        phonePanel.saveButton().doClick();

        assertTrue(repository.findAll().isEmpty());
        assertTrue(saved.isEmpty());
        assertEquals("Saving: court 3 · camera phone-1…", phonePanel.confirmation());
        release.countDown();
    }

    @Test
    void phoneClipIsArchivedOnceItsFileIsWritten() {
        CountDownLatch release = new CountDownLatch(1);
        CapturePanel phonePanel = phonePanelWritingWith((window, target) -> awaitQuietly(release));
        phonePanel.saveButton().doClick();

        release.countDown();
        awaitSwingThread(() -> !saved.isEmpty());

        assertEquals(1, repository.findByCourt(3).size());
        assertTrue(phonePanel.confirmation().startsWith("Saved: court 3 · camera phone-1 · "));
        assertTrue(errors.isEmpty());
    }

    @Test
    void secondPressWhileTheFirstIsWritingKeepsBothClipsApart() {
        CountDownLatch release = new CountDownLatch(1);
        List<Path> targets = Collections.synchronizedList(new ArrayList<>());
        CapturePanel phonePanel = phonePanelWritingWith((window, target) -> {
            targets.add(target);
            awaitQuietly(release);
        });

        phonePanel.saveButton().doClick();
        phonePanel.saveButton().doClick();
        release.countDown();
        awaitSwingThread(() -> saved.size() == 2);

        assertEquals(2, new HashSet<>(targets).size());
        assertEquals(2, repository.findByCourt(3).size());
        assertTrue(errors.isEmpty());
    }

    private CapturePanel phonePanelWritingWith(ClipAssembler writer) {
        Gym phoneGym = SampleGym.build();
        phoneGym.addCourt(new Court(3));
        StreamCamera phone = new StreamCamera("phone-1", "Phone", Resolution.HD, 30, "http://phone/video",
                pending.deferring(writer));
        phoneGym.installCamera(3, phone);
        phone.startRecording();
        CourtPanel phoneCourts = new CourtPanel(phoneGym, 30, errors::add);
        CapturePanel phonePanel =
                new CapturePanel(phoneCourts, repository, BRASILIA, pending, errors::add, saved::add);
        LiveFeed feed = new LiveFeed(phoneGym);
        for (int second = 0; second < Court.DEFAULT_CAPTURE_SECONDS; second++) {
            feed.tick(START.plusSeconds(second));
        }
        phoneCourts.selectCourt(3);
        return phonePanel;
    }

    private static void awaitQuietly(CountDownLatch release) {
        try {
            assertTrue(release.await(5, TimeUnit.SECONDS), "the test never released the clip");
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new ClipAssemblyException("interrupted while waiting to be released", interrupted);
        }
    }

    // The panel reports from the Swing thread, so the test waits there for what it expects.
    private static void awaitSwingThread(BooleanSupplier condition) {
        assertTimeoutPreemptively(Duration.ofSeconds(5), () -> {
            AtomicBoolean met = new AtomicBoolean();
            while (!met.get()) {
                SwingUtilities.invokeAndWait(() -> met.set(condition.getAsBoolean()));
            }
        });
    }

    private void fillBuffers() {
        LiveFeed feed = new LiveFeed(gym);
        for (int second = 0; second < Court.DEFAULT_CAPTURE_SECONDS; second++) {
            feed.tick(START.plusSeconds(second));
        }
    }
}
