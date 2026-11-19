package br.edu.unipampa.varzinho.ui;

import br.edu.unipampa.varzinho.application.CameraView;
import br.edu.unipampa.varzinho.application.ManageGym;
import net.miginfocom.swing.MigLayout;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntSupplier;

/** Compact PTZ control for the dashboard, backed by the same gym use case as the full page. */
final class QuickPtzPanel extends JPanel {
    private final ManageGym management;
    private final IntSupplier selectedCourt;
    private final Consumer<String> messages;
    private final JComboBox<String> cameras = new JComboBox<>();
    private final JSpinner pan = new JSpinner(new SpinnerNumberModel(0, 0, 359, 5));
    private final JSpinner tilt = new JSpinner(new SpinnerNumberModel(0, -90, 90, 5));
    private final JSpinner zoom = new JSpinner(new SpinnerNumberModel(1, 1, 10, 1));

    QuickPtzPanel(ManageGym management, IntSupplier selectedCourt, Consumer<String> messages) {
        super(new MigLayout("insets 6, fillx", "[][][]12[grow,fill]", "[][]12[][][]"));
        if (management == null) throw new IllegalArgumentException("quick PTZ needs gym management");
        if (selectedCourt == null) throw new IllegalArgumentException("quick PTZ needs a selected court");
        if (messages == null) throw new IllegalArgumentException("quick PTZ needs a message destination");
        this.management = management;
        this.selectedCourt = selectedCourt;
        this.messages = messages;
        VarZinhoTheme.styleCard(this);

        JLabel eyebrow = new JLabel("CONTROLE RÁPIDO");
        VarZinhoTheme.styleEyebrow(eyebrow);
        JLabel title = new JLabel("Movimento PTZ");
        VarZinhoTheme.styleTitle(title);
        add(eyebrow, "cell 0 0, span 3");
        add(cameras, "cell 3 0");
        add(title, "cell 0 1, span 4");
        add(step("↑", tilt, 5), "cell 1 2");
        add(step("←", pan, -5), "cell 0 3");
        JButton center = new JButton("●");
        center.setToolTipText("Centralizar câmera");
        center.addActionListener(e -> { pan.setValue(0); tilt.setValue(0); apply(); });
        add(center, "cell 1 3");
        add(step("→", pan, 5), "cell 2 3");
        add(step("↓", tilt, -5), "cell 1 4");
        add(new JLabel("Zoom"), "cell 3 2, center");
        add(zoom, "cell 3 3, growx");
        JButton apply = new JButton("Salvar posição");
        apply.addActionListener(event -> apply());
        add(apply, "cell 3 4");
        cameras.addActionListener(e -> syncAim());
        refresh();
    }

    void refresh() {
        String selected = (String) cameras.getSelectedItem();
        cameras.removeAllItems();
        List<CameraView> aimable = management.court(selectedCourt.getAsInt()).getCameras().stream()
                .filter(CameraView::isAimable)
                .toList();
        aimable.forEach(camera -> cameras.addItem(camera.getId()));
        if (selected != null) cameras.setSelectedItem(selected);
        boolean available = cameras.getItemCount() > 0;
        cameras.setEnabled(available);
        pan.setEnabled(available);
        tilt.setEnabled(available);
        zoom.setEnabled(available);
        for (java.awt.Component component : getComponents()) {
            if (component instanceof JButton button) button.setEnabled(available);
        }
        syncAim();
    }

    private void syncAim() {
        management.court(selectedCourt.getAsInt()).getCameras().stream()
                .filter(camera -> camera.getId().equals(cameras.getSelectedItem())).findFirst().ifPresent(camera -> {
                    pan.setValue(camera.getPan());
                    tilt.setValue(camera.getTilt());
                    zoom.setValue(camera.getZoom());
                });
    }

    private JButton step(String text, JSpinner value, int amount) {
        JButton button = new JButton(text);
        button.addActionListener(event -> {
            int current = (Integer) value.getValue();
            SpinnerNumberModel model = (SpinnerNumberModel) value.getModel();
            int next = Math.max((Integer) model.getMinimum(), Math.min((Integer) model.getMaximum(), current + amount));
            value.setValue(next);
            apply();
        });
        return button;
    }

    private void apply() {
        String camera = (String) cameras.getSelectedItem();
        if (camera == null) {
            messages.accept("A quadra selecionada não possui câmera PTZ.");
            return;
        }
        boolean[] failureReported = {false};
        boolean accepted = GymChangeReporter.attempt(() -> management.aimCamera(selectedCourt.getAsInt(), camera,
                (Integer) pan.getValue(), (Integer) tilt.getValue(), (Integer) zoom.getValue()),
                message -> {
                    failureReported[0] = true;
                    messages.accept(PortugueseText.message(message));
                });
        if (accepted && !failureReported[0]) {
            messages.accept("Posição da câmera " + camera + " atualizada.");
        }
    }
}
