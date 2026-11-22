package br.edu.unipampa.varzinho.ui;

import br.edu.unipampa.varzinho.application.ArchiveListing;
import br.edu.unipampa.varzinho.application.BrowseArchive;
import br.edu.unipampa.varzinho.application.CourtView;
import br.edu.unipampa.varzinho.application.HighlightView;
import br.edu.unipampa.varzinho.application.port.RepositoryException;
import br.edu.unipampa.varzinho.application.port.RecordingEditor;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import java.io.IOException;
import java.awt.Desktop;
import javax.swing.KeyStroke;
import javax.swing.JOptionPane;
import javax.swing.RowFilter;
import javax.swing.table.TableRowSorter;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
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
import java.util.HashSet;
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
 * <p>{@link BrowseArchive} supplies ordered results for the chosen court and day.
 * Text searches narrow those results locally, including session-only category edits.
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
    private final JButton openClip = new JButton("Assistir ao vídeo");
    private final JLabel empty = new JLabel("Nenhum lance encontrado.", JLabel.CENTER);
    private final JTextField search = new JTextField(12);
    private final JPanel footer = new JPanel(new BorderLayout());
    private java.util.function.IntConsumer onCount = count -> { };

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
        VarZinhoTheme.styleCard(this);
        this.rows = new HighlightTableModel(timeFormatIn(zone));
        this.table = new JTable(rows);
        TableRowSorter<HighlightTableModel> sorter = new TableRowSorter<>(rows);
        sorter.setSortsOnUpdates(true);
        for (int column = 0; column < rows.getColumnCount(); column++) sorter.setSortable(column, false);
        table.setRowSorter(sorter);
        table.getColumnModel().getColumn(4).setCellEditor(new javax.swing.DefaultCellEditor(
                new JComboBox<>(new String[]{"Lance", "Defesa", "Gol", "Ponto"})));
        Runnable applyFilters = () -> {
            String query = search.getText().strip().toLowerCase(java.util.Locale.ROOT);
            sorter.setRowFilter(query.isEmpty() ? null : new RowFilter<>() {
                @Override public boolean include(Entry<? extends HighlightTableModel, ? extends Integer> entry) {
                    for (int column = 0; column < entry.getValueCount(); column++) {
                        String value = entry.getStringValue(column);
                        if (column == 1) value = "Quadra " + value;
                        if (value.toLowerCase(java.util.Locale.ROOT).contains(query)) return true;
                    }
                    return false;
                }
            });
        };
        search.putClientProperty("JTextField.placeholderText", "Buscar gravação");
        search.putClientProperty("JTextField.showClearButton", true);
        search.setToolTipText("Buscar gravação (Ctrl+F). Enter seleciona o primeiro resultado.");
        search.getAccessibleContext().setAccessibleName("Buscar gravação");
        search.addActionListener(event -> {
            if (table.getRowCount() == 0) return;
            table.setRowSelectionInterval(0, 0);
            table.scrollRectToVisible(table.getCellRect(0, 0, true));
            table.requestFocusInWindow();
        });
        registerKeyboardAction(event -> {
            search.requestFocusInWindow();
            search.selectAll();
        }, KeyStroke.getKeyStroke("ctrl F"), WHEN_IN_FOCUSED_WINDOW);
        search.registerKeyboardAction(event -> search.setText(""), KeyStroke.getKeyStroke("ESCAPE"), WHEN_FOCUSED);
        table.registerKeyboardAction(event -> previewSelectedClip(), KeyStroke.getKeyStroke("ctrl ENTER"), WHEN_FOCUSED);
        openClip.setToolTipText("Abrir o lance selecionado (Ctrl+Enter na tabela)");
        table.getAccessibleContext().setAccessibleName("Histórico de melhores momentos");
        search.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { applyFilters.run(); }
            public void removeUpdate(DocumentEvent e) { applyFilters.run(); }
            public void changedUpdate(DocumentEvent e) { applyFilters.run(); }
        });
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
        AdaptivePair heading = new AdaptivePair(780, "[grow,fill][]");
        JPanel titles = new JPanel(new java.awt.GridLayout(2, 1));
        titles.setOpaque(false);
        JLabel eyebrow = new JLabel("GRAVAÇÕES RECENTES");
        VarZinhoTheme.styleEyebrow(eyebrow);
        JLabel title = new JLabel("Histórico de melhores momentos");
        VarZinhoTheme.styleTitle(title);
        titles.add(eyebrow);
        titles.add(title);
        heading.add(titles);
        JPanel filterRow = new JPanel(new FlowLayout(FlowLayout.LEADING, 6, 0));
        filterRow.setOpaque(false);
        filterRow.add(search);
        filterRow.add(filter);
        filterRow.add(new JLabel("Dia:"));
        filterRow.add(dayFilter);
        filterRow.add(dateField);
        openClip.setIcon(VarZinhoIcons.openClip());
        openClip.addActionListener(event -> openSelectedClip());
        filterRow.add(openClip);
        heading.add(filterRow);
        add(heading, BorderLayout.NORTH);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getSelectionModel().addListSelectionListener(event -> updateOpenClip());
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
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                int column = table.columnAtPoint(e.getPoint());
                int row = table.rowAtPoint(e.getPoint());
                if (javax.swing.SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 1
                        && column >= 0 && table.convertColumnIndexToModel(column) == 5 && row >= 0) {
                    table.setRowSelectionInterval(row, row);
                    previewSelectedClip();
                }
            }
        });
        JScrollPane scroll = new JScrollPane(table);
        scroll.setColumnHeaderView(table.getTableHeader());
        scroll.setBorder(javax.swing.BorderFactory.createEmptyBorder());
        add(scroll, BorderLayout.CENTER);
        footer.setOpaque(false);
        footer.add(empty, BorderLayout.CENTER);
        add(footer, BorderLayout.SOUTH);
        sorter.addRowSorterListener(event -> empty.setVisible(table.getRowCount() == 0));
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
    JTextField searchField() { return search; }
    void onCount(java.util.function.IntConsumer listener) {
        onCount = listener;
        listener.accept(rows.getRowCount());
    }

    JButton openClipButton() { return openClip; }

    void enableEditing(RecordingEditor editor) {
        JButton rename = new JButton("Renomear gravação");
        JButton delete = new JButton("Excluir gravação");
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.TRAILING));
        actions.setOpaque(false);
        actions.add(rename);
        actions.add(delete);
        footer.add(actions, BorderLayout.EAST);
        Runnable update = () -> {
            boolean oneSelected = table.getSelectedRowCount() == 1;
            rename.setEnabled(oneSelected);
            delete.setEnabled(oneSelected);
        };
        table.getSelectionModel().addListSelectionListener(event -> update.run());
        update.run();
        rename.addActionListener(event -> renameSelection(editor));
        delete.addActionListener(event -> deleteSelection(editor, this::confirmDeletion));
    }

    private void renameSelection(RecordingEditor editor) {
        if (table.getSelectedRowCount() != 1) return;
        HighlightView item = selectedRecording();
        JTextField name = new JTextField(item.getTitle(), 26);
        name.getAccessibleContext().setAccessibleName("Nome da gravação");
        Object[] options = {"Salvar nome", "Cancelar"};
        int answer = JOptionPane.showOptionDialog(this, new Object[]{"Nome da gravação", name}, "Renomear gravação",
                JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[0]);
        if (answer != 0) return;
        try {
            editor.rename(item.getId(), name.getText());
            refresh();
        } catch (IllegalArgumentException invalid) {
            errorSink.accept("Informe um nome em uma única linha.");
        } catch (RepositoryException failure) {
            LOGGER.log(Level.WARNING, "Could not rename recording", failure);
            errorSink.accept("Não foi possível renomear a gravação. Verifique o acervo.");
        }
    }

    private boolean confirmDeletion(HighlightView item) {
        String label = item.getTitle().isEmpty() ? item.getId() : item.getTitle();
        String prompt = "Tem certeza de que deseja excluir “" + label + "”?\nQuadra " + item.getCourtNumber()
                + " · " + item.getCameraId() + " · " + rows.timeFormat.format(item.getCapturedAt())
                + "\nO lance será removido do acervo e o MP4 original será apagado, se existir.";
        Object[] options = {"Cancelar", "Excluir gravação"};
        return JOptionPane.showOptionDialog(this, prompt, "Confirmar exclusão", JOptionPane.DEFAULT_OPTION,
                JOptionPane.WARNING_MESSAGE, null, options, options[0]) == 1;
    }

    private HighlightView selectedRecording() {
        return rows.highlights.get(table.convertRowIndexToModel(table.getSelectedRow()));
    }

    void deleteSelection(RecordingEditor editor, java.util.function.Predicate<HighlightView> confirm) {
        if (table.getSelectedRowCount() != 1) return;
        HighlightView item = selectedRecording();
        if (!confirm.test(item)) return;
        try {
            editor.delete(item.getId());
            rows.categories.remove(item.getId());
            captureSaved();
        } catch (RepositoryException failure) {
            LOGGER.log(Level.WARNING, "Could not delete recording", failure);
            errorSink.accept("Não foi possível excluir a gravação. Verifique o acervo e o arquivo de vídeo.");
        }
    }

    // Only a stream camera writes a real file; the other kinds name one that nothing
    // ever writes, so a missing file is expected and said plainly rather than failed on.
    private void previewSelectedClip() {
        int row = table.getSelectedRow();
        if (row < 0) {
            errorSink.accept("Selecione um lance no acervo para abrir o clipe.");
            return;
        }
        String clip = rows.clipAt(table.convertRowIndexToModel(row));
        if (!playable(clip)) {
            errorSink.accept("Não há arquivo de vídeo em " + clip
                    + ": somente lances de uma câmera de transmissão são gravados no disco.");
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
        if (table.isEditing() && !table.getCellEditor().stopCellEditing()) table.getCellEditor().cancelCellEditing();
        var selectedIds = new HashSet<String>();
        for (int row : table.getSelectedRows()) {
            selectedIds.add(rows.highlights.get(table.convertRowIndexToModel(row)).getId());
        }
        rows.show(listing.getHighlights());
        if (!selectedIds.isEmpty()) {
            for (int row = 0; row < table.getRowCount(); row++) {
                String id = rows.highlights.get(table.convertRowIndexToModel(row)).getId();
                if (selectedIds.contains(id)) table.addRowSelectionInterval(row, row);
            }
        }
        empty.setVisible(table.getRowCount() == 0);
        if (filter.getSelectedIndex() <= 0 && ALL_DAYS.equals(dayFilter.getSelectedItem())) onCount.accept(rows.getRowCount());
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

    void captureSaved() {
        if (filter.getSelectedIndex() > 0 || !ALL_DAYS.equals(dayFilter.getSelectedItem())) {
            try {
                onCount.accept(archive.all().getHighlights().size());
            } catch (RepositoryException failure) {
                LOGGER.log(Level.WARNING, "Could not update archive count", failure);
            }
        }
        refresh();
    }

    static boolean playable(String path) {
        try { return path.toLowerCase(java.util.Locale.ROOT).endsWith(".mp4") && Files.isRegularFile(Path.of(path)); }
        catch (java.nio.file.InvalidPathException invalid) { return false; }
    }

    private static final class HighlightTableModel extends AbstractTableModel {
        private static final String[] COLUMNS = {"Data e hora", "Quadra", "Câmera", "Duração", "Categoria", "Clipe", "Nome"};
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
        private java.util.Set<String> playablePaths = java.util.Set.of();

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
                    .collect(java.util.stream.Collectors.toSet());
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
