package br.edu.unipampa.varzinho.ui;

import br.edu.unipampa.varzinho.application.CameraView;
import br.edu.unipampa.varzinho.application.CameraKind;
import br.edu.unipampa.varzinho.application.ManageGym;
import br.edu.unipampa.varzinho.application.port.RepositoryException;
import net.miginfocom.swing.MigLayout;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.Component;
import java.awt.event.KeyEvent;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.IntSupplier;
import java.util.stream.Collectors;

/** Shows each camera as an operational row with actions backed by gym management. */
final class CameraListPanel extends JPanel {
    private static final List<String> COLUMNS = List.of("Câmera", "Tipo", "Resolução", "Status");
    private static final int STATUS_COLUMN = 3;
    private final ManageGym management;
    private final GymChanges changes;
    private final IntSupplier selectedCourt;
    private final Consumer<String> errorSink;
    private final CameraTableModel cameraRows = new CameraTableModel();
    private final JTable cameraTable = new JTable(cameraRows);
    private final JPanel rows = new JPanel(new MigLayout("insets 0, fillx, wrap 1", "[grow,fill]", "[]"));
    private final JButton start = new JButton("Iniciar");
    private final JButton stop = new JButton("Parar");
    private final JButton maintenance = new JButton("Enviar para manutenção");
    private final JButton remove = new JButton("Remover câmera");
    private final List<Runnable> cameraListeners = new ArrayList<>();
    private final List<Runnable> selectionListeners = new ArrayList<>();
    private List<CameraView> shownCameras = List.of();
    private List<String> cameraLines = List.of();
    private int selectedIndex = -1;
    private ButtonGroup choices;
    private final JLabel title = new JLabel();
    private final JLabel summary = new JLabel();
    private final JButton addCamera = new JButton("+  Adicionar câmera");
    private Runnable positionAction;
    private Runnable configureAction;
    private Runnable changed = () -> { };

    CameraListPanel(ManageGym management, GymChanges changes, IntSupplier selectedCourt, Consumer<String> errorSink) {
        super(new BorderLayout(0, 14));
        if (management == null) throw new IllegalArgumentException("a camera list needs the gym's management");
        if (changes == null) throw new IllegalArgumentException("a camera list needs a way to change the gym");
        if (selectedCourt == null) throw new IllegalArgumentException("a camera list needs the selected court");
        if (errorSink == null) throw new IllegalArgumentException("a camera list needs somewhere to report errors");
        this.management = management;
        this.changes = changes;
        this.selectedCourt = selectedCourt;
        this.errorSink = errorSink;
        cameraTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        cameraTable.getTableHeader().setReorderingAllowed(false);
        // JTable claims F2 to edit a cell; these cells are not editable, and F2 saves a highlight.
        cameraTable.getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0), "none");
        cameraTable.getColumnModel().getColumn(STATUS_COLUMN).setCellRenderer(new BadgeRenderer());
        VarZinhoTheme.styleCard(this);

        JPanel heading = new JPanel(new MigLayout("insets 0, fillx", "[grow][]", "[][]"));
        heading.setOpaque(false);
        JLabel eyebrow = new JLabel("DISPOSITIVOS");
        VarZinhoTheme.styleEyebrow(eyebrow);
        VarZinhoTheme.styleTitle(title);
        heading.add(eyebrow, "cell 0 0");
        heading.add(title, "cell 0 1");
        heading.add(summary, "cell 0 2");
        heading.add(addCamera, "cell 1 0, spany 3");
        addCamera.setBackground(VarZinhoTheme.EMERALD);
        addCamera.setForeground(Color.WHITE);
        add(heading, BorderLayout.NORTH);
        addCamera.putClientProperty("FlatLaf.style", "arc: 14; margin: 8,14,8,14; borderWidth: 0");

        rows.setOpaque(false);
        JScrollPane scroll = ScrollablePage.around(rows);
        scroll.getViewport().setOpaque(false);
        scroll.setOpaque(false);
        scroll.setBorder(javax.swing.BorderFactory.createEmptyBorder());
        add(scroll, BorderLayout.CENTER);

        start.addActionListener(event -> changeSelected(start, management::startRecording));
        stop.addActionListener(event -> changeSelected(stop, management::stopRecording));
        maintenance.addActionListener(event -> changeSelected(maintenance, management::sendToMaintenance));
        remove.addActionListener(event -> changeSelected(remove, management::removeCamera));
        refresh();
    }

    /**
     * Reads the court's cameras again and tells whoever follows them, so an install, a
     * move or a change made outside this panel reaches the summaries as well.
     */
    void refresh() {
        String selected = selectedCamera().map(CameraView::getId).orElse(null);
        shownCameras = management.court(selectedCourt.getAsInt()).getCameras();
        title.setText("Câmeras da quadra " + selectedCourt.getAsInt());
        long active = shownCameras.stream().filter(CameraView::isRecording).count();
        summary.setText(active + (active == 1 ? " câmera gravando" : " câmeras gravando")
                + " · " + shownCameras.size() + (shownCameras.size() == 1 ? " instalada" : " instaladas"));
        cameraLines = shownCameras.stream()
                .map(PortugueseText::cameraDescription)
                .toList();
        selectedIndex = -1;
        for (int i = 0; i < shownCameras.size(); i++) {
            if (shownCameras.get(i).getId().equals(selected)) selectedIndex = i;
        }
        cameraRows.fireTableDataChanged();
        rebuildRows();
        cameraListeners.forEach(Runnable::run);
        changed.run();
    }

    void refreshKeepingSelection() {
        refresh();
        selectionListeners.forEach(Runnable::run);
    }

    Optional<CameraView> selectedCamera() {
        return selectedIndex < 0 || selectedIndex >= shownCameras.size()
                ? Optional.empty() : Optional.of(shownCameras.get(selectedIndex));
    }

    void onSelectionChange(Runnable listener) {
        selectionListeners.add(listener);
    }

    void selectCamera(int index) {
        selectedIndex = index >= 0 && index < shownCameras.size() ? index : -1;
        if (selectedIndex < 0) choices.clearSelection();
        var buttons = choices.getElements();
        for (int i = 0; buttons.hasMoreElements(); i++) {
            boolean selected = i == selectedIndex;
            var button = buttons.nextElement();
            if (selected) button.setSelected(true);
            styleSelection((JPanel) rows.getComponent(i), selected);
        }
        selectionListeners.forEach(Runnable::run);
    }

    List<String> cameraLines() { return List.copyOf(cameraLines); }
    JTable summaryTable() { return cameraTable; }

    void onCamerasChanged(Runnable listener) { cameraListeners.add(listener); }

    List<String> cameraColumns() { return COLUMNS; }

    List<String> cameraRow(int index) {
        List<String> row = new ArrayList<>();
        for (int column = 0; column < COLUMNS.size(); column++) row.add(cameraRows.text(index, column));
        return row;
    }

    List<StatusBadge> statusBadges() {
        return shownCameras.stream().map(camera -> StatusBadge.of(camera.getStatus())).collect(Collectors.toList());
    }

    void onAdd(Runnable action) { addCamera.addActionListener(event -> action.run()); }
    void onConfigure(Runnable action) { configureAction = action; }
    void onPosition(Runnable action) { positionAction = action; }
    void onRefresh(Runnable action) { changed = action; }
    JButton startButton() { return start; }
    JButton stopButton() { return stop; }
    JButton maintenanceButton() { return maintenance; }
    JButton removeCameraButton() { return remove; }

    private void rebuildRows() {
        rows.removeAll();
        choices = new ButtonGroup();
        for (int index = 0; index < shownCameras.size(); index++) {
            rows.add(cameraRow(index, shownCameras.get(index), cameraLines.get(index)), "growx, h 72!");
        }
        if (shownCameras.isEmpty()) rows.add(new JLabel("Nenhuma câmera instalada nesta quadra."), "h 52!");
        setPreferredSize(new Dimension(760, 112 + Math.max(1, Math.min(5, shownCameras.size())) * 78));
        rows.revalidate();
        rows.repaint();
    }

    private JPanel cameraRow(int index, CameraView camera, String description) {
        JPanel row = new JPanel(new MigLayout("insets 10 12, fillx", "[52!][0:pref,grow]12[]", "[][]"));
        styleSelection(row, selectedIndex == index);
        JLabel icon = new JLabel(VarZinhoIcons.cameras(camera.isRecording() ? VarZinhoTheme.EMERALD :
                camera.isUnderMaintenance() ? VarZinhoTheme.MAINTENANCE : VarZinhoTheme.STOPPED));
        String state = camera.isRecording()
                ? (camera.getKind() == CameraKind.STREAM ? "Ao vivo" : "Simulada")
                : camera.isUnderMaintenance() ? "Manutenção" : "Parada";
        JRadioButton id = new JRadioButton(camera.getId(), selectedIndex == index);
        id.setOpaque(false);
        id.getAccessibleContext().setAccessibleName("Selecionar câmera " + camera.getId());
        choices.add(id);
        id.addActionListener(event -> selectCamera(index));
        id.setFont(id.getFont().deriveFont(java.awt.Font.BOLD));
        JLabel details = new JLabel(camera.getModel() + "   ·   " + PortugueseText.cameraKind(camera.getKind())
                + "   ·   " + camera.getResolution().label());
        details.setToolTipText(details.getText());
        id.setToolTipText(camera.getId());
        row.setToolTipText(description);
        details.putClientProperty("FlatLaf.style", "foreground: $VarZinho.stopped");
        JPanel actions = actionsFor(index, camera);
        row.add(VarZinhoTheme.tile(icon.getIcon()), "cell 0 0, span 1 2, growx, h 46!");
        row.add(id, "cell 1 0");
        JLabel badge = new JLabel("● " + state);
        badge.setFont(badge.getFont().deriveFont(java.awt.Font.BOLD, 12f));
        badge.putClientProperty("FlatLaf.style", "foreground: $VarZinho."
                + (camera.isRecording() ? "emerald" : camera.isUnderMaintenance() ? "maintenance" : "stopped"));
        row.add(badge, "cell 1 0, gapleft 8");
        row.add(details, "cell 1 1");
        row.add(actions, "cell 2 0, span 1 2");
        row.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent event) { selectCamera(index); }
        });
        return row;
    }

    private static void styleSelection(JPanel row, boolean selected) {
        row.putClientProperty("FlatLaf.style", selected
                ? "background: $VarZinho.selectedSurface; arc: 12"
                : "background: $VarZinho.cardBackground; arc: 12");
    }

    private JPanel actionsFor(int index, CameraView camera) {
        JPanel actions = new JPanel(new MigLayout("insets 0", "[][]", "[]"));
        actions.setOpaque(false);
        String label = camera.isUnderMaintenance() ? "Liberar" : camera.isRecording() ? "Parar" : "Iniciar";
        JButton record = new JButton(label, camera.isUnderMaintenance() ? VarZinhoIcons.maintenance()
                : camera.isRecording() ? VarZinhoIcons.stop() : VarZinhoIcons.record());
        record.getAccessibleContext().setAccessibleName(label + " · " + camera.getId());
        record.addActionListener(event -> {
            selectCamera(index);
            if (camera.isUnderMaintenance()) changeSelected(record, management::releaseFromMaintenance);
            else if (camera.isRecording()) stop.doClick(0);
            else start.doClick(0);
        });
        record.putClientProperty("FlatLaf.style", camera.isRecording()
                ? "foreground: $VarZinho.recording; borderColor: $VarZinho.recording" : "foreground: $VarZinho.emerald; borderColor: $VarZinho.emerald");
        actions.add(record, "h 36!");
        JButton more = new JButton("Ações");
        more.setToolTipText("Ações da câmera " + camera.getId());
        more.getAccessibleContext().setAccessibleName(more.getToolTipText());
        JPopupMenu menu = new JPopupMenu();
        menu.add(menuItem("Mover para outra quadra", more, index, () -> positionAction.run()));
        menu.add(menuItem("Configurar câmera", more, index, () -> configureAction.run()));
        JMenuItem service = menuItem("Enviar para manutenção", more, index, () -> maintenance.doClick(0));
        service.setEnabled(!camera.isUnderMaintenance());
        menu.add(service);
        menu.addSeparator();
        menu.add(menuItem("Remover câmera", more, index, () -> remove.doClick(0)));
        more.setComponentPopupMenu(menu);
        more.addActionListener(event -> menu.show(more, 0, more.getHeight()));
        actions.add(more, "h 36!");
        return actions;
    }

    private JMenuItem menuItem(String text, JButton owner, int index, Runnable action) {
        JMenuItem item = new JMenuItem(text);
        item.addActionListener(event -> {
            // A refresh can replace the row while its popup is open.
            if (!owner.isEnabled() || !SwingUtilities.isDescendingFrom(owner, rows)) return;
            selectCamera(index);
            action.run();
        });
        return item;
    }

    private void changeSelected(java.awt.Component trigger, CameraChange change) {
        Optional<CameraView> camera = selectedCamera();
        if (camera.isEmpty()) {
            errorSink.accept("Selecione uma câmera primeiro.");
            return;
        }
        int courtNumber = selectedCourt.getAsInt();
        String cameraId = camera.get().getId();
        changes.apply(trigger, () -> change.apply(courtNumber, cameraId), errorSink,
                result -> refreshKeepingSelection());
    }

    @FunctionalInterface
    private interface CameraChange {
        void apply(int courtNumber, String cameraId) throws RepositoryException;
    }

    private final class CameraTableModel extends AbstractTableModel {
        @Override public int getRowCount() { return shownCameras.size(); }
        @Override public int getColumnCount() { return COLUMNS.size(); }
        @Override public String getColumnName(int column) { return COLUMNS.get(column); }

        @Override
        public Object getValueAt(int row, int column) {
            CameraView camera = shownCameras.get(row);
            return column == STATUS_COLUMN ? StatusBadge.of(camera.getStatus()) : text(row, column);
        }

        String text(int row, int column) {
            CameraView camera = shownCameras.get(row);
            return switch (column) {
                case 0 -> camera.getId();
                case 1 -> PortugueseText.cameraKind(camera.getKind());
                case 2 -> camera.getResolution().label();
                default -> StatusBadge.of(camera.getStatus()).label();
            };
        }
    }

    private static final class BadgeRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                                                       boolean focused, int row, int column) {
            StatusBadge badge = (StatusBadge) value;
            JLabel cell = (JLabel) super.getTableCellRendererComponent(table, badge.label(), selected,
                    focused, row, column);
            cell.setHorizontalAlignment(SwingConstants.CENTER);
            cell.setOpaque(true);
            cell.setBackground(badge.color());
            cell.setForeground(Color.WHITE);
            return cell;
        }
    }
}
