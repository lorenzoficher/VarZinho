package br.edu.unipampa.varzinho.ui;

import br.edu.unipampa.varzinho.bootstrap.SampleGym;
import br.edu.unipampa.varzinho.application.BrowseArchive;
import br.edu.unipampa.varzinho.application.CourtView;
import br.edu.unipampa.varzinho.application.ManageGym;
import br.edu.unipampa.varzinho.infrastructure.persistence.InMemoryGymRepository;
import br.edu.unipampa.varzinho.application.port.RepositoryException;
import br.edu.unipampa.varzinho.domain.highlight.Highlight;
import br.edu.unipampa.varzinho.domain.highlight.VideoClip;
import br.edu.unipampa.varzinho.domain.structure.Court;
import br.edu.unipampa.varzinho.domain.structure.Gym;
import br.edu.unipampa.varzinho.enums.Resolution;
import br.edu.unipampa.varzinho.infrastructure.persistence.CsvHighlightRepository;
import br.edu.unipampa.varzinho.infrastructure.persistence.InMemoryHighlightRepository;
import br.edu.unipampa.varzinho.application.port.HighlightRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.swing.JTable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArchivePanelTest {
    private static final ZoneId BRASILIA = ZoneId.of("America/Sao_Paulo");
    private static final Instant EARLY = Instant.parse("2026-09-29T20:00:00Z");
    private static final Instant LATE = Instant.parse("2026-09-29T21:30:15Z");
    // 21:30 on 19/11/2026 in Brasília, already 20/11 in UTC.
    private static final Clock TONIGHT = Clock.fixed(Instant.parse("2026-11-20T00:30:00Z"), BRASILIA);

    private final Gym gym = SampleGym.build();
    private final List<String> errors = new ArrayList<>();

    @TempDir
    private Path archiveDir;

    @Test
    void allCourtsShowsEveryHighlightNewestFirst() throws RepositoryException {
        InMemoryHighlightRepository repository = new InMemoryHighlightRepository();
        repository.save(highlight("h-1", EARLY, 1));
        repository.save(highlight("h-2", LATE, 2));

        ArchivePanel panel = new ArchivePanel(courts(), new BrowseArchive(repository), BRASILIA, errors::add);

        assertEquals(2, panel.table().getRowCount());
        assertEquals("camera-h-2", panel.table().getValueAt(0, 2));
        assertEquals("camera-h-1", panel.table().getValueAt(1, 2));
    }

    @Test
    void aCourtAddedAfterTheWindowOpenedCanBeFilteredBy() throws RepositoryException {
        InMemoryHighlightRepository repository = new InMemoryHighlightRepository();
        repository.save(highlight("h-1", EARLY, 1));
        repository.save(highlight("h-3", LATE, 3));
        ArchivePanel panel = new ArchivePanel(courts(), new BrowseArchive(repository), BRASILIA, errors::add);
        gym.addCourt(new Court(3));

        panel.refreshCourts();
        panel.filter().setSelectedItem("Quadra 3");

        assertEquals(1, panel.table().getRowCount());
        assertEquals("camera-h-3", panel.table().getValueAt(0, 2));
    }

    @Test
    void choosingACourtShowsOnlyThatCourtsHighlights() throws RepositoryException {
        InMemoryHighlightRepository repository = new InMemoryHighlightRepository();
        repository.save(highlight("h-1", EARLY, 1));
        repository.save(highlight("h-2", LATE, 2));
        ArchivePanel panel = new ArchivePanel(courts(), new BrowseArchive(repository), BRASILIA, errors::add);

        panel.filter().setSelectedItem("Quadra 2");

        assertEquals(1, panel.table().getRowCount());
        assertEquals(2, panel.table().getValueAt(0, 1));
    }

    @Test
    void filterOffersAllCourtsFollowedByEachCourt() {
        ArchivePanel panel = new ArchivePanel(courts(), new BrowseArchive(new InMemoryHighlightRepository()), BRASILIA, errors::add);

        assertEquals("Todas as quadras", panel.filter().getItemAt(0));
        assertEquals(gym.getCourts().size() + 1, panel.filter().getItemCount());
        assertEquals("Quadra " + gym.getCourts().get(0).getNumber(), panel.filter().getItemAt(1));
    }

    @Test
    void rowShowsCaptureTimeInTheGivenZoneAndTheClipDetails() throws RepositoryException {
        InMemoryHighlightRepository repository = new InMemoryHighlightRepository();
        repository.save(highlight("h-1", LATE, 1));

        JTable table = new ArchivePanel(courts(), new BrowseArchive(repository), BRASILIA, errors::add).table();

        assertEquals(List.of("Data e hora", "Quadra", "Câmera", "Duração", "Clipe"), columnNames(table));
        assertEquals("29/09/2026 18:30:15", table.getValueAt(0, 0));
        assertEquals(1, table.getValueAt(0, 1));
        assertEquals("camera-h-1", table.getValueAt(0, 2));
        assertEquals("30 s", table.getValueAt(0, 3));
        assertEquals("clips/h-1.mp4", table.getValueAt(0, 4));
    }

    @Test
    void refreshPicksUpHighlightsSavedAfterTheTableFilled() throws RepositoryException {
        InMemoryHighlightRepository repository = new InMemoryHighlightRepository();
        ArchivePanel panel = new ArchivePanel(courts(), new BrowseArchive(repository), BRASILIA, errors::add);
        repository.save(highlight("h-1", EARLY, 1));

        panel.refresh();

        assertEquals(1, panel.table().getRowCount());
    }

    @Test
    void archiveFailureIsReportedAndLeavesTheRowsAsTheyWere() throws RepositoryException {
        FailingRepository repository = new FailingRepository();
        repository.save(highlight("h-1", EARLY, 1));
        ArchivePanel panel = new ArchivePanel(courts(), new BrowseArchive(repository), BRASILIA, errors::add);
        repository.failing = true;

        panel.refresh();

        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("ler o acervo"));
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

        ArchivePanel panel = new ArchivePanel(courts(), new BrowseArchive(new CsvHighlightRepository(file)), BRASILIA, errors::add);

        assertEquals(2, panel.table().getRowCount());
        assertEquals("camera-h-3", panel.table().getValueAt(0, 2));
        assertEquals("camera-h-1", panel.table().getValueAt(1, 2));
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("linha 3"));
    }

    @Test
    void choosingADayAndACourtListsOnlyThatDaysHighlightsOnThatCourtNewestFirst() throws RepositoryException {
        InMemoryHighlightRepository repository = new InMemoryHighlightRepository();
        repository.save(highlight("today-early", Instant.parse("2026-11-19T22:00:00Z"), 2));
        repository.save(highlight("today-late", Instant.parse("2026-11-20T00:07:00Z"), 2));
        repository.save(highlight("today-other-court", Instant.parse("2026-11-19T23:00:00Z"), 1));
        repository.save(highlight("yesterday", Instant.parse("2026-11-19T02:00:00Z"), 2));
        ArchivePanel panel = panelAt(repository);

        panel.dayFilter().setSelectedItem("Hoje");
        panel.filter().setSelectedItem("Quadra 2");

        assertEquals(List.of("camera-today-late", "camera-today-early"), cameras(panel));
    }

    @Test
    void yesterdayIsTheLocalDayBeforeToday() throws RepositoryException {
        InMemoryHighlightRepository repository = new InMemoryHighlightRepository();
        repository.save(highlight("today", Instant.parse("2026-11-20T00:07:00Z"), 1));
        repository.save(highlight("yesterday-late", Instant.parse("2026-11-19T02:00:00Z"), 1));
        ArchivePanel panel = panelAt(repository);

        panel.dayFilter().setSelectedItem("Ontem");

        assertEquals(List.of("camera-yesterday-late"), cameras(panel));
    }

    @Test
    void aChosenDateListsThatDay() throws RepositoryException {
        InMemoryHighlightRepository repository = new InMemoryHighlightRepository();
        repository.save(highlight("h-1", EARLY, 1));
        repository.save(highlight("today", Instant.parse("2026-11-20T00:07:00Z"), 1));
        ArchivePanel panel = panelAt(repository);

        panel.dayFilter().setSelectedItem("Escolher data");
        panel.dateField().setText("29/09/2026");
        panel.dateField().postActionEvent();

        assertEquals(List.of("camera-h-1"), cameras(panel));
        assertTrue(errors.isEmpty(), errors.toString());
    }

    @Test
    void choosingADateStartsFromTodaySoTheRowsAlwaysMatchTheFilter() throws RepositoryException {
        InMemoryHighlightRepository repository = new InMemoryHighlightRepository();
        repository.save(highlight("h-1", EARLY, 2));
        repository.save(highlight("today", Instant.parse("2026-11-20T00:07:00Z"), 2));
        ArchivePanel panel = panelAt(repository);

        panel.dayFilter().setSelectedItem("Escolher data");
        panel.filter().setSelectedItem("Quadra 2");

        assertEquals("19/11/2026", panel.dateField().getText());
        assertEquals(List.of("camera-today"), cameras(panel));
        assertTrue(errors.isEmpty(), errors.toString());
    }

    @Test
    void anUnreadableDateIsReportedAndTheRowsAreKept() throws RepositoryException {
        InMemoryHighlightRepository repository = new InMemoryHighlightRepository();
        repository.save(highlight("h-1", EARLY, 1));
        ArchivePanel panel = panelAt(repository);

        panel.dayFilter().setSelectedItem("Escolher data");
        panel.dateField().setText("29/09/2026");
        panel.dateField().postActionEvent();
        panel.dateField().setText("31/02/2026");
        panel.dateField().postActionEvent();

        assertEquals(List.of("Informe a data como dd/mm/aaaa."), errors);
        assertEquals(1, panel.table().getRowCount());
    }

    @Test
    void aDamagedArchiveFilteredByDayStillShowsTheReadableHighlightsAndTheWarning() throws IOException {
        Path file = archiveDir.resolve("highlights.csv");
        Files.write(file, List.of(
                "id,capturedAt,courtNumber,cameraId,clipPath,durationSeconds,resolution,sizeMb",
                "h-1,2026-11-19T22:00:00Z,1,camera-h-1,clips/h-1.mp4,30,FULL_HD,12.5",
                "h-2,not-a-date,1"));
        ArchivePanel panel = panelAt(new CsvHighlightRepository(file));
        errors.clear();

        panel.dayFilter().setSelectedItem("Hoje");

        assertEquals(List.of("camera-h-1"), cameras(panel));
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("linha 3"));
    }

    @Test
    void openClipIsDisabledUntilAHighlightIsChosen() {
        ArchivePanel panel = new ArchivePanel(courts(), new BrowseArchive(new InMemoryHighlightRepository()), BRASILIA, errors::add);

        assertFalse(panel.openClipButton().isEnabled());
    }

    @Test
    void aClipWithNoFileDisablesTheButtonAndSaysWhy() throws RepositoryException {
        InMemoryHighlightRepository repository = new InMemoryHighlightRepository();
        repository.save(highlight("h-never-written", EARLY, 1));
        ArchivePanel panel = panelAt(repository);

        panel.table().setRowSelectionInterval(0, 0);

        assertFalse(panel.openClipButton().isEnabled());
        assertEquals("Este lance não tem arquivo de vídeo.", panel.openClipButton().getToolTipText());
    }

    @Test
    void aClipWhoseFileExistsOpensInThePlayer() throws Exception {
        Path written = Files.createFile(archiveDir.resolve("phone.mp4"));
        InMemoryHighlightRepository repository = new InMemoryHighlightRepository();
        repository.save(new Highlight("phone", EARLY, 1, "phone-1",
                new VideoClip(written.toString(), 30, Resolution.FULL_HD, 12.5)));
        List<Path> opened = new ArrayList<>();
        ArchivePanel panel = panelAt(repository, opened::add);

        panel.table().setRowSelectionInterval(0, 0);
        panel.openClipButton().doClick();

        assertEquals(List.of(written), opened);
        assertTrue(errors.isEmpty(), errors.toString());
    }

    @Test
    void withNoPlayerAvailableTheMessageBarSaysSo() throws Exception {
        Path written = Files.createFile(archiveDir.resolve("phone.mp4"));
        InMemoryHighlightRepository repository = new InMemoryHighlightRepository();
        repository.save(new Highlight("phone", EARLY, 1, "phone-1",
                new VideoClip(written.toString(), 30, Resolution.FULL_HD, 12.5)));
        ArchivePanel panel = panelAt(repository, clip -> {
            throw new UnsupportedOperationException("no desktop");
        });

        panel.table().setRowSelectionInterval(0, 0);
        panel.openClipButton().doClick();

        assertEquals(List.of("Nenhum reprodutor de vídeo disponível para abrir o clipe."), errors);
    }

    private ArchivePanel panelAt(HighlightRepository repository) {
        return panelAt(repository, clip -> { throw new AssertionError("no player should open " + clip); });
    }

    private ArchivePanel panelAt(HighlightRepository repository, ArchivePanel.ClipPlayer player) {
        return new ArchivePanel(courts(), new BrowseArchive(repository, TONIGHT, BRASILIA), TONIGHT, BRASILIA,
                player, errors::add);
    }

    private static List<String> cameras(ArchivePanel panel) {
        List<String> cameras = new ArrayList<>();
        for (int row = 0; row < panel.table().getRowCount(); row++) {
            cameras.add((String) panel.table().getValueAt(row, 2));
        }
        return cameras;
    }

    private Supplier<List<CourtView>> courts() {
        return new ManageGym(gym, new InMemoryGymRepository())::courts;
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
