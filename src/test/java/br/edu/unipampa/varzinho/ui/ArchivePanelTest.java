package br.edu.unipampa.varzinho.ui;

import br.edu.unipampa.varzinho.domain.highlight.Highlight;
import br.edu.unipampa.varzinho.domain.highlight.VideoClip;
import br.edu.unipampa.varzinho.domain.structure.Court;
import br.edu.unipampa.varzinho.domain.structure.Gym;
import br.edu.unipampa.varzinho.enums.Resolution;
import br.edu.unipampa.varzinho.exception.RepositoryException;
import br.edu.unipampa.varzinho.repository.CsvHighlightRepository;
import br.edu.unipampa.varzinho.repository.HighlightRepository;
import br.edu.unipampa.varzinho.repository.InMemoryHighlightRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.swing.JTable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArchivePanelTest {
    private static final ZoneId BRASILIA = ZoneId.of("America/Sao_Paulo");
    private static final Instant EARLY = Instant.parse("2026-09-29T20:00:00Z");
    private static final Instant LATE = Instant.parse("2026-09-29T21:30:15Z");

    private final Gym gym = SampleGym.build();
    private final List<String> errors = new ArrayList<>();

    @TempDir
    private Path archiveDir;

    @Test
    void allCourtsShowsEveryHighlightNewestFirst() throws RepositoryException {
        InMemoryHighlightRepository repository = new InMemoryHighlightRepository();
        repository.save(highlight("h-1", EARLY, 1));
        repository.save(highlight("h-2", LATE, 2));

        ArchivePanel panel = new ArchivePanel(gym, repository, BRASILIA, errors::add);

        assertEquals(2, panel.table().getRowCount());
        assertEquals("camera-h-2", panel.table().getValueAt(0, 2));
        assertEquals("camera-h-1", panel.table().getValueAt(1, 2));
    }

    @Test
    void aCourtAddedAfterTheWindowOpenedCanBeFilteredBy() throws RepositoryException {
        InMemoryHighlightRepository repository = new InMemoryHighlightRepository();
        repository.save(highlight("h-1", EARLY, 1));
        repository.save(highlight("h-3", LATE, 3));
        ArchivePanel panel = new ArchivePanel(gym, repository, BRASILIA, errors::add);
        gym.addCourt(new Court(3));

        panel.refreshCourts();
        panel.filter().setSelectedItem("Court 3");

        assertEquals(1, panel.table().getRowCount());
        assertEquals("camera-h-3", panel.table().getValueAt(0, 2));
    }

    @Test
    void choosingACourtShowsOnlyThatCourtsHighlights() throws RepositoryException {
        InMemoryHighlightRepository repository = new InMemoryHighlightRepository();
        repository.save(highlight("h-1", EARLY, 1));
        repository.save(highlight("h-2", LATE, 2));
        ArchivePanel panel = new ArchivePanel(gym, repository, BRASILIA, errors::add);

        panel.filter().setSelectedItem("Court 2");

        assertEquals(1, panel.table().getRowCount());
        assertEquals(2, panel.table().getValueAt(0, 1));
    }

    @Test
    void filterOffersAllCourtsFollowedByEachCourt() {
        ArchivePanel panel = new ArchivePanel(gym, new InMemoryHighlightRepository(),
                BRASILIA, errors::add);

        assertEquals("All courts", panel.filter().getItemAt(0));
        assertEquals(gym.getCourts().size() + 1, panel.filter().getItemCount());
        assertEquals("Court " + gym.getCourts().get(0).getNumber(), panel.filter().getItemAt(1));
    }

    @Test
    void rowShowsCaptureTimeInTheGivenZoneAndTheClipDetails() throws RepositoryException {
        InMemoryHighlightRepository repository = new InMemoryHighlightRepository();
        repository.save(highlight("h-1", LATE, 1));

        JTable table = new ArchivePanel(gym, repository, BRASILIA, errors::add).table();

        assertEquals(List.of("Captured at", "Court", "Camera", "Duration", "Clip"), columnNames(table));
        assertEquals("2026-09-29 18:30:15", table.getValueAt(0, 0));
        assertEquals(1, table.getValueAt(0, 1));
        assertEquals("camera-h-1", table.getValueAt(0, 2));
        assertEquals("30s", table.getValueAt(0, 3));
        assertEquals("clips/h-1.mp4", table.getValueAt(0, 4));
    }

    @Test
    void refreshPicksUpHighlightsSavedAfterTheTableFilled() throws RepositoryException {
        InMemoryHighlightRepository repository = new InMemoryHighlightRepository();
        ArchivePanel panel = new ArchivePanel(gym, repository, BRASILIA, errors::add);
        repository.save(highlight("h-1", EARLY, 1));

        panel.refresh();

        assertEquals(1, panel.table().getRowCount());
    }

    @Test
    void archiveFailureIsReportedAndLeavesTheRowsAsTheyWere() throws RepositoryException {
        FailingRepository repository = new FailingRepository();
        repository.save(highlight("h-1", EARLY, 1));
        ArchivePanel panel = new ArchivePanel(gym, repository, BRASILIA, errors::add);
        repository.failing = true;

        panel.refresh();

        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("disk unplugged"));
        assertEquals(1, panel.table().getRowCount());
    }

    @Test
    void damagedArchiveShowsTheRecoveredHighlightsAndReportsTheDamage() throws IOException {
        Path file = archiveDir.resolve("highlights.csv");
        Files.write(file, List.of(
                "id,capturedAt,courtNumber,cameraId,clipPath,durationSeconds,resolution,sizeMb",
                "h-1,2026-09-29T20:00:00Z,1,camera-h-1,clips/h-1.mp4,30,FULL_HD,12.5",
                "h-2,not-a-date,1",
                "h-3,2026-09-29T21:30:15Z,1,camera-h-3,clips/h-3.mp4,30,FULL_HD,12.5"));

        ArchivePanel panel = new ArchivePanel(gym, new CsvHighlightRepository(file), BRASILIA, errors::add);

        assertEquals(2, panel.table().getRowCount());
        assertEquals("camera-h-3", panel.table().getValueAt(0, 2));
        assertEquals("camera-h-1", panel.table().getValueAt(1, 2));
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("line 3"));
    }

    private static Highlight highlight(String id, Instant capturedAt, int court) {
        VideoClip clip = new VideoClip("clips/" + id + ".mp4", 30, Resolution.FULL_HD, 12.5);
        return new Highlight(id, capturedAt, court, "camera-" + id, clip);
    }

    private static List<String> columnNames(JTable table) {
        List<String> names = new ArrayList<>();
        for (int column = 0; column < table.getColumnCount(); column++) {
            names.add(table.getColumnName(column));
        }
        return names;
    }

    private static final class FailingRepository implements HighlightRepository {
        private final InMemoryHighlightRepository stored = new InMemoryHighlightRepository();
        private boolean failing;

        @Override
        public void save(Highlight highlight) throws RepositoryException {
            stored.save(highlight);
        }

        @Override
        public Optional<Highlight> findById(String id) throws RepositoryException {
            return stored.findById(id);
        }

        @Override
        public List<Highlight> findAll() throws RepositoryException {
            if (failing) throw new RepositoryException("disk unplugged");
            return stored.findAll();
        }

        @Override
        public List<Highlight> findByCourt(int courtNumber) throws RepositoryException {
            if (failing) throw new RepositoryException("disk unplugged");
            return stored.findByCourt(courtNumber);
        }
    }
}
