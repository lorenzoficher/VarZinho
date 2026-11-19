package br.edu.unipampa.varzinho.ui;

import br.edu.unipampa.varzinho.application.*;
import br.edu.unipampa.varzinho.bootstrap.SampleGym;
import br.edu.unipampa.varzinho.infrastructure.persistence.InMemoryGymRepository;
import br.edu.unipampa.varzinho.infrastructure.persistence.InMemoryHighlightRepository;
import br.edu.unipampa.varzinho.infrastructure.simulation.LiveFeed;
import org.junit.jupiter.api.Test;
import javax.swing.SwingUtilities;
import java.time.Instant;
import java.time.ZoneId;
import static org.junit.jupiter.api.Assertions.*;

class DashboardPanelTest {
    @Test
    void embedsDevicesAndHistoryAndKeepsCourtSelectionWhileRecording() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            VarZinhoTheme.install();
            var gym = SampleGym.build();
            var management = new ManageGym(gym, new InMemoryGymRepository());
            var repository = new InMemoryHighlightRepository();
            var zone = ZoneId.of("America/Sao_Paulo");
            var courts = new CourtPanel(management, 30, message -> fail(message));
            var history = new ArchivePanel(management::courts, new BrowseArchive(repository), zone, message -> fail(message));
            var capture = new CapturePanel(courts, new CaptureHighlight(gym, repository, new PendingClipWrites()),
                    zone, message -> { }, highlight -> history.refresh());
            var dashboard = new DashboardPanel(management, courts, capture, page -> { }, message -> fail(message));
            dashboard.attach(courts.devices(), history);
            dashboard.selector().setSelectedItem(2);
            var feed = new LiveFeed(gym);
            Instant start = Instant.parse("2026-11-19T12:00:00Z");
            for (int i = 0; i < 41; i++) {
                feed.tick(start.plusSeconds(i));
                dashboard.tick(management.court(2));
            }
            assertEquals(2, dashboard.selector().getSelectedItem());
            assertEquals(30, management.court(2).getSecondsRecorded());
            assertTrue(SwingUtilities.isDescendingFrom(courts.devices(), dashboard));
            assertTrue(SwingUtilities.isDescendingFrom(history, dashboard));
            capture.saveButton().doClick();
            assertEquals(2, history.table().getValueAt(0, 1));
        });
    }
}
