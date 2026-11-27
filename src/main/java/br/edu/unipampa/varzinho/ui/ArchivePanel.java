package br.edu.unipampa.varzinho.ui;

import br.edu.unipampa.varzinho.application.ArchiveListing;
import br.edu.unipampa.varzinho.application.BrowseArchive;
import br.edu.unipampa.varzinho.application.CourtView;
import br.edu.unipampa.varzinho.application.HighlightView;
import br.edu.unipampa.varzinho.application.ManageRecordings;
import br.edu.unipampa.varzinho.application.port.RepositoryException;
import java.awt.BorderLayout;
import java.awt.Desktop;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;

/** Coordinates the archive query with the filter bar, recording table and actions. */
final class ArchivePanel extends JPanel {
    private static final Logger LOGGER = Logger.getLogger(ArchivePanel.class.getName());
    /** Opens a clip's file: the window's is the system player, a test's records the call. */
    @FunctionalInterface
    interface ClipPlayer {
        void open(Path clip) throws IOException;
    }
    private final BrowseArchive archive;
    private final Consumer<String> errorSink;
    private IntConsumer onCount = count -> { };
    private final ArchiveFilters filters;
    private final ArchiveTable recordings;
    private final RecordingActions actions;
    private int lookups;

    /** One reading of the archive: the rows asked for, and the whole archive's size when it was counted too. */
    private record Reading(ArchiveListing listing, OptionalInt total) { }

    ArchivePanel(Supplier<List<CourtView>> courts, BrowseArchive archive, ZoneId zone, Consumer<String> errorSink) {
        this(courts, archive, Clock.system(zone), zone, clip -> Desktop.getDesktop().open(clip.toFile()), errorSink);
    }

    /** @param clock tells which local day, in {@code zone}, is today and which yesterday */
    ArchivePanel(Supplier<List<CourtView>> courts, BrowseArchive archive, Clock clock, ZoneId zone,
                 ClipPlayer player, Consumer<String> errorSink) {
        super(new BorderLayout(4, 4));
        if (courts == null) throw new IllegalArgumentException("an archive panel needs the gym's courts");
        if (archive == null) throw new IllegalArgumentException("an archive panel needs the archive to browse");
        if (clock == null) throw new IllegalArgumentException("an archive panel needs a clock to know today");
        if (zone == null) throw new IllegalArgumentException("an archive panel needs a time zone to show times in");
        if (player == null) throw new IllegalArgumentException("an archive panel needs a player to open clips");
        if (errorSink == null) throw new IllegalArgumentException("an archive panel needs somewhere to report errors");
        this.archive = archive;
        this.errorSink = errorSink;
        VarZinhoTheme.styleCard(this);
        DateTimeFormatter format = timeFormatIn(zone);
        recordings = new ArchiveTable(format);
        actions = new RecordingActions(recordings, format, player, errorSink, this::refresh, this::captureSaved);
        filters = new ArchiveFilters(courts, clock, zone, actions.openClipButton(), recordings, this::refresh);
        add(filters, BorderLayout.NORTH);
        add(recordings, BorderLayout.CENTER);
        refresh();
    }

    /**
     * How the window writes a capture time, so the table and the save confirmation
     * show the same moment the same way.
     */
    static DateTimeFormatter timeFormatIn(ZoneId zone) {
        return DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss").withZone(zone);
    }

    JComboBox<String> filter() { return filters.filter(); }
    JComboBox<String> dayFilter() { return filters.dayFilter(); }
    JTextField dateField() { return filters.dateField(); }
    JTextField searchField() { return filters.searchField(); }
    JTable table() { return recordings.table(); }
    JButton openClipButton() { return actions.openClipButton(); }
    void enableEditing(ManageRecordings manageRecordings) { actions.enableEditing(manageRecordings); }
    void deleteSelection(ManageRecordings manageRecordings, Predicate<HighlightView> confirm) {
        actions.deleteSelection(manageRecordings, confirm);
    }
    void renameSelection(ManageRecordings manageRecordings, Function<HighlightView, Optional<String>> askName) {
        actions.renameSelection(manageRecordings, askName);
    }
    /** The rename and delete buttons, once editing is enabled. */
    List<JButton> editActions() { return actions.editActions(); }
    void refreshCourts() { filters.refreshCourts(); }
    static boolean playable(String path) { return RecordingActions.playable(path); }

    void onCount(IntConsumer listener) {
        onCount = listener;
        listener.accept(recordings.count());
    }

    /**
     * Reads the archive again for the chosen court and day, on a worker. Damaged lines
     * are reported and the readable records shown; an unreadable date or any other
     * failure is reported and the rows are kept. Only the latest reading is shown.
     */
    void refresh() {
        read(false);
    }

    /** Reads the archive after a capture, counting the whole archive when a filter hides part of it. */
    void captureSaved() {
        read(!filters.includesAll());
    }

    private void read(boolean countAll) {
        Optional<LocalDate> day;
        try {
            day = filters.chosenDay();
        } catch (DateTimeParseException unreadable) {
            errorSink.accept("Informe a data como dd/mm/aaaa.");
            return;
        }
        Optional<Integer> court = filters.courtNumber();
        int reading = ++lookups;
        RepositoryTask.run(filters.queryControls(), "Lendo o acervo…",
                () -> new Reading(lookup(day, court), countAll ? countAll() : OptionalInt.empty()),
                result -> { if (reading == lookups) show(result); },
                failure -> { if (reading == lookups) readFailed(failure); });
    }

    private void readFailed(Throwable failure) {
        Level level = failure instanceof RepositoryException ? Level.WARNING : Level.SEVERE;
        LOGGER.log(level, "Could not read the highlight archive", failure);
        errorSink.accept("Não foi possível ler o acervo. Verifique o armazenamento.");
    }

    private void show(Reading reading) {
        ArchiveListing listing = reading.listing();
        reading.total().ifPresent(onCount::accept);
        recordings.show(listing.getHighlights());
        if (filters.includesAll()) onCount.accept(recordings.count());
        actions.updateOpenClip();
        listing.getWarning().ifPresent(warning -> {
            LOGGER.warning(warning);
            errorSink.accept("Acervo danificado: " + PortugueseText.message(warning));
        });
    }

    private ArchiveListing lookup(Optional<LocalDate> day, Optional<Integer> court) throws RepositoryException {
        if (court.isEmpty()) return day.isPresent() ? archive.byDay(day.get()) : archive.all();
        return day.isPresent() ? archive.byDay(day.get(), court.get()) : archive.byCourt(court.get());
    }

    // A count that cannot be read only leaves the old one; the rows asked for still show.
    private OptionalInt countAll() {
        try {
            return OptionalInt.of(archive.all().getHighlights().size());
        } catch (RepositoryException failure) {
            LOGGER.log(Level.WARNING, "Could not update archive count", failure);
            return OptionalInt.empty();
        }
    }
}
