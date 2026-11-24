package br.edu.unipampa.varzinho.ui;

import br.edu.unipampa.varzinho.application.ArchiveListing;
import br.edu.unipampa.varzinho.application.BrowseArchive;
import br.edu.unipampa.varzinho.application.CourtView;
import br.edu.unipampa.varzinho.application.HighlightView;
import br.edu.unipampa.varzinho.application.port.RecordingEditor;
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
import java.util.function.Consumer;
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
    void enableEditing(RecordingEditor editor) { actions.enableEditing(editor); }
    void deleteSelection(RecordingEditor editor, Predicate<HighlightView> confirm) {
        actions.deleteSelection(editor, confirm);
    }
    void refreshCourts() { filters.refreshCourts(); }
    static boolean playable(String path) { return RecordingActions.playable(path); }

    void onCount(IntConsumer listener) {
        onCount = listener;
        listener.accept(recordings.count());
    }

    /**
     * Reads the archive again for the chosen court and day. Damaged lines are reported
     * and the readable records shown; an unreadable date or any other failure is
     * reported and the rows are kept.
     */
    void refresh() {
        Optional<LocalDate> day;
        try {
            day = filters.chosenDay();
        } catch (DateTimeParseException unreadable) {
            errorSink.accept("Informe a data como dd/mm/aaaa.");
            return;
        }
        ArchiveListing listing;
        try {
            listing = lookup(day);
        } catch (RepositoryException failure) {
            LOGGER.log(Level.WARNING, "Could not read the highlight archive", failure);
            errorSink.accept("Não foi possível ler o acervo. Verifique o armazenamento.");
            return;
        }
        recordings.show(listing.getHighlights());
        if (filters.includesAll()) onCount.accept(recordings.count());
        actions.updateOpenClip();
        listing.getWarning().ifPresent(warning -> {
            LOGGER.warning(warning);
            errorSink.accept("Acervo danificado: " + PortugueseText.message(warning));
        });
    }

    private ArchiveListing lookup(Optional<LocalDate> day) throws RepositoryException {
        Optional<Integer> court = filters.courtNumber();
        if (court.isEmpty()) return day.isPresent() ? archive.byDay(day.get()) : archive.all();
        return day.isPresent() ? archive.byDay(day.get(), court.get()) : archive.byCourt(court.get());
    }

    void captureSaved() {
        if (!filters.includesAll()) {
            try {
                onCount.accept(archive.all().getHighlights().size());
            } catch (RepositoryException failure) {
                LOGGER.log(Level.WARNING, "Could not update archive count", failure);
            }
        }
        refresh();
    }
}
