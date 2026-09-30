package br.edu.unipampa.varzinho.ui;

import br.edu.unipampa.varzinho.domain.capture.Camera;
import br.edu.unipampa.varzinho.domain.capture.FixedCamera;
import br.edu.unipampa.varzinho.domain.capture.PtzCamera;
import br.edu.unipampa.varzinho.domain.structure.Court;
import br.edu.unipampa.varzinho.domain.structure.Gym;
import br.edu.unipampa.varzinho.enums.Resolution;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * The top of the window: picks a court, shows the cameras installed on it and
 * installs new ones.
 *
 * <p>It keeps no list of its own. Every refresh asks the selected court for its
 * cameras, so what is shown is what the court holds.
 */
final class CourtPanel extends JPanel {
    static final String FIXED = "Fixed";
    static final String PTZ = "PTZ";

    private final Gym gym;
    private final int bufferSeconds;
    private final Consumer<String> errorSink;
    private final JComboBox<Integer> courtSelector = new JComboBox<>();
    private final DefaultListModel<String> cameraLines = new DefaultListModel<>();
    private final JList<String> cameraList = new JList<>(cameraLines);
    private final JButton start = new JButton("Start");
    private final JButton stop = new JButton("Stop");
    private final JButton maintenance = new JButton("Send to maintenance");
    private final JTextField idField = new JTextField(8);
    private final JTextField modelField = new JTextField(8);
    private final JComboBox<String> typeSelector = new JComboBox<>(new String[] {FIXED, PTZ});
    private final JComboBox<Resolution> resolutionSelector = new JComboBox<>(Resolution.values());
    private final JSpinner angleSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 359, 1));
    private final JButton install = new JButton("Install camera");

    CourtPanel(Gym gym, int bufferSeconds, Consumer<String> errorSink) {
        super(new BorderLayout(8, 8));
        if (gym == null) throw new IllegalArgumentException("a court panel needs a gym");
        if (gym.getCourts().isEmpty()) throw new IllegalArgumentException("a court panel needs a gym with a court");
        if (bufferSeconds <= 0) {
            throw new IllegalArgumentException("a buffer holds at least one second, not " + bufferSeconds);
        }
        if (errorSink == null) throw new IllegalArgumentException("a court panel needs somewhere to report errors");
        this.gym = gym;
        this.bufferSeconds = bufferSeconds;
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
        add(buildInstallForm(), BorderLayout.SOUTH);
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
    JTextField idField() { return idField; }
    JTextField modelField() { return modelField; }
    JComboBox<String> typeSelector() { return typeSelector; }
    JComboBox<Resolution> resolutionSelector() { return resolutionSelector; }
    JSpinner angleSpinner() { return angleSpinner; }
    JButton installButton() { return install; }

    private JPanel buildInstallForm() {
        JPanel identity = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        identity.add(new JLabel("Id")); identity.add(idField);
        identity.add(new JLabel("Model")); identity.add(modelField);
        identity.add(new JLabel("Type")); identity.add(typeSelector);
        JPanel settings = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        settings.add(new JLabel("Resolution")); settings.add(resolutionSelector);
        settings.add(new JLabel("Angle")); settings.add(angleSpinner);
        settings.add(install);
        JPanel form = new JPanel(new GridLayout(2, 1));
        form.setBorder(BorderFactory.createTitledBorder("Install camera on this court"));
        form.add(identity);
        form.add(settings);
        // A PTZ points wherever it is moved, so only a fixed camera is given an angle.
        typeSelector.addActionListener(event -> angleSpinner.setEnabled(FIXED.equals(typeSelector.getSelectedItem())));
        install.addActionListener(event -> installCamera());
        return form;
    }

    private void installCamera() {
        Court court = selectedCourt();
        int before = court.getCameras().size();
        Camera camera;
        try {
            camera = buildCamera();
        } catch (IllegalArgumentException refusal) {
            // The camera constructors validate what was typed; the panel only relays it.
            errorSink.accept(refusal.getMessage());
            return;
        }
        court.installCamera(camera);
        // Court ignores an id it already has without saying so. Until it throws
        // instead, the panel tells the user rather than leave a silent button.
        if (court.getCameras().size() == before) {
            errorSink.accept("Court " + court.getNumber() + " already has a camera named " + camera.getId() + ".");
            return;
        }
        showCameras();
    }

    private Camera buildCamera() {
        Resolution resolution = (Resolution) resolutionSelector.getSelectedItem();
        if (PTZ.equals(typeSelector.getSelectedItem())) {
            return new PtzCamera(idField.getText(), modelField.getText(), resolution, bufferSeconds);
        }
        return new FixedCamera(idField.getText(), modelField.getText(), resolution, bufferSeconds,
                (Integer) angleSpinner.getValue());
    }

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
