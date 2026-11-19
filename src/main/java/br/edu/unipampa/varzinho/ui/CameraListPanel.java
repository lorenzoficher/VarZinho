package br.edu.unipampa.varzinho.ui;

import br.edu.unipampa.varzinho.application.CameraView;
import br.edu.unipampa.varzinho.application.ManageGym;
import br.edu.unipampa.varzinho.application.port.RepositoryException;
import net.miginfocom.swing.MigLayout;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.IntSupplier;

/** Shows each camera as an operational row with actions backed by gym management. */
final class CameraListPanel extends JPanel {
    private final ManageGym management;
    private final IntSupplier selectedCourt;
    private final Consumer<String> errorSink;
    private final JPanel rows = new JPanel(new MigLayout("insets 0, fillx, wrap 1", "[grow,fill]", "[]"));
    private final JButton start = new JButton("Iniciar");
    private final JButton stop = new JButton("Parar");
    private final JButton maintenance = new JButton("Enviar para manutenção");
    private final JButton remove = new JButton("Remover câmera");
    private final List<Runnable> selectionListeners = new ArrayList<>();
    private List<CameraView> shownCameras = List.of();
    private List<String> cameraLines = List.of();
    private int selectedIndex = -1;

    CameraListPanel(ManageGym management, IntSupplier selectedCourt, Consumer<String> errorSink) {
        super(new BorderLayout(0, 14));
        if (management == null) throw new IllegalArgumentException("a camera list needs the gym's management");
        if (selectedCourt == null) throw new IllegalArgumentException("a camera list needs the selected court");
        if (errorSink == null) throw new IllegalArgumentException("a camera list needs somewhere to report errors");
        this.management = management;
        this.selectedCourt = selectedCourt;
        this.errorSink = errorSink;
        VarZinhoTheme.styleCard(this);
        setPreferredSize(new Dimension(760, 330));

        JPanel heading = new JPanel(new MigLayout("insets 0, fillx", "[grow][]", "[][]"));
        heading.setOpaque(false);
        JLabel eyebrow = new JLabel("DISPOSITIVOS");
        VarZinhoTheme.styleEyebrow(eyebrow);
        JLabel title = new JLabel("Câmeras da quadra selecionada");
        VarZinhoTheme.styleTitle(title);
        heading.add(eyebrow, "cell 0 0");
        heading.add(title, "cell 0 1");
        add(heading, BorderLayout.NORTH);

        rows.setOpaque(false);
        JScrollPane scroll = new JScrollPane(rows);
        scroll.setBorder(javax.swing.BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(14);
        add(scroll, BorderLayout.CENTER);

        start.addActionListener(event -> changeSelected(management::startRecording));
        stop.addActionListener(event -> changeSelected(management::stopRecording));
        maintenance.addActionListener(event -> changeSelected(management::sendToMaintenance));
        remove.addActionListener(event -> changeSelected(management::removeCamera));
        refresh();
    }

    void refresh() {
        shownCameras = management.court(selectedCourt.getAsInt()).getCameras();
        cameraLines = shownCameras.stream()
                .map(camera -> PortugueseText.cameraDescription(camera.getDescription()))
                .toList();
        if (selectedIndex >= shownCameras.size()) selectedIndex = -1;
        rebuildRows();
    }

    void refreshKeepingSelection() {
        int kept = selectedIndex;
        refresh();
        if (kept >= 0 && kept < shownCameras.size()) selectedIndex = kept;
        rebuildRows();
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
        rebuildRows();
        selectionListeners.forEach(Runnable::run);
    }

    List<String> cameraLines() { return List.copyOf(cameraLines); }
    JButton startButton() { return start; }
    JButton stopButton() { return stop; }
    JButton maintenanceButton() { return maintenance; }
    JButton removeCameraButton() { return remove; }

    private void rebuildRows() {
        rows.removeAll();
        for (int index = 0; index < shownCameras.size(); index++) {
            rows.add(cameraRow(index, shownCameras.get(index), cameraLines.get(index)), "growx, h 72!");
        }
        rows.revalidate();
        rows.repaint();
    }

    private JPanel cameraRow(int index, CameraView camera, String description) {
        JPanel row = new JPanel(new MigLayout("insets 10 12, fillx", "[52!][grow][]", "[][]"));
        row.putClientProperty("FlatLaf.style", selectedIndex == index
                ? "background: fade($VarZinho.accent,14%); arc: 12"
                : "background: $VarZinho.cardBackground; arc: 12");
        JLabel icon = new JLabel(VarZinhoIcons.cameras(statusColor(description)));
        JLabel id = new JLabel(camera.getId());
        id.setFont(id.getFont().deriveFont(java.awt.Font.BOLD));
        JLabel details = new JLabel(description);
        details.setForeground(VarZinhoTheme.STOPPED);
        JPanel actions = actionsFor(index, description);
        row.add(icon, "cell 0 0, span 1 2, align center");
        row.add(id, "cell 1 0");
        row.add(details, "cell 1 1");
        row.add(actions, "cell 2 0, span 1 2");
        row.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent event) { selectCamera(index); }
        });
        return row;
    }

    private JPanel actionsFor(int index, String description) {
        JPanel actions = new JPanel(new MigLayout("insets 0", "[][][][]", "[]"));
        actions.setOpaque(false);
        JButton record = rowAction(description.contains("ATIVA") ? "Parar" : "Iniciar",
                description.contains("ATIVA") ? VarZinhoIcons.stop() : VarZinhoIcons.record(),
                index, description.contains("ATIVA") ? stop : start);
        actions.add(record, "h 36!");
        actions.add(rowAction("Mover", VarZinhoIcons.aim(), index, null), "h 36!");
        actions.add(rowAction("Manutenção", VarZinhoIcons.maintenance(), index, maintenance), "h 36!");
        actions.add(rowAction("Remover", VarZinhoIcons.archive(), index, remove), "h 36!");
        return actions;
    }

    private JButton rowAction(String text, javax.swing.Icon icon, int index, JButton action) {
        JButton button = new JButton(text, icon);
        button.addActionListener(event -> {
            selectCamera(index);
            if (action == null) {
                errorSink.accept("Use o painel de posicionamento abaixo para mover a câmera selecionada.");
            } else {
                action.doClick();
            }
        });
        return button;
    }

    private static Color statusColor(String description) {
        if (description.contains("ATIVA")) return VarZinhoTheme.EMERALD;
        if (description.contains("MANUTENÇÃO")) return VarZinhoTheme.MAINTENANCE;
        return VarZinhoTheme.STOPPED;
    }

    private void changeSelected(CameraChange change) {
        Optional<CameraView> camera = selectedCamera();
        if (camera.isEmpty()) {
            errorSink.accept("Selecione uma câmera primeiro.");
            return;
        }
        GymChangeReporter.attempt(
                () -> change.apply(selectedCourt.getAsInt(), camera.get().getId()),
                message -> errorSink.accept(PortugueseText.message(message)));
        refreshKeepingSelection();
    }

    @FunctionalInterface
    private interface CameraChange {
        void apply(int courtNumber, String cameraId) throws RepositoryException;
    }
}
