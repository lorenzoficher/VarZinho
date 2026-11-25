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
    private final GymChanges changes;
    private final IntSupplier selectedCourt;
    private final Consumer<String> messages;
    private final JComboBox<String> cameras = new JComboBox<>();
    private final JButton savePosition = new JButton("Salvar posição");
    private final Runnable onInstalled;
    private Integer previewCourt;
    private final JSpinner pan = new JSpinner(new SpinnerNumberModel(0, 0, 359, 5));
    private final JSpinner tilt = new JSpinner(new SpinnerNumberModel(0, -90, 90, 5));
    private final JLabel zoomValue = new JLabel("1×", JLabel.CENTER);
    private final JLabel position = new JLabel();
    private final JSpinner zoom = new JSpinner(new SpinnerNumberModel(1, 1, 10, 1));

    QuickPtzPanel(ManageGym management, GymChanges changes, IntSupplier selectedCourt, Consumer<String> messages,
                  Runnable onInstalled) {
        super(new MigLayout("insets 6, fillx, hidemode 3", "[grow,fill][pref]", "[][]6[]2[]"));
        if (management == null) throw new IllegalArgumentException("quick PTZ needs gym management");
        if (changes == null) throw new IllegalArgumentException("quick PTZ needs a way to change the gym");
        if (selectedCourt == null) throw new IllegalArgumentException("quick PTZ needs a selected court");
        if (messages == null) throw new IllegalArgumentException("quick PTZ needs a message destination");
        if (onInstalled == null) throw new IllegalArgumentException("quick PTZ needs an installation listener");
        this.onInstalled = onInstalled;
        this.management = management;
        this.changes = changes;
        this.selectedCourt = selectedCourt;
        this.messages = messages;
        VarZinhoTheme.styleCard(this);

        JLabel eyebrow = new JLabel("CONTROLE RÁPIDO");
        VarZinhoTheme.styleEyebrow(eyebrow);
        JLabel title = new JLabel("Movimento PTZ");
        VarZinhoTheme.styleTitle(title);
        cameras.getAccessibleContext().setAccessibleName("Câmera para controle PTZ");
        add(eyebrow, "cell 0 0");
        add(cameras, "cell 1 0, alignx right, w 105:145:170");
        add(title, "cell 0 1, span 2");
        JPanel pad = new JPanel(new MigLayout("insets 0", "[][][]12[105:130:140,fill]", "[][][]"));
        pad.setOpaque(false);
        pad.add(step("↑", tilt, 5), "cell 1 0");
        pad.add(step("←", pan, -5), "cell 0 1");
        JButton center = new JButton("●");
        center.setToolTipText("Centralizar câmera");
        center.getAccessibleContext().setAccessibleName(center.getToolTipText());
        center.addActionListener(e -> { pan.setValue(0); tilt.setValue(0); move(center); });
        pad.add(center, "cell 1 1");
        pad.add(step("→", pan, 5), "cell 2 1");
        pad.add(step("↓", tilt, -5), "cell 1 2");
        pad.add(new JLabel("Zoom"), "cell 3 0, center");
        JPanel zoomPad = new JPanel(new MigLayout("insets 0, gap 2, fillx", "[][grow,fill][]"));
        zoomPad.setOpaque(false);
        zoomPad.add(step("−", zoom, -1));
        zoomPad.add(zoomValue);
        zoomPad.add(step("+", zoom, 1));
        pad.add(zoomPad, "cell 3 1, growx");
        zoom.addChangeListener(e -> zoomValue.setText(zoom.getValue() + "×"));
        savePosition.addActionListener(event -> save(savePosition));
        pad.add(savePosition, "cell 3 2");
        add(pad, "cell 0 2, span 2");
        add(position, "cell 0 3, span 2");
        cameras.addActionListener(e -> syncAim());
        refresh();
    }

    void refresh() {
        int court = selectedCourt.getAsInt();
        String selected = (String) cameras.getSelectedItem();
        List<CameraView> aimable = management.court(court).getCameras().stream()
                .filter(CameraView::isAimable)
                .filter(camera -> !camera.isUnderMaintenance())
                .toList();
        cameras.removeAllItems();
        if (aimable.isEmpty()) {
            if (!Integer.valueOf(court).equals(previewCourt)) {
                pan.setValue(0);
                tilt.setValue(0);
                zoom.setValue(1);
            }
            previewCourt = court;
            cameras.addItem("PTZ de teste");
            savePosition.setToolTipText("Cadastrar a câmera simulada nesta quadra com a posição indicada");
        } else {
            previewCourt = null;
            aimable.forEach(camera -> cameras.addItem(camera.getId()));
            if (aimable.stream().anyMatch(camera -> camera.getId().equals(selected))) cameras.setSelectedItem(selected);
            savePosition.setToolTipText("Salvar a posição da câmera selecionada");
        }
        syncAim();
    }

    private void syncAim() {
        if (previewCourt == null) {
            management.court(selectedCourt.getAsInt()).getCameras().stream()
                    .filter(camera -> camera.getId().equals(cameras.getSelectedItem())).findFirst().ifPresent(camera -> {
                        pan.setValue(camera.getPan().orElseThrow());
                        tilt.setValue(camera.getTilt().orElseThrow());
                        zoom.setValue(camera.getZoom().orElseThrow());
                    });
        }
        zoomValue.setText(zoom.getValue() + "×");
        position.setText("Panorâmica " + pan.getValue() + "°   ·   Inclinação " + tilt.getValue() + "°");
    }

    private JButton step(String text, JSpinner value, int amount) {
        JButton button = new JButton(text);
        button.setToolTipText(switch (text) {
            case "↑" -> "Inclinar para cima";
            case "↓" -> "Inclinar para baixo";
            case "←" -> "Girar à esquerda";
            case "→" -> "Girar à direita";
            case "+" -> "Aumentar zoom";
            default -> "Diminuir zoom";
        });
        button.getAccessibleContext().setAccessibleName(button.getToolTipText());
        button.setPreferredSize(new java.awt.Dimension(34, 32));
        button.addActionListener(event -> {
            int current = (Integer) value.getValue();
            SpinnerNumberModel model = (SpinnerNumberModel) value.getModel();
            int next = value == pan ? Math.floorMod(current + amount, 360)
                    : Math.max((Integer) model.getMinimum(), Math.min((Integer) model.getMaximum(), current + amount));
            value.setValue(next);
            move(button);
        });
        return button;
    }

    private void move(JButton trigger) {
        if (previewCourt != null) syncAim();
        else save(trigger);
    }

    private void save(JButton trigger) {
        int panning = (Integer) pan.getValue();
        int tilting = (Integer) tilt.getValue();
        int zooming = (Integer) zoom.getValue();
        if (previewCourt != null) {
            int court = previewCourt;
            changes.apply(trigger, () -> management.installSimulatedPtz(court, panning, tilting, zooming), messages,
                    result -> {
                        if (result != GymChanges.Result.REFUSED) {
                            refresh();
                            onInstalled.run();
                        }
                        if (result == GymChanges.Result.SAVED) {
                            messages.accept("PTZ simulada cadastrada com a posição escolhida.");
                        }
                    });
            return;
        }
        int court = selectedCourt.getAsInt();
        String camera = (String) cameras.getSelectedItem();
        changes.apply(trigger, () -> management.aimCamera(court, camera, panning, tilting, zooming), messages,
                result -> {
                    if (result == GymChanges.Result.SAVED) {
                        messages.accept("Posição da câmera " + camera + " atualizada.");
                    }
                    syncAim();
                });
    }
}
