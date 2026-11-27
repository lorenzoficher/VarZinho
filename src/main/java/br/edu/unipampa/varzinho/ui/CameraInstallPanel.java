package br.edu.unipampa.varzinho.ui;

import br.edu.unipampa.varzinho.application.CameraKind;
import br.edu.unipampa.varzinho.application.CameraSpec;
import br.edu.unipampa.varzinho.application.ManageGym;
import br.edu.unipampa.varzinho.enums.Resolution;
import net.miginfocom.swing.MigLayout;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import java.awt.Component;
import java.util.function.Consumer;
import java.util.function.IntSupplier;

/** Collects a camera specification and installs it on the selected court. */
final class CameraInstallPanel extends JPanel {
    private final ManageGym management;
    private final GymChanges changes;
    private final IntSupplier selectedCourt;
    private final int bufferSeconds;
    private final Consumer<String> errorSink;
    private final Runnable onInstalled;
    private final JTextField idField = new JTextField(8);
    private final JTextField modelField = new JTextField(8);
    private final JComboBox<CameraKind> typeSelector =
            new JComboBox<>(CameraKind.installable().toArray(CameraKind[]::new));
    private final JComboBox<Resolution> resolutionSelector = new JComboBox<>(Resolution.values());
    private final JSpinner angleSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 359, 1));
    private final JButton install = new JButton("Cadastrar câmera");
    private final JLabel destination = new JLabel();
    private final JLabel feedback = new JLabel("A câmera será cadastrada como parada.");

    CameraInstallPanel(ManageGym management, GymChanges changes, IntSupplier selectedCourt, int bufferSeconds,
                       Consumer<String> errorSink, Runnable onInstalled) {
        super(new MigLayout("insets 16, fillx, wrap 2", "[180:220,grow,fill]12[180:220,grow,fill]"));
        if (management == null) throw new IllegalArgumentException("camera installation needs gym management");
        if (changes == null) throw new IllegalArgumentException("camera installation needs a way to change the gym");
        if (selectedCourt == null) throw new IllegalArgumentException("camera installation needs a selected court");
        if (bufferSeconds <= 0) throw new IllegalArgumentException("a buffer holds at least one second");
        if (errorSink == null) throw new IllegalArgumentException("camera installation needs an error destination");
        if (onInstalled == null) throw new IllegalArgumentException("camera installation needs a refresh action");
        this.management = management;
        this.changes = changes;
        this.selectedCourt = selectedCourt;
        this.bufferSeconds = bufferSeconds;
        this.errorSink = errorSink;
        this.onInstalled = onInstalled;
        VarZinhoTheme.styleCard(this);
        VarZinhoTheme.styleButton(install, ButtonRole.PRIMARY);
        typeSelector.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                    int index, boolean selected, boolean focused) {
                String label = value instanceof CameraKind kind ? PortugueseText.cameraKind(kind) : "";
                return super.getListCellRendererComponent(list, label, index, selected, focused);
            }
        });
        resolutionSelector.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                    int index, boolean selected, boolean focused) {
                String label = value instanceof Resolution resolution ? resolution.label() : "";
                return super.getListCellRendererComponent(list, label, index, selected, focused);
            }
        });
        JLabel title = new JLabel("Adicionar câmera");
        VarZinhoTheme.styleTitle(title);
        VarZinhoTheme.styleEyebrow(destination);
        showDestination();
        add(destination, "span");
        add(title, "span, gapbottom 14");
        add(new JLabel("ID da câmera")); add(new JLabel("Modelo"));
        add(idField, VarZinhoTheme.CONTROL_SIZE); add(modelField, VarZinhoTheme.CONTROL_SIZE);
        add(new JLabel("Tipo")); add(new JLabel("Resolução"));
        add(typeSelector, VarZinhoTheme.CONTROL_SIZE); add(resolutionSelector, VarZinhoTheme.CONTROL_SIZE);
        add(new JLabel("Ângulo de visão (graus)"), "span"); add(angleSpinner, "span, growx, " + VarZinhoTheme.CONTROL_SIZE);
        add(feedback, "span, gapy 12");
        add(install, "skip 1, " + VarZinhoTheme.CONTROL_SIZE);
        typeSelector.addActionListener(
                event -> angleSpinner.setEnabled(typeSelector.getSelectedItem() == CameraKind.FIXED));
        install.addActionListener(event -> install());
        EnterKey.submits(install, idField, modelField, angleSpinner);
    }

    JTextField idField() { return idField; }
    JTextField modelField() { return modelField; }
    JComboBox<CameraKind> typeSelector() { return typeSelector; }
    JComboBox<Resolution> resolutionSelector() { return resolutionSelector; }
    JSpinner angleSpinner() { return angleSpinner; }
    JButton installButton() { return install; }

    void showDestination() {
        destination.setText("QUADRA " + selectedCourt.getAsInt());
        feedback.setText("A câmera será cadastrada como parada.");
    }

    private void install() {
        int number = selectedCourt.getAsInt();
        CameraSpec specification = new CameraSpec(idField.getText(), modelField.getText(),
                (CameraKind) typeSelector.getSelectedItem(), (Resolution) resolutionSelector.getSelectedItem(),
                bufferSeconds, (Integer) angleSpinner.getValue());
        changes.apply(install, () -> management.installCamera(number, specification),
                message -> { feedback.setText(message); errorSink.accept(message); },
                result -> installed(specification.getKind(), result));
    }

    private void installed(CameraKind kind, GymChanges.Result result) {
        if (result == GymChanges.Result.SAVED) {
            feedback.setText(kind == CameraKind.PTZ
                    ? "PTZ cadastrada. Teste o movimento no painel."
                    : "Câmera cadastrada. Informe outro ID para adicionar mais uma.");
            idField.setText("");
            idField.requestFocusInWindow();
        }
        if (result != GymChanges.Result.REFUSED) onInstalled.run();
    }
}
