package br.edu.unipampa.varzinho.ui;

import br.edu.unipampa.varzinho.application.CameraView;
import br.edu.unipampa.varzinho.application.ManageGym;
import br.edu.unipampa.varzinho.application.port.RepositoryException;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.IntSupplier;

/** Shows one court's cameras and changes the selected camera's state. */
final class CameraListPanel extends JPanel {
    private final ManageGym management;
    private final IntSupplier selectedCourt;
    private final Consumer<String> errorSink;
    private final DefaultListModel<String> cameraLines = new DefaultListModel<>();
    private final JList<String> cameraList = new JList<>(cameraLines);
    private final JButton start = new JButton("Iniciar");
    private final JButton stop = new JButton("Parar");
    private final JButton maintenance = new JButton("Enviar para manutenção");
    private final JButton remove = new JButton("Remover câmera");
    private List<CameraView> shownCameras = List.of();

    CameraListPanel(ManageGym management, IntSupplier selectedCourt, Consumer<String> errorSink) {
        super(new BorderLayout(8, 0));
        if (management == null) throw new IllegalArgumentException("a camera list needs the gym's management");
        if (selectedCourt == null) throw new IllegalArgumentException("a camera list needs the selected court");
        if (errorSink == null) throw new IllegalArgumentException("a camera list needs somewhere to report errors");
        this.management = management;
        this.selectedCourt = selectedCourt;
        this.errorSink = errorSink;
        cameraList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        add(new JScrollPane(cameraList), BorderLayout.CENTER);
        JPanel buttons = new JPanel(new GridLayout(0, 1, 4, 4));
        start.setIcon(VarZinhoIcons.record());
        stop.setIcon(VarZinhoIcons.stop());
        maintenance.setIcon(VarZinhoIcons.maintenance());
        buttons.add(start); buttons.add(stop); buttons.add(maintenance); buttons.add(remove);
        add(buttons, BorderLayout.EAST);
        start.addActionListener(event -> changeSelected(management::startRecording));
        stop.addActionListener(event -> changeSelected(management::stopRecording));
        maintenance.addActionListener(event -> changeSelected(management::sendToMaintenance));
        remove.addActionListener(event -> changeSelected(management::removeCamera));
        refresh();
    }

    void refresh() {
        shownCameras = management.court(selectedCourt.getAsInt()).getCameras();
        cameraLines.clear();
        shownCameras.forEach(camera -> cameraLines.addElement(
                PortugueseText.cameraDescription(camera)));
    }

    void refreshKeepingSelection() {
        int selected = cameraList.getSelectedIndex();
        refresh();
        if (selected < shownCameras.size()) cameraList.setSelectedIndex(selected);
    }

    Optional<CameraView> selectedCamera() {
        int index = cameraList.getSelectedIndex();
        return index < 0 || index >= shownCameras.size() ? Optional.empty() : Optional.of(shownCameras.get(index));
    }

    void onSelectionChange(Runnable listener) {
        cameraList.addListSelectionListener(event -> listener.run());
    }

    void selectCamera(int index) { cameraList.setSelectedIndex(index); }

    List<String> cameraLines() { return Collections.list(cameraLines.elements()); }

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
}
