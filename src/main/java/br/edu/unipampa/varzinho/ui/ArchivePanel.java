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
import javax.swing.table.AbstractTableModel;
import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * The archive: every saved highlight as a table row, narrowed to one court on request.
 *
 * <p>It fills itself from the archive when built, so highlights from earlier runs
 * show up at once. It never filters or sorts rows itself — choosing a court only chooses
 * which {@link BrowseArchive} lookup to ask. A damaged archive still shows the records
 * that could be read, and a lookup that fails outright leaves the rows it already had.
 */
final class ArchivePanel extends JPanel {
    private static final String ALL_COURTS = "All courts";

    private final Supplier<List<CourtView>> courts;
    private final BrowseArchive archive;
    private final Consumer<String> errorSink;
    private final List<Integer> courtNumbers = new ArrayList<>();
    private final JComboBox<String> filter = new JComboBox<>();
    private final HighlightTableModel rows;
    private final JTable table;
    private final JButton openClip = new JButton("Open clip");

    ArchivePanel(Supplier<List<CourtView>> courts, BrowseArchive archive, ZoneId zone, Consumer<String> errorSink) {
        super(new BorderLayout(4, 4));
        if (courts == null) throw new IllegalArgumentException("an archive panel needs the gym's courts");
        if (archive == null) throw new IllegalArgumentException("an archive panel needs the archive to browse");
        if (zone == null) throw new IllegalArgumentException("an archive panel needs a time zone to show times in");
        if (errorSink == null) throw new IllegalArgumentException("an archive panel needs somewhere to report errors");
        this.courts = courts;
        this.archive = archive;
        this.errorSink = errorSink;
        this.rows = new HighlightTableModel(timeFormatIn(zone));
        this.table = new JTable(rows);
        filter.addItem(ALL_COURTS);
        refreshCourts();
        filter.addActionListener(event -> refresh());
        JPanel filterRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        filterRow.add(new JLabel("Archive:"));
        filterRow.add(filter);
        openClip.addActionListener(event -> openSelectedClip());
        filterRow.add(openClip);
        add(filterRow, BorderLayout.NORTH);
        table.setPreferredScrollableViewportSize(new Dimension(640, 160));
        add(new JScrollPane(table), BorderLayout.CENTER);
        refresh();
    }

    /**
     * How the window writes a capture time, so the table and the save confirmation
     * show the same moment the same way.
     */
    static DateTimeFormatter timeFormatIn(ZoneId zone) {
        return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(zone);
    }

    JComboBox<String> filter() { return filter; }

    JTable table() { return table; }

    JButton openClipButton() { return openClip; }

    // Only a stream camera writes a real file; the other kinds name one that nothing
    // ever writes, so a missing file is expected and said plainly rather than failed on.
    private void openSelectedClip() {
        int row = table.getSelectedRow();
        if (row < 0) {
            errorSink.accept("Choose a highlight in the archive to open its clip.");
            return;
        }
        Path clip = Path.of(rows.clipAt(table.convertRowIndexToModel(row)));
        if (!Files.exists(clip)) {
            errorSink.accept("There is no video file at " + clip
                    + ": only highlights from a stream camera are recorded to disk.");
            return;
        }
        try {
            Desktop.getDesktop().open(clip.toFile());
        } catch (IOException | UnsupportedOperationException failure) {
            errorSink.accept("The clip could not be opened: " + failure.getMessage());
        }
    }

    /**
     * Offers every court the gym has now as a filter, keeping the current choice. Called
     * when a court is added after the window opened.
     */
    void refreshCourts() {
        courts.get().stream().map(CourtView::getNumber)
                .filter(number -> !courtNumbers.contains(number))
                .forEach(number -> {
                    courtNumbers.add(number);
                    filter.addItem("Court " + number);
                });
    }

    /**
     * Reads the archive again for the chosen filter. Damaged lines are reported and the
     * readable records shown; any other failure is reported and the rows are kept.
     */
    void refresh() {
        ArchiveListing listing;
        try {
            listing = lookup();
        } catch (RepositoryException failure) {
            errorSink.accept("Archive error: " + failure.getMessage());
            return;
        }
        rows.show(listing.getHighlights());
        listing.getWarning().ifPresent(warning -> errorSink.accept("Archive damaged: " + warning));
    }

    private ArchiveListing lookup() throws RepositoryException {
        int chosen = filter.getSelectedIndex();
        if (chosen <= 0) return archive.all();
        return archive.byCourt(courtNumbers.get(chosen - 1));
    }

    private static final class HighlightTableModel extends AbstractTableModel {
        private static final String[] COLUMNS = {"Captured at", "Court", "Camera", "Duration", "Clip"};

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
                case 3: return highlight.getDurationSeconds() + "s";
                case 4: return highlight.getClipPath();
                default: throw new IndexOutOfBoundsException("no column " + column);
            }
        }
    }
}
