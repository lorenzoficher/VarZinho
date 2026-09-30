package br.edu.unipampa.varzinho.ui;

import br.edu.unipampa.varzinho.domain.capture.Camera;
import br.edu.unipampa.varzinho.domain.capture.FixedCamera;
import br.edu.unipampa.varzinho.domain.capture.PtzCamera;
import br.edu.unipampa.varzinho.domain.structure.Court;
import br.edu.unipampa.varzinho.domain.structure.Gym;
import br.edu.unipampa.varzinho.enums.Resolution;
import br.edu.unipampa.varzinho.exception.DuplicateCameraException;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
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
    private final JButton remove = new JButton("Remove camera");
    private final JTextField idField = new JTextField(8);
    private final JTextField modelField = new JTextField(8);
    private final JComboBox<String> typeSelector = new JComboBox<>(new String[] {FIXED, PTZ});
    private final JComboBox<Resolution> resolutionSelector = new JComboBox<>(Resolution.values());
    private final JSpinner angleSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 359, 1));
    private final JButton install = new JButton("Install camera");
    private final JTextField courtNumberField = new JTextField(4);
    private final JButton addCourt = new JButton("Add court");
    private final List<Runnable> courtAddedListeners = new ArrayList<>();
    private final List<Runnable> gymChangedListeners = new ArrayList<>();
    // Wider than the head reaches, so an impossible aim reaches the camera and is refused there.
    private final JSpinner panSpinner = new JSpinner(new SpinnerNumberModel(0, -999, 999, 5));
    private final JSpinner tiltSpinner = new JSpinner(new SpinnerNumberModel(0, -999, 999, 5));
    private final JSpinner zoomSpinner = new JSpinner(new SpinnerNumberModel(1, -99, 99, 1));
    private final JButton move = new JButton("Move");

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
        listCourts();
        courtSelector.addActionListener(event -> showCameras());
        add(buildCourtRow(), BorderLayout.NORTH);
        add(new JScrollPane(cameraList), BorderLayout.CENTER);
        cameraList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        start.addActionListener(event -> onSelectedCamera(Camera::startRecording));
        stop.addActionListener(event -> onSelectedCamera(Camera::stopRecording));
        maintenance.addActionListener(event -> onSelectedCamera(Camera::sendToMaintenance));
        // Its highlights stay in the archive: a camera does not own what it recorded.
        remove.addActionListener(event -> onSelectedCamera(camera -> selectedCourt().removeCamera(camera)));
        JPanel buttons = new JPanel(new GridLayout(0, 1, 4, 4));
        buttons.add(start); buttons.add(stop); buttons.add(maintenance); buttons.add(remove);
        add(buttons, BorderLayout.EAST);
        JPanel forms = new JPanel();
        forms.setLayout(new BoxLayout(forms, BoxLayout.Y_AXIS));
        forms.add(buildAimRow());
        forms.add(buildInstallForm());
        add(forms, BorderLayout.SOUTH);
        cameraList.addListSelectionListener(event -> offerAimingForSelection());
        showCameras();
        offerAimingForSelection();
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

    void onCourtAdded(Runnable listener) {
        courtAddedListeners.add(listener);
    }

    /** Runs the listener after every change the gym accepted: a court, a camera, a state, an aim. */
    void onGymChanged(Runnable listener) {
        gymChangedListeners.add(listener);
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
    JButton removeCameraButton() { return remove; }
    JTextField idField() { return idField; }
    JTextField modelField() { return modelField; }
    JComboBox<String> typeSelector() { return typeSelector; }
    JComboBox<Resolution> resolutionSelector() { return resolutionSelector; }
    JSpinner angleSpinner() { return angleSpinner; }
    JButton installButton() { return install; }
    JTextField courtNumberField() { return courtNumberField; }
    JButton addCourtButton() { return addCourt; }
    JSpinner panSpinner() { return panSpinner; }
    JSpinner tiltSpinner() { return tiltSpinner; }
    JSpinner zoomSpinner() { return zoomSpinner; }
    JButton moveButton() { return move; }

    private JPanel buildCourtRow() {
        JPanel adder = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        adder.add(new JLabel("New court number"));
        adder.add(courtNumberField);
        adder.add(addCourt);
        addCourt.addActionListener(event -> addCourt());
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.add(courtSelector, BorderLayout.CENTER);
        row.add(adder, BorderLayout.EAST);
        return row;
    }

    private void addCourt() {
        int number;
        try {
            number = Integer.parseInt(courtNumberField.getText().trim());
        } catch (NumberFormatException notANumber) {
            // Reading digits is the form's job; whether the number is acceptable is the gym's.
            errorSink.accept("Type the court number as digits.");
            return;
        }
        try {
            gym.addCourt(new Court(number));
        } catch (IllegalArgumentException refusal) {
            errorSink.accept(refusal.getMessage());
            return;
        }
        int position = 0;
        while (position < courtSelector.getItemCount() && courtSelector.getItemAt(position) < number) {
            position++;
        }
        courtSelector.insertItemAt(number, position);
        courtSelector.setSelectedItem(number);
        courtNumberField.setText("");
        courtAddedListeners.forEach(Runnable::run);
        gymChangedListeners.forEach(Runnable::run);
    }

    private void listCourts() {
        gym.getCourts().stream().map(Court::getNumber).sorted().forEach(courtSelector::addItem);
    }

    private JPanel buildAimRow() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        row.setBorder(BorderFactory.createTitledBorder("Aim the selected PTZ camera"));
        row.add(new JLabel("Pan")); row.add(panSpinner);
        row.add(new JLabel("Tilt")); row.add(tiltSpinner);
        row.add(new JLabel("Zoom")); row.add(zoomSpinner);
        row.add(move);
        move.addActionListener(event -> onSelectedCamera(camera -> ((PtzCamera) camera).moveTo(
                (Integer) panSpinner.getValue(), (Integer) tiltSpinner.getValue(),
                (Integer) zoomSpinner.getValue())));
        return row;
    }

    private void offerAimingForSelection() {
        boolean ptzSelected = selectedCamera().filter(PtzCamera.class::isInstance).isPresent();
        panSpinner.setEnabled(ptzSelected);
        tiltSpinner.setEnabled(ptzSelected);
        zoomSpinner.setEnabled(ptzSelected);
        move.setEnabled(ptzSelected);
    }

    private Optional<Camera> selectedCamera() {
        int index = cameraList.getSelectedIndex();
        List<Camera> cameras = selectedCourt().getCameras();
        if (index < 0 || index >= cameras.size()) return Optional.empty();
        return Optional.of(cameras.get(index));
    }

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
        try {
            gym.installCamera(selectedCourt().getNumber(), buildCamera());
        } catch (IllegalArgumentException | DuplicateCameraException refusal) {
            // The camera constructors and the gym validate what was typed; the panel only relays it.
            errorSink.accept(refusal.getMessage());
            return;
        }
        showCameras();
        gymChangedListeners.forEach(Runnable::run);
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
        boolean accepted = true;
        try {
            transition.accept(selectedCourt().getCameras().get(index));
        } catch (IllegalStateException | IllegalArgumentException refusal) {
            // The camera guards its own transitions and range; the panel only relays the refusal.
            errorSink.accept(refusal.getMessage());
            accepted = false;
        }
        showCameras();
        cameraList.setSelectedIndex(index);
        if (accepted) gymChangedListeners.forEach(Runnable::run);
    }

    private void showCameras() {
        cameraLines.clear();
        for (Camera camera : selectedCourt().getCameras()) {
            cameraLines.addElement(camera.describe());
        }
    }
}
