package br.edu.unipampa.varzinho.ui;

import br.edu.unipampa.varzinho.domain.highlight.Highlight;
import br.edu.unipampa.varzinho.domain.structure.Court;
import br.edu.unipampa.varzinho.domain.structure.Gym;
import br.edu.unipampa.varzinho.exception.RepositoryException;
import br.edu.unipampa.varzinho.repository.HighlightRepository;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

/**
 * The archive: every saved highlight as a table row, narrowed to one court on request.
 *
 * <p>It fills itself from the repository when built, so highlights from earlier runs
 * show up at once. It never filters rows itself — choosing a court only chooses which
 * repository lookup to ask — and a lookup that fails leaves the rows it already had.
 */
final class ArchivePanel extends JPanel {
    private static final String ALL_COURTS = "All courts";

    private final HighlightRepository repository;
    private final Consumer<String> errorSink;
    private final List<Integer> courtNumbers = new ArrayList<>();
    private final JComboBox<String> filter = new JComboBox<>();
    private final HighlightTableModel rows;
    private final JTable table;

    ArchivePanel(Gym gym, HighlightRepository repository, ZoneId zone, Consumer<String> errorSink) {
        super(new BorderLayout(4, 4));
        if (gym == null) throw new IllegalArgumentException("an archive panel needs a gym");
        if (repository == null) throw new IllegalArgumentException("an archive panel needs a highlight repository");
        if (zone == null) throw new IllegalArgumentException("an archive panel needs a time zone to show times in");
        if (errorSink == null) throw new IllegalArgumentException("an archive panel needs somewhere to report errors");
        this.repository = repository;
        this.errorSink = errorSink;
        this.rows = new HighlightTableModel(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(zone));
        this.table = new JTable(rows);
        filter.addItem(ALL_COURTS);
        for (Court court : gym.getCourts()) {
            courtNumbers.add(court.getNumber());
            filter.addItem("Court " + court.getNumber());
        }
        filter.addActionListener(event -> refresh());
        JPanel filterRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        filterRow.add(new JLabel("Archive:"));
        filterRow.add(filter);
        add(filterRow, BorderLayout.NORTH);
        table.setPreferredScrollableViewportSize(new Dimension(640, 160));
        add(new JScrollPane(table), BorderLayout.CENTER);
        refresh();
    }

    JComboBox<String> filter() { return filter; }

    JTable table() { return table; }

    /** Reads the archive again for the chosen filter; on failure reports it and keeps the rows. */
    void refresh() {
        try {
            List<Highlight> found = new ArrayList<>(lookup());
            found.sort(Comparator.comparing(Highlight::getCapturedAt).reversed());
            rows.show(found);
        } catch (RepositoryException failure) {
            errorSink.accept("Archive error: " + failure.getMessage());
        }
    }

    private List<Highlight> lookup() throws RepositoryException {
        int chosen = filter.getSelectedIndex();
        if (chosen <= 0) return repository.findAll();
        return repository.findByCourt(courtNumbers.get(chosen - 1));
    }

    private static final class HighlightTableModel extends AbstractTableModel {
        private static final String[] COLUMNS = {"Captured at", "Court", "Camera", "Duration", "Clip"};

        private final DateTimeFormatter timeFormat;
        private List<Highlight> highlights = List.of();

        HighlightTableModel(DateTimeFormatter timeFormat) {
            this.timeFormat = timeFormat;
        }

        void show(List<Highlight> highlights) {
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
            Highlight highlight = highlights.get(row);
            switch (column) {
                case 0: return timeFormat.format(highlight.getCapturedAt());
                case 1: return highlight.getCourtNumber();
                case 2: return highlight.getCameraId();
                case 3: return highlight.getClip().getDurationSeconds() + "s";
                case 4: return highlight.getClip().getFilePath();
                default: throw new IndexOutOfBoundsException("no column " + column);
            }
        }
    }
}
