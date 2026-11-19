package br.edu.unipampa.varzinho.ui;

import br.edu.unipampa.varzinho.application.CameraView;
import br.edu.unipampa.varzinho.application.CourtView;
import br.edu.unipampa.varzinho.application.ManageGym;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/** Moves the selected camera between courts or aims it when it supports aiming. */
final class CameraPositionPanel extends JPanel {
    private final ManageGym management;
    private final IntSupplier selectedCourt;
    private final Supplier<Optional<CameraView>> selectedCamera;
    private final Consumer<String> errorSink;
    private final Runnable onChanged;
    private final JComboBox<Integer> moveTargetSelector = new JComboBox<>();
    private final JButton moveToCourt = new JButton("Mover para a quadra");
    // Wider than the head reaches, so the camera remains responsible for refusing an impossible aim.
    private final JSpinner panSpinner = new JSpinner(new SpinnerNumberModel(0, -999, 999, 5));
    private final JSpinner tiltSpinner = new JSpinner(new SpinnerNumberModel(0, -999, 999, 5));
    private final JSpinner zoomSpinner = new JSpinner(new SpinnerNumberModel(1, -99, 99, 1));
    private final JButton aim = new JButton("Mover");

    CameraPositionPanel(ManageGym management, IntSupplier selectedCourt,
                        Supplier<Optional<CameraView>> selectedCamera, Consumer<String> errorSink,
                        Runnable onChanged) {
        super(new GridLayout(2, 1));
        if (management == null) throw new IllegalArgumentException("camera positioning needs gym management");
        if (selectedCourt == null) throw new IllegalArgumentException("camera positioning needs a selected court");
        if (selectedCamera == null) throw new IllegalArgumentException("camera positioning needs a selected camera");
        if (errorSink == null) throw new IllegalArgumentException("camera positioning needs an error destination");
        if (onChanged == null) throw new IllegalArgumentException("camera positioning needs a refresh action");
        this.management = management;
        this.selectedCourt = selectedCourt;
        this.selectedCamera = selectedCamera;
        this.errorSink = errorSink;
        this.onChanged = onChanged;
        aim.setIcon(VarZinhoIcons.aim());
        for (CourtView court : management.courts()) moveTargetSelector.addItem(court.getNumber());
        add(buildMoveRow());
        add(buildAimRow());
        updateAimAvailability();
    }

    void addCourt(int number) {
        int position = 0;
        while (position < moveTargetSelector.getItemCount() && moveTargetSelector.getItemAt(position) < number) {
            position++;
        }
        moveTargetSelector.insertItemAt(number, position);
    }

    void updateAimAvailability() {
        boolean enabled = selectedCamera.get().filter(CameraView::isAimable).isPresent();
        panSpinner.setEnabled(enabled);
        tiltSpinner.setEnabled(enabled);
        zoomSpinner.setEnabled(enabled);
        aim.setEnabled(enabled);
    }

    JSpinner panSpinner() { return panSpinner; }
    JSpinner tiltSpinner() { return tiltSpinner; }
    JSpinner zoomSpinner() { return zoomSpinner; }
    JButton moveButton() { return aim; }
    JComboBox<Integer> moveTargetSelector() { return moveTargetSelector; }
    JButton moveToCourtButton() { return moveToCourt; }

    private JPanel buildMoveRow() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        row.setBorder(BorderFactory.createTitledBorder("Mover a câmera selecionada"));
        row.add(new JLabel("Para a quadra")); row.add(moveTargetSelector); row.add(moveToCourt);
        moveToCourt.addActionListener(event -> changeSelected(camera -> management.moveCamera(
                selectedCourt.getAsInt(), camera.getId(), (Integer) moveTargetSelector.getSelectedItem())));
        return row;
    }

    private JPanel buildAimRow() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        row.setBorder(BorderFactory.createTitledBorder("Apontar a câmera PTZ selecionada"));
        row.add(new JLabel("Panorâmica")); row.add(panSpinner);
        row.add(new JLabel("Inclinação")); row.add(tiltSpinner);
        row.add(new JLabel("Zoom")); row.add(zoomSpinner); row.add(aim);
        aim.addActionListener(event -> changeSelected(camera -> management.aimCamera(
                selectedCourt.getAsInt(), camera.getId(), (Integer) panSpinner.getValue(),
                (Integer) tiltSpinner.getValue(), (Integer) zoomSpinner.getValue())));
        return row;
    }

    private void changeSelected(CameraChange change) {
        Optional<CameraView> camera = selectedCamera.get();
        if (camera.isEmpty()) {
            errorSink.accept("Selecione uma câmera primeiro.");
            return;
        }
        GymChangeReporter.attempt(() -> change.apply(camera.get()),
                errorSink);
        onChanged.run();
        updateAimAvailability();
    }

    @FunctionalInterface
    private interface CameraChange {
        void apply(CameraView camera) throws br.edu.unipampa.varzinho.application.port.RepositoryException;
    }
}
