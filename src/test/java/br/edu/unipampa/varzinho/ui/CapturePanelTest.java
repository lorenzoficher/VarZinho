package br.edu.unipampa.varzinho.ui;

import br.edu.unipampa.varzinho.domain.capture.Camera;
import br.edu.unipampa.varzinho.domain.capture.StreamCamera;
import br.edu.unipampa.varzinho.domain.highlight.Highlight;
import br.edu.unipampa.varzinho.domain.structure.Court;
import br.edu.unipampa.varzinho.domain.structure.Gym;
import br.edu.unipampa.varzinho.enums.Resolution;
import br.edu.unipampa.varzinho.exception.ClipAssemblyException;
import br.edu.unipampa.varzinho.repository.InMemoryHighlightRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CapturePanelTest {
    private static final ZoneId BRASILIA = ZoneId.of("America/Sao_Paulo");
    private static final Instant START = Instant.parse("2026-09-29T20:00:00Z");

    private final Gym gym = SampleGym.build();
    private final InMemoryHighlightRepository repository = new InMemoryHighlightRepository();
    private final List<String> errors = new ArrayList<>();
    private final List<Highlight> saved = new ArrayList<>();
    private final CourtPanel courtPanel = new CourtPanel(gym, 30, errors::add);
    private final CapturePanel panel = new CapturePanel(courtPanel, repository, BRASILIA, errors::add, saved::add);

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
        Gym phoneGym = SampleGym.build();
        phoneGym.addCourt(new Court(3));
        StreamCamera phone = new StreamCamera("phone-1", "Phone", Resolution.HD, 30, "http://phone/video",
                (window, target) -> { throw new ClipAssemblyException("ffmpeg is not installed"); });
        phoneGym.installCamera(3, phone);
        phone.startRecording();
        CourtPanel phoneCourts = new CourtPanel(phoneGym, 30, errors::add);
        CapturePanel phonePanel = new CapturePanel(phoneCourts, repository, BRASILIA, errors::add, saved::add);
        LiveFeed feed = new LiveFeed(phoneGym);
        for (int second = 0; second < Court.DEFAULT_CAPTURE_SECONDS; second++) {
            feed.tick(START.plusSeconds(second));
        }
        phoneCourts.selectCourt(3);

        phonePanel.saveButton().doClick();

        assertEquals(List.of("ffmpeg is not installed"), errors);
        assertTrue(repository.findAll().isEmpty());
        assertEquals("", phonePanel.confirmation());
    }

    private void fillBuffers() {
        LiveFeed feed = new LiveFeed(gym);
        for (int second = 0; second < Court.DEFAULT_CAPTURE_SECONDS; second++) {
            feed.tick(START.plusSeconds(second));
        }
    }
}
