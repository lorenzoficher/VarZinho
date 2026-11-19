package br.edu.unipampa.varzinho.ui;

import br.edu.unipampa.varzinho.application.CameraView;
import br.edu.unipampa.varzinho.application.ManageGym;
import br.edu.unipampa.varzinho.application.port.RepositoryException;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.IntSupplier;
import java.util.stream.Collectors;

/**
 * Shows one court's cameras as a table — camera, kind, resolution and a status badge —
 * with the actions that change the selected camera's state beside it.
 */
final class CameraListPanel extends JPanel {
    private static final List<String> COLUMNS = List.of("Câmera", "Tipo", "Resolução", "Status");
    private static final int STATUS_COLUMN = 3;
    private final ManageGym management;
    private final IntSupplier selectedCourt;
    private final Consumer<String> errorSink;
    private final CameraTableModel cameraRows = new CameraTableModel();
    private final JTable cameraTable = new JTable(cameraRows);
    private final JButton start = new JButton("Iniciar");
    private final JButton stop = new JButton("Parar");
    private final JButton maintenance = new JButton("Enviar para manutenção");
    private final JButton remove = new JButton("Remover câmera");
    private final List<Runnable> cameraListeners = new ArrayList<>();
    private List<CameraView> shownCameras = List.of();

    CameraListPanel(ManageGym management, IntSupplier selectedCourt, Consumer<String> errorSink) {
        super(new BorderLayout(8, 0));
        if (management == null) throw new IllegalArgumentException("a camera list needs the gym's management");
        if (selectedCourt == null) throw new IllegalArgumentException("a camera list needs the selected court");
        if (errorSink == null) throw new IllegalArgumentException("a camera list needs somewhere to report errors");
        this.management = management;
        this.selectedCourt = selectedCourt;
        this.errorSink = errorSink;
        cameraTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        cameraTable.getTableHeader().setReorderingAllowed(false);
        // JTable claims F2 to edit a cell; these cells are not editable, and F2 saves a highlight.
        cameraTable.getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0), "none");
        cameraTable.getColumnModel().getColumn(STATUS_COLUMN).setCellRenderer(new BadgeRenderer());
        add(new JScrollPane(cameraTable), BorderLayout.CENTER);
        JPanel buttons = new JPanel(new GridLayout(0, 1, 4, 4));
        start.setIcon(VarZinhoIcons.record());
        stop.setIcon(VarZinhoIcons.stop());
        maintenance.setIcon(VarZinhoIcons.maintenance());
        buttons.add(start); buttons.add(stop); buttons.add(maintenance); buttons.add(remove);
        JPanel actions = new JPanel(new BorderLayout());
        actions.add(buttons, BorderLayout.NORTH);
        add(actions, BorderLayout.EAST);
        start.addActionListener(event -> changeSelected(management::startRecording));
        stop.addActionListener(event -> changeSelected(management::stopRecording));
        maintenance.addActionListener(event -> changeSelected(management::sendToMaintenance));
        remove.addActionListener(event -> changeSelected(management::removeCamera));
        refresh();
    }

    /**
     * Reads the court's cameras again and tells whoever follows them, so an install, a
     * move or a change made outside this panel reaches the summaries as well.
     */
    void refresh() {
        shownCameras = management.court(selectedCourt.getAsInt()).getCameras();
        cameraRows.fireTableDataChanged();
        cameraListeners.forEach(Runnable::run);
    }

    void refreshKeepingSelection() {
        int selected = cameraTable.getSelectedRow();
        refresh();
        if (selected >= 0 && selected < shownCameras.size()) cameraTable.setRowSelectionInterval(selected, selected);
    }

    Optional<CameraView> selectedCamera() {
        int index = cameraTable.getSelectedRow();
        return index < 0 || index >= shownCameras.size() ? Optional.empty() : Optional.of(shownCameras.get(index));
    }

    void onSelectionChange(Runnable listener) {
        cameraTable.getSelectionModel().addListSelectionListener(event -> listener.run());
    }

    /** Called whenever the court's cameras are read again, so summaries follow at once. */
    void onCamerasChanged(Runnable listener) {
        cameraListeners.add(listener);
    }

    void selectCamera(int index) { cameraTable.setRowSelectionInterval(index, index); }

    List<String> cameraLines() {
        return shownCameras.stream().map(PortugueseText::cameraDescription).collect(Collectors.toList());
    }

    List<String> cameraColumns() { return COLUMNS; }

    List<String> cameraRow(int index) {
        List<String> row = new ArrayList<>();
        for (int column = 0; column < COLUMNS.size(); column++) row.add(cameraRows.text(index, column));
        return row;
    }

    List<StatusBadge> statusBadges() {
        return shownCameras.stream().map(camera -> StatusBadge.of(camera.getStatus())).collect(Collectors.toList());
    }

    JButton startButton() { return start; }
    JButton stopButton() { return stop; }
    JButton maintenanceButton() { return maintenance; }
    JButton removeCameraButton() { return remove; }

    private void changeSelected(CameraChange change) {
        Optional<CameraView> camera = selectedCamera();
        if (camera.isEmpty()) {
            errorSink.accept("Selecione uma câmera primeiro.");
            return;
        }
        GymChangeReporter.attempt(
                () -> change.apply(selectedCourt.getAsInt(), camera.get().getId()),
                errorSink);
        refreshKeepingSelection();
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
