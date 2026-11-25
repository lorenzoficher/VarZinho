package br.edu.unipampa.varzinho.ui;

import br.edu.unipampa.varzinho.application.HighlightView;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableRowSorter;

/** Presents recordings and keeps selection stable across searches and reloads. */
final class ArchiveTable extends JPanel {
    private final HighlightTableModel rows;
    private final JTable table;
    private final TableRowSorter<HighlightTableModel> sorter;
    private final JLabel empty = new JLabel("Nenhum lance encontrado.", JLabel.CENTER);
    private final JPanel footer = new JPanel(new BorderLayout());

    ArchiveTable(DateTimeFormatter format) {
        super(new BorderLayout());
        rows = new HighlightTableModel(format);
        this.table = new JTable(rows);
        TabKey.leaves(table);
        sorter = new TableRowSorter<>(rows);
        sorter.setSortsOnUpdates(true);
        for (int column = 0; column < rows.getColumnCount(); column++) sorter.setSortable(column, false);
        table.setRowSorter(sorter);
        table.getColumnModel().getColumn(4).setCellEditor(new DefaultCellEditor(
                new JComboBox<>(new String[]{"Lance", "Defesa", "Gol", "Ponto"})));
        table.getAccessibleContext().setAccessibleName("Histórico de melhores momentos");
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setPreferredScrollableViewportSize(new Dimension(640, 160));
        table.setRowHeight(38);
        table.setShowHorizontalLines(true);
        table.setShowVerticalLines(false);
        table.setFillsViewportHeight(true);
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setPreferredSize(new Dimension(0, 36));
        table.getColumnModel().getColumn(5).setCellRenderer((grid, value, selected, focus, row, column) -> {
            boolean available = rows.hasVideo(table.convertRowIndexToModel(row));
            JButton play = new JButton(available ? "Assistir" : "Sem MP4");
            if (available) play.setIcon(VarZinhoIcons.arena("play-video", 18, VarZinhoTheme.ON_ACCENT));
            play.setEnabled(available);
            play.setToolTipText(String.valueOf(value));
            return play;
        });
        JScrollPane scroll = new JScrollPane(table);
        scroll.setColumnHeaderView(table.getTableHeader());
        scroll.setBorder(BorderFactory.createEmptyBorder());
        add(scroll, BorderLayout.CENTER);
        footer.setOpaque(false);
        footer.add(empty, BorderLayout.CENTER);
        add(footer, BorderLayout.SOUTH);
        sorter.addRowSorterListener(event -> empty.setVisible(table.getRowCount() == 0));
    }

    void onPreview(Runnable preview) {
        table.registerKeyboardAction(event -> preview.run(), KeyStroke.getKeyStroke("ctrl ENTER"), WHEN_FOCUSED);
        table.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                int column = table.columnAtPoint(e.getPoint());
                int row = table.rowAtPoint(e.getPoint());
                if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 1
                        && column >= 0 && table.convertColumnIndexToModel(column) == 5 && row >= 0) {
                    table.setRowSelectionInterval(row, row);
                    preview.run();
                }
            }
        });
    }

    void search(String text) {
        String query = text.strip().toLowerCase(Locale.ROOT);
        sorter.setRowFilter(query.isEmpty() ? null : new RowFilter<>() {
            @Override public boolean include(Entry<? extends HighlightTableModel, ? extends Integer> entry) {
                for (int column = 0; column < entry.getValueCount(); column++) {
                    String value = entry.getStringValue(column);
                    if (column == 1) value = "Quadra " + value;
                    if (value.toLowerCase(Locale.ROOT).contains(query)) return true;
                }
                return false;
            }
        });
    }

    void selectFirst() {
        if (table.getRowCount() == 0) return;
        table.setRowSelectionInterval(0, 0);
        table.scrollRectToVisible(table.getCellRect(0, 0, true));
        table.requestFocusInWindow();
    }

    JTable table() { return table; }
    int count() { return rows.getRowCount(); }
    void addActions(JPanel actions) { footer.add(actions, BorderLayout.EAST); }
    HighlightView selectedRecording() { return rows.highlights.get(table.convertRowIndexToModel(table.getSelectedRow())); }
    void forgetCategory(String id) { rows.categories.remove(id); }

    void show(List<HighlightView> highlights) {
        if (table.isEditing() && !table.getCellEditor().stopCellEditing()) table.getCellEditor().cancelCellEditing();
        var selectedIds = new HashSet<String>();
        for (int row : table.getSelectedRows()) {
            selectedIds.add(rows.highlights.get(table.convertRowIndexToModel(row)).getId());
        }
        rows.show(highlights);
        if (!selectedIds.isEmpty()) {
            for (int row = 0; row < table.getRowCount(); row++) {
                String id = rows.highlights.get(table.convertRowIndexToModel(row)).getId();
                if (selectedIds.contains(id)) table.addRowSelectionInterval(row, row);
            }
        }
        empty.setVisible(table.getRowCount() == 0);
    }

    private static final class HighlightTableModel extends AbstractTableModel {
        private static final String[] COLUMNS = {"Data e hora", "Quadra", "Câmera", "Duração", "Categoria", "Clipe", "Nome"};
        private final Map<String,String> categories = new HashMap<>();

        @Override public boolean isCellEditable(int row, int column) { return column == 4; }

        @Override public void setValueAt(Object value, int row, int column) {
            if (column == 4 && value instanceof String category) {
                categories.put(highlights.get(row).getId(), category);
                fireTableCellUpdated(row, column);
            }
        }

        private final DateTimeFormatter timeFormat;
        private List<HighlightView> highlights = List.of();
        private Set<String> playablePaths = Set.of();

        boolean hasVideo(int row) { return playablePaths.contains(clipAt(row)); }

        String clipAt(int row) {
            return highlights.get(row).getClipPath();
        }

        HighlightTableModel(DateTimeFormatter timeFormat) {
            this.timeFormat = timeFormat;
        }

        void show(List<HighlightView> highlights) {
            this.highlights = List.copyOf(highlights);
            playablePaths = highlights.stream().map(HighlightView::getClipPath).filter(ArchivePanel::playable)
                    .collect(Collectors.toSet());
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
                case 6: return highlight.getTitle().isEmpty() ? "Sem nome" : highlight.getTitle();
                default: throw new IndexOutOfBoundsException("no column " + column);
            }
        }
    }
}
