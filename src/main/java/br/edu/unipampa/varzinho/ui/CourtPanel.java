package br.edu.unipampa.varzinho.ui;

import br.edu.unipampa.varzinho.domain.capture.Camera;
import br.edu.unipampa.varzinho.domain.structure.Court;
import br.edu.unipampa.varzinho.domain.structure.Gym;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * The top of the window: picks a court and shows the cameras installed on it.
 *
 * <p>It keeps no list of its own. Every refresh asks the selected court for its
 * cameras, so what is shown is what the court holds.
 */
final class CourtPanel extends JPanel {
    private final Gym gym;
    private final Consumer<String> errorSink;
    private final JComboBox<Integer> courtSelector = new JComboBox<>();
    private final DefaultListModel<String> cameraLines = new DefaultListModel<>();
    private final JList<String> cameraList = new JList<>(cameraLines);
    private final JButton start = new JButton("Start");
    private final JButton stop = new JButton("Stop");
    private final JButton maintenance = new JButton("Send to maintenance");

    CourtPanel(Gym gym, Consumer<String> errorSink) {
        super(new BorderLayout(8, 8));
        if (gym == null) throw new IllegalArgumentException("a court panel needs a gym");
        if (gym.getCourts().isEmpty()) throw new IllegalArgumentException("a court panel needs a gym with a court");
        if (errorSink == null) throw new IllegalArgumentException("a court panel needs somewhere to report errors");
        this.gym = gym;
        this.errorSink = errorSink;
        gym.getCourts().stream().map(Court::getNumber).sorted().forEach(courtSelector::addItem);
        courtSelector.addActionListener(event -> showCameras());
        add(courtSelector, BorderLayout.NORTH);
        add(new JScrollPane(cameraList), BorderLayout.CENTER);
        cameraList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        start.addActionListener(event -> onSelectedCamera(Camera::startRecording));
        stop.addActionListener(event -> onSelectedCamera(Camera::stopRecording));
        maintenance.addActionListener(event -> onSelectedCamera(Camera::sendToMaintenance));
        JPanel buttons = new JPanel(new GridLayout(0, 1, 4, 4));
        buttons.add(start); buttons.add(stop); buttons.add(maintenance);
        add(buttons, BorderLayout.EAST);
        showCameras();
    }

    Court selectedCourt() {
        return gym.findCourt((Integer) courtSelector.getSelectedItem());
    }

    void selectCourt(int number) {
        courtSelector.setSelectedItem(number);
    }

    void onCourtChange(Runnable listener) {
        courtSelector.addActionListener(event -> listener.run());
    }

    void selectCamera(int index) {
        cameraList.setSelectedIndex(index);
    }

    List<String> cameraLines() {
        return Collections.list(cameraLines.elements());
    }

    JButton startButton() { return start; }
    JButton stopButton() { return stop; }
    JButton maintenanceButton() { return maintenance; }

    private void onSelectedCamera(Consumer<Camera> transition) {
        int index = cameraList.getSelectedIndex();
        if (index < 0) {
            errorSink.accept("Select a camera first.");
            return;
        }
        try {
            transition.accept(selectedCourt().getCameras().get(index));
        } catch (IllegalStateException refusal) {
            // The camera guards its own transitions; the panel only relays the refusal.
            errorSink.accept(refusal.getMessage());
        }
        showCameras();
        cameraList.setSelectedIndex(index);
    }

    private void showCameras() {
        cameraLines.clear();
        for (Camera camera : selectedCourt().getCameras()) {
            cameraLines.addElement(camera.describe());
        }
    }
}
