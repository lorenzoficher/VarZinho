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
import javax.swing.JToggleButton;
import javax.swing.RowFilter;
import javax.swing.table.TableRowSorter;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
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
    private static final String ALL_COURTS = "Todas as quadras";

    private final Supplier<List<CourtView>> courts;
    private final BrowseArchive archive;
    private final Consumer<String> errorSink;
    private final List<Integer> courtNumbers = new ArrayList<>();
    private final JComboBox<String> filter = new JComboBox<>();
    private final HighlightTableModel rows;
    private final JTable table;
    private final JButton openClip = new JButton("Assistir ao vídeo");
    private final JTextField search = new JTextField(12);
    private final JToggleButton today = new JToggleButton("Hoje");
    private java.util.function.IntConsumer onCount = count -> { };

    ArchivePanel(Supplier<List<CourtView>> courts, BrowseArchive archive, ZoneId zone, Consumer<String> errorSink) {
        super(new BorderLayout(4, 4));
        if (courts == null) throw new IllegalArgumentException("an archive panel needs the gym's courts");
        if (archive == null) throw new IllegalArgumentException("an archive panel needs the archive to browse");
        if (zone == null) throw new IllegalArgumentException("an archive panel needs a time zone to show times in");
        if (errorSink == null) throw new IllegalArgumentException("an archive panel needs somewhere to report errors");
        this.courts = courts;
        this.archive = archive;
        this.errorSink = errorSink;
        VarZinhoTheme.styleCard(this);
        this.rows = new HighlightTableModel(timeFormatIn(zone));
        this.table = new JTable(rows);
        TableRowSorter<HighlightTableModel> sorter = new TableRowSorter<>(rows);
        table.setRowSorter(sorter);
        table.getColumnModel().getColumn(4).setCellEditor(new javax.swing.DefaultCellEditor(
                new JComboBox<>(new String[]{"Lance", "Defesa", "Gol", "Ponto"})));
        Runnable applyFilters = () -> sorter.setRowFilter(new RowFilter<>() {
            public boolean include(Entry<? extends HighlightTableModel, ? extends Integer> entry) {
                HighlightView item = rows.highlights.get(entry.getIdentifier());
                String keywords = ("Quadra " + item.getCourtNumber() + " " + item.getCameraId()
                        + " " + item.getClipPath()).toLowerCase(java.util.Locale.ROOT);
                return keywords.contains(search.getText().strip().toLowerCase(java.util.Locale.ROOT))
                        && (!today.isSelected() || item.getCapturedAt().atZone(zone).toLocalDate()
                        .equals(java.time.LocalDate.now(zone)));
            }
        });
        search.putClientProperty("JTextField.placeholderText", "Buscar gravação");
        search.getAccessibleContext().setAccessibleName("Buscar gravação");
        search.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { applyFilters.run(); }
            public void removeUpdate(DocumentEvent e) { applyFilters.run(); }
            public void changedUpdate(DocumentEvent e) { applyFilters.run(); }
        });
        today.addActionListener(event -> applyFilters.run());
        filter.addItem(ALL_COURTS);
        refreshCourts();
        filter.addActionListener(event -> refresh());
        JPanel heading = new JPanel(new BorderLayout(8, 8));
        heading.setOpaque(false);
        JPanel titles = new JPanel(new java.awt.GridLayout(2, 1));
        titles.setOpaque(false);
        JLabel eyebrow = new JLabel("GRAVAÇÕES RECENTES");
        VarZinhoTheme.styleEyebrow(eyebrow);
        JLabel title = new JLabel("Histórico de melhores momentos");
        VarZinhoTheme.styleTitle(title);
        titles.add(eyebrow);
        titles.add(title);
        heading.add(titles, BorderLayout.WEST);
        JPanel filterRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        filterRow.setOpaque(false);
        filterRow.add(search);
        filterRow.add(today);
        filterRow.add(filter);
        openClip.addActionListener(event -> openSelectedClip());
        filterRow.add(openClip);
        heading.add(filterRow, BorderLayout.EAST);
        add(heading, BorderLayout.NORTH);
        table.setPreferredScrollableViewportSize(new Dimension(640, 160));
        table.setRowHeight(38);
        table.setShowHorizontalLines(true);
        table.setShowVerticalLines(false);
        table.setFillsViewportHeight(true);
        table.getColumnModel().getColumn(5).setCellRenderer((grid, value, selected, focus, row, column) -> {
            JButton play = new JButton("▷  Assistir");
            play.setToolTipText(String.valueOf(value));
            return play;
        });
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                int column = table.columnAtPoint(e.getPoint());
                if (column >= 0 && table.convertColumnIndexToModel(column) == 5
                        && table.rowAtPoint(e.getPoint()) >= 0) openSelectedClip();
            }
        });
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

    JTable table() { return table; }
    JTextField searchField() { return search; }
    void onCount(java.util.function.IntConsumer listener) {
        onCount = listener;
        listener.accept(rows.getRowCount());
    }

    JButton openClipButton() { return openClip; }

    // Only a stream camera writes a real file; the other kinds name one that nothing
    // ever writes, so a missing file is expected and said plainly rather than failed on.
    private void openSelectedClip() {
        int row = table.getSelectedRow();
        if (row < 0) {
            errorSink.accept("Selecione um destaque no acervo para abrir o clipe.");
            return;
        }
        Path clip = Path.of(rows.clipAt(table.convertRowIndexToModel(row)));
        if (!Files.isRegularFile(clip) || !clip.toString().toLowerCase(java.util.Locale.ROOT).endsWith(".mp4")) {
            errorSink.accept("Não há arquivo de vídeo em " + clip
                    + ": somente destaques de uma câmera de transmissão são gravados no disco.");
            return;
        }
        ClipDialog.open(this, rows.highlights.get(table.convertRowIndexToModel(row)), rows.timeFormat);
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
                    filter.addItem("Quadra " + number);
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
            errorSink.accept("Erro no acervo: " + failure.getMessage());
            return;
        }
        rows.show(listing.getHighlights());
        if (filter.getSelectedIndex() <= 0) onCount.accept(rows.getRowCount());
        listing.getWarning().ifPresent(warning -> errorSink.accept("Acervo danificado: " + warning));
    }

    private ArchiveListing lookup() throws RepositoryException {
        int chosen = filter.getSelectedIndex();
        if (chosen <= 0) return archive.all();
        return archive.byCourt(courtNumbers.get(chosen - 1));
    }

    private static final class HighlightTableModel extends AbstractTableModel {
        private static final String[] COLUMNS = {"Data e hora", "Quadra", "Câmera", "Duração", "Categoria", "Clipe"};
        private final java.util.Map<String,String> categories = new java.util.HashMap<>();

        @Override public boolean isCellEditable(int row, int column) { return column == 4; }

        @Override public void setValueAt(Object value, int row, int column) {
            if (column == 4 && value instanceof String category) {
                categories.put(highlights.get(row).getId(), category);
                fireTableCellUpdated(row, column);
            }
        }

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
                case 4: return categories.getOrDefault(highlight.getId(), "Lance");
                case 5: return highlight.getClipPath();
                default: throw new IndexOutOfBoundsException("no column " + column);
            }
        }
    }
}
