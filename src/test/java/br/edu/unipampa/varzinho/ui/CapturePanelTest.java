package br.edu.unipampa.varzinho.ui;

import br.edu.unipampa.varzinho.application.CaptureHighlight;
import br.edu.unipampa.varzinho.application.HighlightView;
import br.edu.unipampa.varzinho.application.ManageGym;
import br.edu.unipampa.varzinho.bootstrap.SampleGym;
import br.edu.unipampa.varzinho.domain.capture.ClipAssembler;
import br.edu.unipampa.varzinho.domain.capture.StreamCamera;
import br.edu.unipampa.varzinho.domain.highlight.Highlight;
import br.edu.unipampa.varzinho.domain.structure.Court;
import br.edu.unipampa.varzinho.domain.structure.Gym;
import br.edu.unipampa.varzinho.enums.Resolution;
import br.edu.unipampa.varzinho.exception.ClipAssemblyException;
import br.edu.unipampa.varzinho.infrastructure.persistence.InMemoryGymRepository;
import br.edu.unipampa.varzinho.infrastructure.persistence.InMemoryHighlightRepository;
import br.edu.unipampa.varzinho.infrastructure.simulation.LiveFeed;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.SwingUtilities;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CapturePanelTest {
    private static final ZoneId BRASILIA = ZoneId.of("America/Sao_Paulo");
    private static final Instant START = Instant.parse("2026-09-29T20:00:00Z");

    private final Gym gym = SampleGym.build();
    private final InMemoryHighlightRepository repository = new InMemoryHighlightRepository();
    private final List<String> errors = new ArrayList<>();
    private final List<HighlightView> saved = new ArrayList<>();
    private CourtPanel courtPanel;
    private CapturePanel panel;

    @BeforeEach
    void buildPanelsOnSwing() throws Exception {
        onSwing(() -> {
            courtPanel = courtsFor(gym);
            panel = new CapturePanel(courtPanel, new CaptureHighlight(gym, repository),
                    BRASILIA, errors::add, saved::add);
        });
    }

    @Test
    void onePressSavesExactlyOneHighlightForTheSelectedCourt() throws Exception {
        onSwing(() -> {
            fillBuffers(gym);
            courtPanel.selectCourt(2);
        });

        press(panel);
        awaitFinished(panel);

        assertEquals(1, repository.findAll().size());
        assertEquals(1, repository.findByCourt(2).size());
        assertTrue(errors.isEmpty());
    }

    @Test
    void confirmationNamesCourtCameraLocalTimeAndDuration() throws Exception {
        onSwing(() -> fillBuffers(gym));

        press(panel);
        awaitFinished(panel);

        Highlight highlight = repository.findAll().get(0);
        String localTime = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                .withZone(BRASILIA).format(highlight.getCapturedAt());
        onSwing(() -> assertEquals("Saved: court 1 · camera fixed-1 · " + localTime + " · 30s",
                panel.confirmation()));
        assertEquals(highlight.getClip().getFilePath(), saved.get(0).getClipPath());
    }

    @Test
    void buttonLabelFollowsTheSelectedCourt() throws Exception {
        onSwing(() -> {
            assertEquals("Save highlight - court 1", panel.saveButton().getText());
            courtPanel.selectCourt(2);
            assertEquals("Save highlight - court 2", panel.saveButton().getText());
        });
    }

    @Test
    void courtWithNoActiveCameraReportsTheDomainMessageAndSavesNothing() throws Exception {
        onSwing(() -> {
            fillBuffers(gym);
            courtPanel.selectCourt(2);
            gym.findCourt(2).getCameras().get(0).stopRecording();
        });

        press(panel);
        awaitFinished(panel);

        assertEquals(1, errors.size());
        assertTrue(repository.findAll().isEmpty());
        assertTrue(saved.isEmpty());
        onSwing(() -> assertEquals("", panel.confirmation()));
    }

    @Test
    void failedSaveClearsThePreviousConfirmation() throws Exception {
        onSwing(() -> {
            fillBuffers(gym);
            courtPanel.selectCourt(2);
        });
        press(panel);
        awaitFinished(panel);
        onSwing(() -> gym.findCourt(2).getCameras().get(0).stopRecording());

        press(panel);
        awaitFinished(panel);

        assertEquals(1, errors.size());
        onSwing(() -> assertEquals("", panel.confirmation()));
    }

    @Test
    void emptyBufferReportsTheDomainMessageAndSavesNothing() throws Exception {
        press(panel);
        awaitFinished(panel);

        assertEquals(1, errors.size());
        assertTrue(repository.findAll().isEmpty());
        assertTrue(saved.isEmpty());
    }

    @Test
    void phoneThatCannotWriteItsClipReportsTheFailureAndSavesNothing() throws Exception {
        Phone phone = phoneWritingWith((window, target) -> {
            throw new ClipAssemblyException("ffmpeg is not installed");
        });

        press(phone.panel());
        awaitFinished(phone.panel());

        assertEquals(List.of("ffmpeg is not installed"), errors);
        assertTrue(repository.findAll().isEmpty());
        onSwing(() -> assertEquals("", phone.panel().confirmation()));
    }

    @Test
    void realCameraAssemblyRunsOffSwingWhileTheWindowKeepsResponding() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        AtomicBoolean writesOnSwing = new AtomicBoolean();
        Phone phone = phoneWritingWith((window, target) -> {
            writesOnSwing.set(SwingUtilities.isEventDispatchThread());
            started.countDown();
            awaitQuietly(release);
        });

        SwingUtilities.invokeLater(() -> phone.panel().saveButton().doClick());
        try {
            assertTrue(started.await(5, TimeUnit.SECONDS));
            assertFalse(writesOnSwing.get());
            CountDownLatch responded = new CountDownLatch(1);
            SwingUtilities.invokeLater(responded::countDown);
            assertTrue(responded.await(5, TimeUnit.SECONDS));
            assertTrue(repository.findAll().isEmpty());
            onSwing(() -> {
                assertTrue(saved.isEmpty());
                assertEquals("Saving: court 3…", phone.panel().confirmation());
            });
        } finally {
            release.countDown();
        }
        awaitFinished(phone.panel());
        assertEquals(1, saved.size());
    }

    @Test
    void phoneClipIsArchivedOnlyAfterTheAssemblerReturnsAndCallbacksRunOnSwing() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        AtomicBoolean finished = new AtomicBoolean();
        Phone phone = phoneWritingWith((window, target) -> {
            started.countDown();
            awaitQuietly(release);
            finished.set(true);
        });
        List<HighlightView> delivered = new ArrayList<>();
        AtomicBoolean callbackOnSwing = new AtomicBoolean();
        AtomicReference<CapturePanel> capture = new AtomicReference<>();
        onSwing(() -> capture.set(new CapturePanel(phone.courts(),
                new CaptureHighlight(phone.gym(), repository), BRASILIA, errors::add, view -> {
                    assertTrue(finished.get());
                    callbackOnSwing.set(SwingUtilities.isEventDispatchThread());
                    delivered.add(view);
                }, phone.activity())));

        press(capture.get());
        try {
            assertTrue(started.await(5, TimeUnit.SECONDS));
            assertTrue(repository.findAll().isEmpty());
        } finally {
            release.countDown();
        }
        awaitFinished(capture.get());

        assertEquals(1, repository.findByCourt(3).size());
        assertEquals(1, delivered.size());
        assertTrue(callbackOnSwing.get());
        assertTrue(errors.isEmpty());
    }

    @Test
    void anotherPressCannotReadTheCameraWhileItsCaptureIsRunning() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        AtomicInteger writes = new AtomicInteger();
        Phone phone = phoneWritingWith((window, target) -> {
            writes.incrementAndGet();
            started.countDown();
            awaitQuietly(release);
        });

        press(phone.panel());
        try {
            assertTrue(started.await(5, TimeUnit.SECONDS));
            onSwing(() -> {
                assertFalse(phone.panel().saveButton().isEnabled());
                phone.panel().saveButton().doClick();
            });
            assertEquals(1, writes.get());
        } finally {
            release.countDown();
        }
        awaitFinished(phone.panel());
        assertEquals(1, repository.findAll().size());

        press(phone.panel());
        awaitFinished(phone.panel());
        assertEquals(2, writes.get());
        assertEquals(2, repository.findAll().size());
    }

    @Test
    void feedsAndPhoneDropWaitUntilTheCaptureReturnsTheCamera() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        AtomicInteger ticks = new AtomicInteger();
        AtomicBoolean dropped = new AtomicBoolean();
        Phone phone = phoneWritingWith((window, target) -> {
            started.countDown();
            awaitQuietly(release);
        });

        press(phone.panel());
        try {
            assertTrue(started.await(5, TimeUnit.SECONDS));
            onSwing(() -> {
                phone.activity().ifIdle(() -> {
                    phone.feed().tick(START.plusSeconds(60));
                    ticks.incrementAndGet();
                });
                phone.activity().whenIdle(() -> {
                    phone.camera().stopRecording();
                    dropped.set(true);
                });
                assertFalse(phone.courts().stopButton().isEnabled());
                assertFalse(phone.courts().installButton().isEnabled());
                assertFalse(dropped.get());
                assertEquals(0, ticks.get());
            });
        } finally {
            release.countDown();
        }
        awaitFinished(phone.panel());

        onSwing(() -> {
            assertTrue(dropped.get());
            assertFalse(phone.camera().isRecording());
            assertTrue(phone.courts().installButton().isEnabled());
            phone.activity().ifIdle(ticks::incrementAndGet);
            assertEquals(1, ticks.get());
            assertFalse(phone.courts().moveButton().isEnabled());
        });
    }

    @Test
    void aFailedAssemblyRestoresCameraControlsAndAllowsAnotherCapture() throws Exception {
        AtomicInteger attempts = new AtomicInteger();
        Phone phone = phoneWritingWith((window, target) -> {
            if (attempts.incrementAndGet() == 1) throw new ClipAssemblyException("broken segment");
        });

        press(phone.panel());
        awaitFinished(phone.panel());
        onSwing(() -> {
            assertFalse(phone.activity().isRunning());
            assertTrue(phone.courts().installButton().isEnabled());
            assertFalse(phone.courts().moveButton().isEnabled());
        });
        assertTrue(repository.findAll().isEmpty());

        press(phone.panel());
        awaitFinished(phone.panel());

        assertEquals(1, errors.size());
        assertEquals(1, repository.findAll().size());
    }

    private CourtPanel courtsFor(Gym model) {
        return new CourtPanel(new ManageGym(model, new InMemoryGymRepository()), 30, errors::add);
    }

    private Phone phoneWritingWith(ClipAssembler writer) throws Exception {
        AtomicReference<Phone> result = new AtomicReference<>();
        onSwing(() -> {
            Gym model = SampleGym.build();
            model.addCourt(new Court(3));
            StreamCamera camera = new StreamCamera("phone-1", "Phone", Resolution.HD, 30,
                    "http://phone/video", writer);
            model.installCamera(3, camera);
            camera.startRecording();
            CourtPanel courts = courtsFor(model);
            CaptureActivity activity = new CaptureActivity();
            CapturePanel capture = new CapturePanel(courts, new CaptureHighlight(model, repository),
                    BRASILIA, errors::add, saved::add, activity);
            LiveFeed feed = new LiveFeed(model);
            for (int second = 0; second < Court.DEFAULT_CAPTURE_SECONDS; second++) {
                feed.tick(START.plusSeconds(second));
            }
            courts.selectCourt(3);
            result.set(new Phone(capture, courts, activity, feed, camera, model));
        });
        return result.get();
    }

    private static void press(CapturePanel capture) throws Exception {
        onSwing(() -> capture.saveButton().doClick());
    }

    private static void awaitFinished(CapturePanel capture) {
        awaitSwingThread(() -> capture.saveButton().isEnabled());
    }

    private static void awaitQuietly(CountDownLatch release) {
        try {
            if (!release.await(5, TimeUnit.SECONDS)) throw new ClipAssemblyException("test did not release the clip");
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new ClipAssemblyException("interrupted while waiting to be released", interrupted);
        }
    }

    private static void onSwing(Runnable action) throws Exception {
        SwingUtilities.invokeAndWait(action);
    }

    private static void awaitSwingThread(BooleanSupplier condition) {
        assertTimeoutPreemptively(Duration.ofSeconds(5), () -> {
            AtomicBoolean met = new AtomicBoolean();
            while (!met.get()) onSwing(() -> met.set(condition.getAsBoolean()));
        });
    }

    private static void fillBuffers(Gym model) {
        LiveFeed feed = new LiveFeed(model);
        for (int second = 0; second < Court.DEFAULT_CAPTURE_SECONDS; second++) {
            feed.tick(START.plusSeconds(second));
        }
    }

    private record Phone(CapturePanel panel, CourtPanel courts, CaptureActivity activity,
                         LiveFeed feed, StreamCamera camera, Gym gym) { }
}
