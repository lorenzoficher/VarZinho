package br.edu.unipampa.varzinho.ui;

import br.edu.unipampa.varzinho.application.CameraKind;
import br.edu.unipampa.varzinho.application.CameraView;
import br.edu.unipampa.varzinho.application.CourtView;
import br.edu.unipampa.varzinho.application.ManageGym;
import br.edu.unipampa.varzinho.enums.Resolution;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.IntPredicate;

/** Arranges the four independent parts of the court area. */
final class CourtPanel extends JPanel {
    private final List<Component> pausedControls = new ArrayList<>();
    private Runnable releaseInput;
    private final CourtSelectionPanel courtSelection;
    private final CameraListPanel cameraList;
    private final CameraInstallPanel cameraInstallation;
    private final CameraPositionPanel cameraPosition;
    private final JPanel cameraForms;
    private final CourtOverviewPanel courtOverview;
    private final JLabel installedTitle = new JLabel();
    private final JTable installedCameras;
    private final ManageGym management;
    private final List<Runnable> identityListeners = new ArrayList<>();
    private final GymChanges changes;
    private final int bufferSeconds;
    private final Consumer<String> errorSink;

    CourtPanel(ManageGym management, int bufferSeconds, Consumer<String> errorSink) {
        this(management, bufferSeconds, errorSink, new CaptureActivity());
    }

    /** @param activity the loan of the model this panel's changes share with captures */
    CourtPanel(ManageGym management, int bufferSeconds, Consumer<String> errorSink, CaptureActivity activity) {
        super(new BorderLayout(12, 12));
        this.management = management;
        this.bufferSeconds = bufferSeconds;
        this.errorSink = errorSink;
        changes = new GymChanges(activity, this::pauseChanges, this::resumeChanges);
        courtSelection = new CourtSelectionPanel(management, changes, errorSink);
        cameraList = new CameraListPanel(management, changes, courtSelection::selectedCourtNumber, errorSink);
        installedCameras = cameraList.summaryTable();
        cameraInstallation = new CameraInstallPanel(management, changes, courtSelection::selectedCourtNumber,
                bufferSeconds, errorSink, () -> {
                    cameraList.refresh();
                    refreshCourtOverview();
                });
        cameraPosition = new CameraPositionPanel(management, changes, courtSelection::selectedCourtNumber,
                cameraList::selectedCamera, errorSink, cameraList::refreshKeepingSelection);
        cameraList.onPosition(() -> cameraPosition.moveTargetSelector().requestFocusInWindow());
        courtOverview = new CourtOverviewPanel(this::selectCourt, this::sportOf, this::nameOf, null);
        refreshCourtOverview();
        cameraList.onConfigure(() -> cameraList.selectedCamera()
                .ifPresent(camera -> configureDialog(camera).setVisible(true)));
        cameraList.onAdd(() -> installDialog().setVisible(true));

        courtSelection.onCourtChange(() -> {
            cameraList.refresh();
            cameraPosition.updateAimAvailability();
            cameraInstallation.showDestination();
            refreshCourtOverview();
        });
        courtSelection.onCourtAdded(() -> {
            cameraPosition.addCourt(courtSelection.selectedCourtNumber());
            refreshCourtOverview();
        });
        courtSelection.onCourtRemoved(number -> {
            cameraPosition.removeCourt(number);
            refreshCourtOverview();
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
        JPanel installed = new JPanel(new BorderLayout(0, 12));
        VarZinhoTheme.styleCard(installed);
        VarZinhoTheme.styleTitle(installedTitle);
        installedTitle.setLabelFor(installedCameras);
        installed.add(installedTitle, BorderLayout.NORTH);
        installedCameras.setRowHeight(32);
        installedCameras.setPreferredScrollableViewportSize(new java.awt.Dimension(360, 160));
        installed.add(new JScrollPane(installedCameras), BorderLayout.CENTER);
        JPanel body = new JPanel(new net.miginfocom.swing.MigLayout(
                "insets 0, fillx, wrap 1", "[0:pref,grow,fill]", "[]16[]"));
        body.add(courtOverview);
        AdaptivePair cameras = new AdaptivePair(860, "[0:pref,grow,fill][0:pref,grow,fill]");
        cameras.add(cameraInstallation, "aligny top");
        cameras.add(installed, "growy");
        body.add(cameras);
        JPanel page = new JPanel(new BorderLayout());
        page.add(courtSelection, BorderLayout.NORTH);
        page.add(ScrollablePage.around(body), BorderLayout.CENTER);
        return page;
    }

    void pauseChanges() {
        javax.swing.JRootPane root = javax.swing.SwingUtilities.getRootPane(cameraList);
        if (root != null) {
            releaseInput = CaptureInputGuard.pause(root);
            return;
        }
        Component workspace = javax.swing.SwingUtilities.getAncestorOfClass(ArenaWorkspace.class, cameraList);
        pause(workspace == null ? this : workspace);
        pause(cameraList.startButton());
        pause(cameraList.stopButton());
        pause(cameraList.maintenanceButton());
        pause(cameraList.removeCameraButton());
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
        if (releaseInput != null) {
            releaseInput.run();
            releaseInput = null;
        }
        pausedControls.forEach(control -> control.setEnabled(true));
        pausedControls.clear();
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

    GymChanges gymChanges() { return changes; }

    int selectedCourtNumber() { return courtSelection.selectedCourtNumber(); }
    void selectCourt(int number) { courtSelection.selectCourt(number); }
    void onCourtChange(Runnable listener) { courtSelection.onCourtChange(listener); }
    void onCourtAdded(Runnable listener) { courtSelection.onCourtAdded(listener); }
    void onCourtRemoved(Runnable listener) { courtSelection.onCourtRemoved(number -> listener.run()); }
    void removeSelectedCourt(IntPredicate confirm) { courtSelection.removeSelectedCourt(confirm); }
    List<Integer> courtNumbers() { return courtSelection.courtNumbers(); }
    List<Integer> moveTargets() { return cameraPosition.moveTargets(); }
    void onCamerasChanged(Runnable listener) { cameraList.onCamerasChanged(listener); }
    /** Shows a change the panel did not make itself, such as the phone dropping. */
    void refreshCameras() { cameraList.refreshKeepingSelection(); }
    void refreshCourtOverview() {
        int number = selectedCourtNumber();
        courtOverview.refresh(management.courts(), number);
        var cameras = management.court(number).getCameras();
        installedTitle.setText("Câmeras da quadra " + number + " (" + cameras.size() + ")");
    }
    /** @return the sport the gym records for the court, or null when it has none */
    String sportOf(int number) { return court(number).flatMap(CourtView::getSport).orElse(null); }
    Optional<CameraView> selectedCamera() { return cameraList.selectedCamera(); }

    JDialog configureDialog(CameraView camera) {
        JDialog dialog = cameraDialog("Configurar câmera · " + camera.getId());
        JLabel feedback = new JLabel(" ");
        CameraPositionPanel controls = new CameraPositionPanel(management, changes, this::selectedCourtNumber,
                cameraList::selectedCamera, message -> { feedback.setText(message); errorSink.accept(message); },
                () -> { cameraList.refreshKeepingSelection(); dialog.dispose(); });
        dialog.add(controls);
        dialog.add(feedback, BorderLayout.SOUTH);
        dialog.pack();
        dialog.setLocationRelativeTo(cameraList);
        return dialog;
    }

    JDialog installDialog() {
        JDialog dialog = cameraDialog("Adicionar câmera");
        CameraInstallPanel form = new CameraInstallPanel(management, changes, this::selectedCourtNumber, bufferSeconds,
                errorSink, () -> { cameraList.refresh(); dialog.dispose(); });
        JButton cancel = new JButton("Cancelar");
        VarZinhoTheme.styleButton(cancel, ButtonRole.SECONDARY);
        cancel.addActionListener(event -> dialog.dispose());
        JPanel footer = new JPanel(new BorderLayout());
        footer.add(cancel, BorderLayout.EAST);
        dialog.add(form);
        dialog.add(footer, BorderLayout.SOUTH);
        dialog.getRootPane().setDefaultButton(form.installButton());
        dialog.pack();
        dialog.setLocationRelativeTo(cameraList);
        return dialog;
    }

    private JDialog cameraDialog(String title) {
        JDialog dialog = new JDialog(javax.swing.SwingUtilities.getWindowAncestor(cameraList), title,
                java.awt.Dialog.ModalityType.DOCUMENT_MODAL);
        dialog.setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        EscapeKey.closes(dialog);
        return dialog;
    }

    String nameOf(int number) {
        return court(number).flatMap(CourtView::getDisplayName).orElse("Quadra " + number);
    }

    // A selector may still offer a court another change just removed, so a missing court
    // reads as an unnamed one instead of failing the repaint.
    private Optional<CourtView> court(int number) {
        return management.courts().stream().filter(court -> court.getNumber() == number).findFirst();
    }
    void onIdentityChange(Runnable action) { identityListeners.add(action); }

    /** @param onSaved runs only when the gym stored the new name; a blank name clears it */
    void renameSelected(Component trigger, String name, Runnable onSaved) {
        int number = selectedCourtNumber();
        relabel(trigger, () -> management.renameCourt(number, name), onSaved);
    }

    /** @param onSaved runs only when the gym stored the new sport */
    void chooseSport(Component trigger, String sport, Runnable onSaved) {
        int number = selectedCourtNumber();
        relabel(trigger, () -> management.chooseSport(number, sport), onSaved);
    }

    // A label the gym could not store is undone, so every outcome repaints what it holds.
    private void relabel(Component trigger, GymChanges.Change change, Runnable onSaved) {
        changes.apply(trigger, change, errorSink, result -> {
            identityChanged();
            if (result == GymChanges.Result.SAVED) onSaved.run();
        });
    }

    private void identityChanged() {
        refreshCourtOverview();
        identityListeners.forEach(Runnable::run);
    }
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
    JButton removeCourtButton() { return courtSelection.removeCourtButton(); }
    JSpinner panSpinner() { return cameraPosition.panSpinner(); }
    JSpinner tiltSpinner() { return cameraPosition.tiltSpinner(); }
    JSpinner zoomSpinner() { return cameraPosition.zoomSpinner(); }
    JButton moveButton() { return cameraPosition.moveButton(); }
    JComboBox<Integer> moveTargetSelector() { return cameraPosition.moveTargetSelector(); }
    JButton moveToCourtButton() { return cameraPosition.moveToCourtButton(); }
}
