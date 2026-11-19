package br.edu.unipampa.varzinho.ui;

import br.edu.unipampa.varzinho.application.CameraKind;
import br.edu.unipampa.varzinho.application.ManageGym;
import br.edu.unipampa.varzinho.enums.Resolution;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Arranges the four independent parts of the court area. */
final class CourtPanel extends JPanel {
    private final List<Component> pausedControls = new ArrayList<>();
    private final CourtSelectionPanel courtSelection;
    private final CameraListPanel cameraList;
    private final CameraInstallPanel cameraInstallation;
    private final CameraPositionPanel cameraPosition;

    CourtPanel(ManageGym management, int bufferSeconds, Consumer<String> errorSink) {
        super(new BorderLayout(8, 8));
        courtSelection = new CourtSelectionPanel(management, errorSink);
        cameraList = new CameraListPanel(management, courtSelection::selectedCourtNumber, errorSink);
        cameraInstallation = new CameraInstallPanel(management, courtSelection::selectedCourtNumber,
                bufferSeconds, errorSink, cameraList::refresh);
        cameraPosition = new CameraPositionPanel(management, courtSelection::selectedCourtNumber,
                cameraList::selectedCamera, errorSink, cameraList::refreshKeepingSelection);

        courtSelection.onCourtChange(() -> {
            cameraList.refresh();
            cameraPosition.updateAimAvailability();
        });
        courtSelection.onCourtAdded(() -> cameraPosition.addCourt(courtSelection.selectedCourtNumber()));
        cameraList.onSelectionChange(cameraPosition::updateAimAvailability);

        JPanel forms = new JPanel();
        forms.setLayout(new BoxLayout(forms, BoxLayout.Y_AXIS));
        forms.add(cameraPosition);
        forms.add(cameraInstallation);
        add(courtSelection, BorderLayout.NORTH);
        add(cameraList, BorderLayout.CENTER);
        add(forms, BorderLayout.SOUTH);
    }

    void pauseChanges() {
        pause(this);
    }

    private void pause(Component control) {
        if (control.isEnabled()) {
            pausedControls.add(control);
            control.setEnabled(false);
        }
        if (control instanceof Container container) {
            for (Component child : container.getComponents()) pause(child);
        }
    }

    void resumeChanges() {
        pausedControls.forEach(control -> control.setEnabled(true));
        pausedControls.clear();
    }

    int selectedCourtNumber() { return courtSelection.selectedCourtNumber(); }
    void selectCourt(int number) { courtSelection.selectCourt(number); }
    void onCourtChange(Runnable listener) { courtSelection.onCourtChange(listener); }
    void onCourtAdded(Runnable listener) { courtSelection.onCourtAdded(listener); }
    void onCamerasChanged(Runnable listener) { cameraList.onCamerasChanged(listener); }
    void selectCamera(int index) { cameraList.selectCamera(index); }
    List<String> cameraLines() { return cameraList.cameraLines(); }
    List<String> cameraColumns() { return cameraList.cameraColumns(); }
    List<String> cameraRow(int index) { return cameraList.cameraRow(index); }
    List<StatusBadge> statusBadges() { return cameraList.statusBadges(); }
    JButton startButton() { return cameraList.startButton(); }
    JButton stopButton() { return cameraList.stopButton(); }
    JButton maintenanceButton() { return cameraList.maintenanceButton(); }
    JButton removeCameraButton() { return cameraList.removeCameraButton(); }
    JTextField idField() { return cameraInstallation.idField(); }
    JTextField modelField() { return cameraInstallation.modelField(); }
    JComboBox<CameraKind> typeSelector() { return cameraInstallation.typeSelector(); }
    JComboBox<Resolution> resolutionSelector() { return cameraInstallation.resolutionSelector(); }
    JSpinner angleSpinner() { return cameraInstallation.angleSpinner(); }
    JButton installButton() { return cameraInstallation.installButton(); }
    JTextField courtNumberField() { return courtSelection.courtNumberField(); }
    JButton addCourtButton() { return courtSelection.addCourtButton(); }
    JSpinner panSpinner() { return cameraPosition.panSpinner(); }
    JSpinner tiltSpinner() { return cameraPosition.tiltSpinner(); }
    JSpinner zoomSpinner() { return cameraPosition.zoomSpinner(); }
    JButton moveButton() { return cameraPosition.moveButton(); }
    JComboBox<Integer> moveTargetSelector() { return cameraPosition.moveTargetSelector(); }
    JButton moveToCourtButton() { return cameraPosition.moveToCourtButton(); }
}
