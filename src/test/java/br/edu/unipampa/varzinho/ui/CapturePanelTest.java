package br.edu.unipampa.varzinho.ui;

import br.edu.unipampa.varzinho.domain.capture.Camera;
import br.edu.unipampa.varzinho.domain.highlight.Highlight;
import br.edu.unipampa.varzinho.domain.structure.Gym;
import br.edu.unipampa.varzinho.repository.InMemoryHighlightRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CapturePanelTest {
    private static final Instant START = Instant.parse("2026-09-29T20:00:00Z");

    private final Gym gym = SampleGym.build();
    private final InMemoryHighlightRepository repository = new InMemoryHighlightRepository();
    private final List<String> errors = new ArrayList<>();
    private final List<Highlight> saved = new ArrayList<>();
    private final CourtPanel courtPanel = new CourtPanel(gym, errors::add);
    private final CapturePanel panel = new CapturePanel(courtPanel, repository, errors::add, saved::add);

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
    void confirmationDescribesTheSavedHighlight() {
        fillBuffers();

        panel.saveButton().doClick();

        Highlight highlight = repository.findAll().get(0);
        assertEquals(highlight.describe(), panel.confirmation());
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
    void emptyBufferReportsTheDomainMessageAndSavesNothing() {
        panel.saveButton().doClick();

        assertEquals(1, errors.size());
        assertTrue(repository.findAll().isEmpty());
        assertTrue(saved.isEmpty());
    }

    private void fillBuffers() {
        LiveFeed feed = new LiveFeed(gym, 30);
        for (int second = 0; second < 5; second++) {
            feed.tick(START.plusSeconds(second));
        }
    }
}
