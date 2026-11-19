package br.edu.unipampa.varzinho.ui;

import br.edu.unipampa.varzinho.application.ArchiveListing;
import br.edu.unipampa.varzinho.application.BrowseArchive;
import br.edu.unipampa.varzinho.application.CourtView;
import br.edu.unipampa.varzinho.application.HighlightView;
import br.edu.unipampa.varzinho.application.port.RepositoryException;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;
import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The <b>Acervo</b> page: saved highlights as table rows, found the way the gym looks
 * for them — by court and by day ("tonight, on court 2") — and opened in the system's
 * video player.
 *
 * <p>It fills itself from the archive when built, so highlights from earlier runs
 * show up at once. It never filters or sorts rows itself — choosing a court or a day
 * only chooses which {@link BrowseArchive} lookup to ask. A damaged archive still shows
 * the records that could be read, and a lookup that fails outright leaves the rows it
 * already had.
 *
 * <p>Whether a clip's file exists is checked here, not in the model: the domain asks
 * nothing of the disk, and a port for one presentation check would be more than the
 * question needs.
 */
final class ArchivePanel extends JPanel {
    private static final Logger LOGGER = Logger.getLogger(ArchivePanel.class.getName());
    private static final String ALL_COURTS = "Todas as quadras";
    private static final String ALL_DAYS = "Todos os dias";
    private static final String TODAY = "Hoje";
    private static final String YESTERDAY = "Ontem";
    private static final String CHOSEN_DATE = "Escolher data";
    private static final String NO_PLAYER = "Nenhum reprodutor de vídeo disponível para abrir o clipe.";
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);

    /** Opens a clip's file: the window's is the system player, a test's records the call. */
    @FunctionalInterface
    interface ClipPlayer {
        void open(Path clip) throws IOException;
    }

    private final Supplier<List<CourtView>> courts;
    private final BrowseArchive archive;
    private final Clock clock;
    private final ZoneId zone;
    private final ClipPlayer player;
    private final Consumer<String> errorSink;
    private final List<Integer> courtNumbers = new ArrayList<>();
    private final JComboBox<String> filter = new JComboBox<>();
    private final JComboBox<String> dayFilter = new JComboBox<>(new String[] {ALL_DAYS, TODAY, YESTERDAY, CHOSEN_DATE});
    private final JTextField dateField = new JTextField(8);
    private final HighlightTableModel rows;
    private final JTable table;
    private final JButton openClip = new JButton("Abrir clipe");

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
        this.courts = courts;
        this.archive = archive;
        this.clock = clock;
        this.zone = zone;
        this.player = player;
        this.errorSink = errorSink;
        this.rows = new HighlightTableModel(timeFormatIn(zone));
        this.table = new JTable(rows);
        filter.addItem(ALL_COURTS);
        refreshCourts();
        filter.addActionListener(event -> refresh());
        dateField.setToolTipText("dd/mm/aaaa");
        dateField.setEnabled(false);
        dayFilter.addActionListener(event -> {
            dateField.setEnabled(CHOSEN_DATE.equals(dayFilter.getSelectedItem()));
            // A chosen date starts from today, so the rows shown always match a day the filter names.
            if (dateField.isEnabled() && dateField.getText().isBlank()) {
                dateField.setText(DATE_FORMAT.format(LocalDate.now(clock.withZone(zone))));
            }
            refresh();
        });
        dateField.addActionListener(event -> refresh());
        JPanel filterRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        filterRow.add(new JLabel("Quadra:"));
        filterRow.add(filter);
        filterRow.add(new JLabel("Dia:"));
        filterRow.add(dayFilter);
        filterRow.add(dateField);
        openClip.setIcon(VarZinhoIcons.openClip());
        openClip.addActionListener(event -> openSelectedClip());
        filterRow.add(openClip);
        add(filterRow, BorderLayout.NORTH);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getSelectionModel().addListSelectionListener(event -> updateOpenClip());
        table.setPreferredScrollableViewportSize(new Dimension(640, 160));
        add(new JScrollPane(table), BorderLayout.CENTER);
        refresh();
    }

    /**
     * How the window writes a capture time, so the table and the save confirmation
     * show the same moment the same way.
     */
    static DateTimeFormatter timeFormatIn(ZoneId zone) {
        return DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss").withZone(zone);
    }

    JComboBox<String> filter() { return filter; }

    JComboBox<String> dayFilter() { return dayFilter; }

    JTextField dateField() { return dateField; }

    JTable table() { return table; }

    JButton openClipButton() { return openClip; }

    /**
     * Offers every court the gym has now as a filter, keeping the current choice. Called
     * when a court is added after the window opened.
     */
    void refreshCourts() {
        courts.get().stream().map(CourtView::getNumber)
                .filter(number -> !courtNumbers.contains(number))
                .forEach(number -> {
                    courtNumbers.add(number);
                    filter.addItem("Quadra " + number);
                });
    }

    /**
     * Reads the archive again for the chosen court and day. Damaged lines are reported
     * and the readable records shown; an unreadable date or any other failure is
     * reported and the rows are kept.
     */
    void refresh() {
        Optional<LocalDate> day;
        try {
            day = chosenDay();
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
        rows.show(listing.getHighlights());
        updateOpenClip();
        listing.getWarning().ifPresent(warning -> {
            LOGGER.warning(warning);
            errorSink.accept("Acervo danificado: " + PortugueseText.message(warning));
        });
    }

    private Optional<LocalDate> chosenDay() {
        LocalDate today = LocalDate.now(clock.withZone(zone));
        Object chosen = dayFilter.getSelectedItem();
        if (TODAY.equals(chosen)) return Optional.of(today);
        if (YESTERDAY.equals(chosen)) return Optional.of(today.minusDays(1));
        if (CHOSEN_DATE.equals(chosen)) return Optional.of(LocalDate.parse(dateField.getText().trim(), DATE_FORMAT));
        return Optional.empty();
    }

    private ArchiveListing lookup(Optional<LocalDate> day) throws RepositoryException {
        int chosen = filter.getSelectedIndex();
        if (chosen <= 0) return day.isPresent() ? archive.byDay(day.get()) : archive.all();
        int court = courtNumbers.get(chosen - 1);
        return day.isPresent() ? archive.byDay(day.get(), court) : archive.byCourt(court);
    }

    // Only a stream camera writes a real file; the other kinds name one that nothing
    // ever writes, so a missing file is expected and said plainly rather than failed on.
    private void updateOpenClip() {
        Optional<Path> clip = selectedClip();
        boolean exists = clip.filter(Files::exists).isPresent();
        openClip.setEnabled(exists);
        if (clip.isEmpty()) openClip.setToolTipText("Selecione um lance para abrir o clipe.");
        else if (!exists) openClip.setToolTipText("Este lance não tem arquivo de vídeo.");
        else openClip.setToolTipText(null);
    }

    private Optional<Path> selectedClip() {
        int row = table.getSelectedRow();
        if (row < 0) return Optional.empty();
        try {
            return Optional.of(Path.of(rows.clipAt(table.convertRowIndexToModel(row))));
        } catch (InvalidPathException unusable) {
            LOGGER.log(Level.FINE, "A clip path cannot name a file on this system", unusable);
            return Optional.empty();
        }
    }

    private void openSelectedClip() {
        Optional<Path> clip = selectedClip().filter(Files::exists);
        if (clip.isEmpty()) {
            // The file went away after the row was chosen.
            updateOpenClip();
            return;
        }
        try {
            player.open(clip.get());
        } catch (IOException | UnsupportedOperationException | IllegalArgumentException failure) {
            LOGGER.log(Level.WARNING, "Could not open the clip in the system player", failure);
            errorSink.accept(NO_PLAYER);
        }
    }

    private static final class HighlightTableModel extends AbstractTableModel {
        private static final String[] COLUMNS = {"Data e hora", "Quadra", "Câmera", "Duração", "Clipe"};

        private final DateTimeFormatter timeFormat;
        private List<HighlightView> highlights = List.of();

        String clipAt(int row) {
            return highlights.get(row).getClipPath();
        }

        HighlightTableModel(DateTimeFormatter timeFormat) {
            this.timeFormat = timeFormat;
        }

        void show(List<HighlightView> highlights) {
            this.highlights = List.copyOf(highlights);
            fireTableDataChanged();
        }

        @Override
        public int getRowCount() { return highlights.size(); }

        @Override
        public int getColumnCount() { return COLUMNS.length; }

        @Override
        public String getColumnName(int column) { return COLUMNS[column]; }

        @Override
        public Object getValueAt(int row, int column) {
            HighlightView highlight = highlights.get(row);
            switch (column) {
                case 0: return timeFormat.format(highlight.getCapturedAt());
                case 1: return highlight.getCourtNumber();
                case 2: return highlight.getCameraId();
                case 3: return highlight.getDurationSeconds() + " s";
                case 4: return highlight.getClipPath();
                default: throw new IndexOutOfBoundsException("no column " + column);
            }
        }
    }
}
