package br.edu.unipampa.varzinho.ui;

import br.edu.unipampa.varzinho.application.CameraKind;
import br.edu.unipampa.varzinho.application.CameraSpec;
import br.edu.unipampa.varzinho.application.CameraView;
import br.edu.unipampa.varzinho.application.CourtView;
import br.edu.unipampa.varzinho.application.ManageGym;
import br.edu.unipampa.varzinho.application.port.RepositoryException;
import br.edu.unipampa.varzinho.enums.Resolution;
import br.edu.unipampa.varzinho.exception.DomainException;

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
 * <p>It keeps no list of its own and changes nothing itself. Every change goes through
 * {@link ManageGym}, which keeps the gym, and every refresh shows the court as the use
 * case reports it, so what is shown is what the court holds.
 */
final class CourtPanel extends JPanel {
    private final ManageGym management;
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
    private final JComboBox<CameraKind> typeSelector = new JComboBox<>(CameraKind.values());
    private final JComboBox<Resolution> resolutionSelector = new JComboBox<>(Resolution.values());
    private final JSpinner angleSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 359, 1));
    private final JButton install = new JButton("Install camera");
    private final JTextField courtNumberField = new JTextField(4);
    private final JButton addCourt = new JButton("Add court");
    private final List<Runnable> courtAddedListeners = new ArrayList<>();
    // Wider than the head reaches, so an impossible aim reaches the camera and is refused there.
    private final JSpinner panSpinner = new JSpinner(new SpinnerNumberModel(0, -999, 999, 5));
    private final JSpinner tiltSpinner = new JSpinner(new SpinnerNumberModel(0, -999, 999, 5));
    private final JSpinner zoomSpinner = new JSpinner(new SpinnerNumberModel(1, -99, 99, 1));
    private final JButton move = new JButton("Move");
    private final JComboBox<Integer> moveTargetSelector = new JComboBox<>();
    private final JButton moveToCourt = new JButton("Move to court");
    private List<CameraView> shownCameras = List.of();

    CourtPanel(ManageGym management, int bufferSeconds, Consumer<String> errorSink) {
        super(new BorderLayout(8, 8));
        if (management == null) throw new IllegalArgumentException("a court panel needs the gym's management");
        if (management.courts().isEmpty()) throw new IllegalArgumentException("a court panel needs a gym with a court");
        if (bufferSeconds <= 0) {
            throw new IllegalArgumentException("a buffer holds at least one second, not " + bufferSeconds);
        }
        if (errorSink == null) throw new IllegalArgumentException("a court panel needs somewhere to report errors");
        this.management = management;
        this.bufferSeconds = bufferSeconds;
        this.errorSink = errorSink;
        listCourts();
        courtSelector.addActionListener(event -> showCameras());
        add(buildCourtRow(), BorderLayout.NORTH);
        add(new JScrollPane(cameraList), BorderLayout.CENTER);
        cameraList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        start.addActionListener(event -> onSelectedCamera(id -> management.startRecording(selectedCourtNumber(), id)));
        stop.addActionListener(event -> onSelectedCamera(id -> management.stopRecording(selectedCourtNumber(), id)));
        maintenance.addActionListener(event -> onSelectedCamera(
                id -> management.sendToMaintenance(selectedCourtNumber(), id)));
        // Its highlights stay in the archive: a camera does not own what it recorded.
        remove.addActionListener(event -> onSelectedCamera(id -> management.removeCamera(selectedCourtNumber(), id)));
        JPanel buttons = new JPanel(new GridLayout(0, 1, 4, 4));
        buttons.add(start); buttons.add(stop); buttons.add(maintenance); buttons.add(remove);
        add(buttons, BorderLayout.EAST);
        JPanel forms = new JPanel();
        forms.setLayout(new BoxLayout(forms, BoxLayout.Y_AXIS));
        forms.add(buildMoveRow());
        forms.add(buildAimRow());
        forms.add(buildInstallForm());
        add(forms, BorderLayout.SOUTH);
        cameraList.addListSelectionListener(event -> offerAimingForSelection());
        showCameras();
        offerAimingForSelection();
    }

    int selectedCourtNumber() {
        return (Integer) courtSelector.getSelectedItem();
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
    JComboBox<CameraKind> typeSelector() { return typeSelector; }
    JComboBox<Resolution> resolutionSelector() { return resolutionSelector; }
    JSpinner angleSpinner() { return angleSpinner; }
    JButton installButton() { return install; }
    JTextField courtNumberField() { return courtNumberField; }
    JButton addCourtButton() { return addCourt; }
    JSpinner panSpinner() { return panSpinner; }
    JSpinner tiltSpinner() { return tiltSpinner; }
    JSpinner zoomSpinner() { return zoomSpinner; }
    JButton moveButton() { return move; }
    JComboBox<Integer> moveTargetSelector() { return moveTargetSelector; }
    JButton moveToCourtButton() { return moveToCourt; }

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
        if (!attempt(() -> management.addCourt(number))) {
            return;
        }
        insertInOrder(courtSelector, number);
        insertInOrder(moveTargetSelector, number);
        courtSelector.setSelectedItem(number);
        courtNumberField.setText("");
        courtAddedListeners.forEach(Runnable::run);
    }

    private void listCourts() {
        for (CourtView court : management.courts()) {
            courtSelector.addItem(court.getNumber());
            moveTargetSelector.addItem(court.getNumber());
        }
    }

    private static void insertInOrder(JComboBox<Integer> selector, int number) {
        int position = 0;
        while (position < selector.getItemCount() && selector.getItemAt(position) < number) {
            position++;
        }
        selector.insertItemAt(number, position);
    }

    private JPanel buildMoveRow() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        row.setBorder(BorderFactory.createTitledBorder("Move the selected camera"));
        row.add(new JLabel("To court"));
        row.add(moveTargetSelector);
        row.add(moveToCourt);
        moveToCourt.addActionListener(event -> onSelectedCamera(id -> management.moveCamera(
                selectedCourtNumber(), id, (Integer) moveTargetSelector.getSelectedItem())));
        return row;
    }

    private JPanel buildAimRow() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        row.setBorder(BorderFactory.createTitledBorder("Aim the selected PTZ camera"));
        row.add(new JLabel("Pan")); row.add(panSpinner);
        row.add(new JLabel("Tilt")); row.add(tiltSpinner);
        row.add(new JLabel("Zoom")); row.add(zoomSpinner);
        row.add(move);
        move.addActionListener(event -> onSelectedCamera(id -> management.aimCamera(selectedCourtNumber(), id,
                (Integer) panSpinner.getValue(), (Integer) tiltSpinner.getValue(),
                (Integer) zoomSpinner.getValue())));
        return row;
    }

    private void offerAimingForSelection() {
        boolean aimableSelected = selectedCamera().filter(CameraView::isAimable).isPresent();
        panSpinner.setEnabled(aimableSelected);
        tiltSpinner.setEnabled(aimableSelected);
        zoomSpinner.setEnabled(aimableSelected);
        move.setEnabled(aimableSelected);
    }

    private Optional<CameraView> selectedCamera() {
        int index = cameraList.getSelectedIndex();
        if (index < 0 || index >= shownCameras.size()) return Optional.empty();
        return Optional.of(shownCameras.get(index));
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
        typeSelector.addActionListener(
                event -> angleSpinner.setEnabled(typeSelector.getSelectedItem() == CameraKind.FIXED));
        install.addActionListener(event -> installCamera());
        return form;
    }

    private void installCamera() {
        CameraSpec spec = new CameraSpec(idField.getText(), modelField.getText(),
                (CameraKind) typeSelector.getSelectedItem(), (Resolution) resolutionSelector.getSelectedItem(),
                bufferSeconds, (Integer) angleSpinner.getValue());
        attempt(() -> management.installCamera(selectedCourtNumber(), spec));
        showCameras();
    }

    private void onSelectedCamera(CameraChange change) {
        Optional<CameraView> camera = selectedCamera();
        if (camera.isEmpty()) {
            errorSink.accept("Select a camera first.");
            return;
        }
        int index = cameraList.getSelectedIndex();
        attempt(() -> change.apply(camera.get().getId()));
        showCameras();
        cameraList.setSelectedIndex(index);
    }

    /**
     * Runs one change and relays its refusal. The gym and its cameras validate what was
     * asked; the panel only passes the message on.
     *
     * @return whether the gym took the change, even if keeping it then failed
     */
    private boolean attempt(GymChange change) {
        try {
            change.apply();
            return true;
        } catch (RepositoryException failure) {
            errorSink.accept("The gym could not be saved: " + failure.getMessage());
            return true;
        } catch (IllegalStateException | IllegalArgumentException | DomainException refusal) {
            errorSink.accept(refusal.getMessage());
            return false;
        }
    }

    private void showCameras() {
        shownCameras = management.court(selectedCourtNumber()).getCameras();
        cameraLines.clear();
        for (CameraView camera : shownCameras) {
            cameraLines.addElement(camera.getDescription());
        }
    }

    @FunctionalInterface
    private interface GymChange {
        void apply() throws RepositoryException;
    }

    @FunctionalInterface
    private interface CameraChange {
        void apply(String cameraId) throws RepositoryException;
    }
}
