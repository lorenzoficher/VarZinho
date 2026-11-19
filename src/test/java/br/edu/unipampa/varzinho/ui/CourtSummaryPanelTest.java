package br.edu.unipampa.varzinho.ui;

import br.edu.unipampa.varzinho.application.BrowseArchive;
import br.edu.unipampa.varzinho.application.ManageGym;
import br.edu.unipampa.varzinho.bootstrap.SampleGym;
import br.edu.unipampa.varzinho.domain.highlight.Highlight;
import br.edu.unipampa.varzinho.domain.highlight.VideoClip;
import br.edu.unipampa.varzinho.domain.structure.Gym;
import br.edu.unipampa.varzinho.enums.Resolution;
import br.edu.unipampa.varzinho.infrastructure.persistence.InMemoryGymRepository;
import br.edu.unipampa.varzinho.infrastructure.persistence.InMemoryHighlightRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CourtSummaryPanelTest {
    private static final ZoneId BRASILIA = ZoneId.of("America/Sao_Paulo");
    private static final Instant NOW = Instant.parse("2026-11-20T00:30:00Z"); // 21:30 in Brasília

    private final Gym gym = SampleGym.build();
    private final InMemoryHighlightRepository repository = new InMemoryHighlightRepository();
    private final List<String> errors = new ArrayList<>();
    private final CourtPanel courts = new CourtPanel(new ManageGym(gym, new InMemoryGymRepository()), 30, errors::add);
    private final CourtSummaryPanel summary = new CourtSummaryPanel(courts, new ManageGym(gym, new InMemoryGymRepository()),
            new BrowseArchive(repository, Clock.fixed(NOW, BRASILIA), BRASILIA), BRASILIA, errors::add);

    @Test
    void theRecordingCardCountsTheSelectedCourtsRecordingCameras() {
        assertEquals("2 de 2", summary.recordingText());

        courts.selectCourt(2);

        assertEquals("1 de 1", summary.recordingText());
    }

    @Test
    void stoppingOrSendingACameraToMaintenanceUpdatesTheRecordingCardAtOnce() {
        courts.selectCamera(1);
        courts.stopButton().doClick();
        assertEquals("1 de 2", summary.recordingText());

        courts.selectCamera(0);
        courts.maintenanceButton().doClick();
        assertEquals("0 de 2", summary.recordingText());

        courts.selectCamera(1);
        courts.startButton().doClick();
        assertEquals("1 de 2", summary.recordingText());
    }

    @Test
    void installingACameraUpdatesTheRecordingCardAtOnce() {
        courts.idField().setText("fixed-9");
        courts.modelField().setText("Fixed Pro");
        courts.installButton().doClick();

        assertEquals(3, courts.cameraLines().size(), errors.toString());
        assertTrue(summary.recordingText().endsWith(" de 3"), summary.recordingText());
    }

    @Test
    void movingACameraAwayUpdatesTheRecordingCardAtOnce() {
        courts.selectCamera(0);
        courts.moveTargetSelector().setSelectedItem(2);
        courts.moveToCourtButton().doClick();

        assertEquals("1 de 1", summary.recordingText(), errors.toString());
    }

    @Test
    void aCameraStoppedOutsideThePanelShowsOnceTheCamerasAreRefreshed() {
        gym.findCourt(1).getCameras().get(0).stopRecording();

        courts.refreshCameras();

        assertEquals("1 de 2", summary.recordingText());
        assertEquals(StatusBadge.STOPPED, courts.statusBadges().get(0));
    }

    @Test
    void theTodayCardSaysWhenNothingHasBeenSavedYet() {
        assertEquals("0", summary.todayText());
    }

    @Test
    void savingAHighlightUpdatesTheTodayCardWithTheLastLocalTime() throws Exception {
        repository.save(highlightAt(1, "2026-11-19T23:00:00Z"));   // 20:00 local
        repository.save(highlightAt(1, "2026-11-20T00:07:00Z"));   // 21:07 local
        repository.save(highlightAt(1, "2026-11-19T02:00:00Z"));   // yesterday, 23:00 local
        repository.save(highlightAt(2, "2026-11-20T00:20:00Z"));   // another court

        summary.refresh();

        assertEquals("2, último às 21:07", summary.todayText());
        assertTrue(errors.isEmpty(), errors.toString());
    }

    private static Highlight highlightAt(int court, String instant) {
        Instant capturedAt = Instant.parse(instant);
        String id = court + "-" + capturedAt.toEpochMilli();
        return new Highlight(id, capturedAt, court, "fixed-1",
                new VideoClip("clips/" + id + ".mp4", 30, Resolution.FULL_HD, 1.0));
    }
}
