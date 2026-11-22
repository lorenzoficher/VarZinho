package br.edu.unipampa.varzinho.ui;

import br.edu.unipampa.varzinho.application.CameraView;
import br.edu.unipampa.varzinho.application.CourtView;
import br.edu.unipampa.varzinho.application.ManageGym;
import net.miginfocom.swing.MigLayout;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
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
        super(new java.awt.BorderLayout());
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
        AdaptivePair controls = new AdaptivePair(920, "[0:pref,grow,fill][0:pref,grow,fill]");
        controls.add(buildMoveRow(), "growy");
        controls.add(buildAimRow(), "growy");
        add(controls);
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
        Optional<CameraView> target = selectedCamera.get().filter(CameraView::isAimable)
                .filter(camera -> !camera.isUnderMaintenance());
        boolean enabled = target.isPresent();
        target.ifPresent(camera -> {
            panSpinner.setValue(camera.getPan().orElseThrow());
            tiltSpinner.setValue(camera.getTilt().orElseThrow());
            zoomSpinner.setValue(camera.getZoom().orElseThrow());
        });
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
        JPanel row = new JPanel(new MigLayout("insets 16, fillx, wrap 3", "[][grow,fill][]", "[]12[]"));
        VarZinhoTheme.styleCard(row);
        JLabel title = new JLabel("Mover câmera");
        VarZinhoTheme.styleTitle(title);
        row.add(title, "span 3, wrap");
        row.add(new JLabel("Quadra de destino")); row.add(moveTargetSelector); row.add(moveToCourt, "h 38!");
        moveToCourt.addActionListener(event -> changeSelected(camera -> management.moveCamera(
                selectedCourt.getAsInt(), camera.getId(), (Integer) moveTargetSelector.getSelectedItem())));
        return row;
    }

    private JPanel buildAimRow() {
        JPanel row = new JPanel(new MigLayout("insets 16, fillx, wrap 7", "[][grow,fill][][grow,fill][][grow,fill][]", "[]12[]"));
        VarZinhoTheme.styleCard(row);
        JLabel title = new JLabel("Controle PTZ");
        VarZinhoTheme.styleTitle(title);
        row.add(title, "span 7, wrap");
        row.add(new JLabel("Panorâmica")); row.add(panSpinner);
        row.add(new JLabel("Inclinação")); row.add(tiltSpinner);
        row.add(new JLabel("Zoom")); row.add(zoomSpinner); row.add(aim, "h 38!");
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
        if (GymChangeReporter.attempt(() -> change.apply(camera.get()), errorSink)) onChanged.run();
        updateAimAvailability();
    }

    @FunctionalInterface
    private interface CameraChange {
        void apply(CameraView camera) throws br.edu.unipampa.varzinho.application.port.RepositoryException;
    }
}
