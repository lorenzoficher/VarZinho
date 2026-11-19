package br.edu.unipampa.varzinho.ui;

import br.edu.unipampa.varzinho.application.CameraKind;
import br.edu.unipampa.varzinho.application.ManageGym;
import br.edu.unipampa.varzinho.enums.Resolution;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.util.List;
import java.util.function.Consumer;

/** Arranges the four independent parts of the court area. */
final class CourtPanel extends JPanel {
    private final CourtSelectionPanel courtSelection;
    private final CameraListPanel cameraList;
    private final CameraInstallPanel cameraInstallation;
    private final CameraPositionPanel cameraPosition;
    private final JPanel cameraForms;
    private final CourtOverviewPanel courtOverview;

    CourtPanel(ManageGym management, int bufferSeconds, Consumer<String> errorSink) {
        super(new BorderLayout(12, 12));
        courtSelection = new CourtSelectionPanel(management, errorSink);
        cameraList = new CameraListPanel(management, courtSelection::selectedCourtNumber, errorSink);
        cameraInstallation = new CameraInstallPanel(management, courtSelection::selectedCourtNumber,
                bufferSeconds, errorSink, cameraList::refresh);
        cameraPosition = new CameraPositionPanel(management, courtSelection::selectedCourtNumber,
                cameraList::selectedCamera, errorSink, cameraList::refreshKeepingSelection);
        courtOverview = new CourtOverviewPanel(management, this::selectCourt);
        cameraList.onAdd(() -> {
            javax.swing.JDialog dialog = new javax.swing.JDialog(javax.swing.SwingUtilities.getWindowAncestor(cameraList),
                    "Adicionar câmera", java.awt.Dialog.ModalityType.DOCUMENT_MODAL);
            dialog.setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
            javax.swing.JLabel feedback = new javax.swing.JLabel(" ");
            CameraInstallPanel form = new CameraInstallPanel(management, this::selectedCourtNumber, bufferSeconds,
                    msg -> { feedback.setText(msg); errorSink.accept(msg); },
                    () -> { cameraList.refresh(); dialog.dispose(); });
            JButton cancel = new JButton("Cancelar");
            cancel.addActionListener(event -> dialog.dispose());
            JPanel footer = new JPanel(new BorderLayout());
            footer.add(feedback, BorderLayout.CENTER);
            footer.add(cancel, BorderLayout.EAST);
            dialog.add(form);
            dialog.add(footer, BorderLayout.SOUTH);
            dialog.getRootPane().setDefaultButton(form.installButton());
            dialog.pack();
            dialog.setLocationRelativeTo(cameraList);
            dialog.setVisible(true);
        });

        courtSelection.onCourtChange(() -> {
            cameraList.refresh();
            cameraPosition.updateAimAvailability();
        });
        courtSelection.onCourtAdded(() -> {
            cameraPosition.addCourt(courtSelection.selectedCourtNumber());
            courtOverview.refresh();
        });
        cameraList.onSelectionChange(cameraPosition::updateAimAvailability);

        cameraForms = new JPanel(new net.miginfocom.swing.MigLayout(
                "insets 0, fillx, wrap 1", "[grow,fill]", "[][]"));
        cameraForms.putClientProperty("FlatLaf.style", "background: $VarZinho.contentBackground");
        cameraForms.add(cameraPosition, "growx");
        cameraForms.add(cameraInstallation, "growx");
        add(courtSelection, BorderLayout.NORTH);
        add(cameraList, BorderLayout.CENTER);
        add(cameraForms, BorderLayout.SOUTH);
    }

    JPanel courtPage() {
        remove(courtSelection);
        JPanel page = new JPanel(new BorderLayout());
        page.add(courtSelection, BorderLayout.NORTH);
        page.add(courtOverview, BorderLayout.CENTER);
        return page;
    }

    JPanel cameraPage() {
        cameraForms.remove(cameraInstallation);
        remove(cameraList);
        remove(cameraForms);
        JPanel page = new JPanel(new BorderLayout(12, 12));
        page.add(cameraList, BorderLayout.CENTER);
        page.add(cameraForms, BorderLayout.SOUTH);
        return page;
    }

    CameraListPanel devices() { return cameraList; }

    int selectedCourtNumber() { return courtSelection.selectedCourtNumber(); }
    void selectCourt(int number) { courtSelection.selectCourt(number); }
    void onCourtChange(Runnable listener) { courtSelection.onCourtChange(listener); }
    void onCourtAdded(Runnable listener) { courtSelection.onCourtAdded(listener); }
    void refreshCourtOverview() { courtOverview.refresh(); }
    void selectCamera(int index) { cameraList.selectCamera(index); }
    List<String> cameraLines() { return cameraList.cameraLines(); }
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
