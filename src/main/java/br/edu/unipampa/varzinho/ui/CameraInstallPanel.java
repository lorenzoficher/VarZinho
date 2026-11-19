package br.edu.unipampa.varzinho.ui;

import br.edu.unipampa.varzinho.application.CameraKind;
import br.edu.unipampa.varzinho.application.CameraSpec;
import br.edu.unipampa.varzinho.application.ManageGym;
import br.edu.unipampa.varzinho.enums.Resolution;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.function.Consumer;
import java.util.function.IntSupplier;

/** Collects a camera specification and installs it on the selected court. */
final class CameraInstallPanel extends JPanel {
    private final ManageGym management;
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

    CameraInstallPanel(ManageGym management, IntSupplier selectedCourt, int bufferSeconds,
                       Consumer<String> errorSink, Runnable onInstalled) {
        super(new GridLayout(2, 1));
        if (management == null) throw new IllegalArgumentException("camera installation needs gym management");
        if (selectedCourt == null) throw new IllegalArgumentException("camera installation needs a selected court");
        if (bufferSeconds <= 0) throw new IllegalArgumentException("a buffer holds at least one second");
        if (errorSink == null) throw new IllegalArgumentException("camera installation needs an error destination");
        if (onInstalled == null) throw new IllegalArgumentException("camera installation needs a refresh action");
        this.management = management;
        this.selectedCourt = selectedCourt;
        this.bufferSeconds = bufferSeconds;
        this.errorSink = errorSink;
        this.onInstalled = onInstalled;
        typeSelector.setRenderer(new DefaultListCellRenderer() {
            @Override
            public java.awt.Component getListCellRendererComponent(javax.swing.JList<?> list, Object value,
                    int index, boolean selected, boolean focused) {
                String label = value == CameraKind.FIXED ? "Fixa" : "PTZ";
                return super.getListCellRendererComponent(list, label, index, selected, focused);
            }
        });
        resolutionSelector.setRenderer(new DefaultListCellRenderer() {
            @Override
            public java.awt.Component getListCellRendererComponent(javax.swing.JList<?> list, Object value,
                    int index, boolean selected, boolean focused) {
                String label = value instanceof Resolution resolution ? resolution.label() : "";
                return super.getListCellRendererComponent(list, label, index, selected, focused);
            }
        });
        setBorder(BorderFactory.createTitledBorder("Adicionar câmera nesta quadra"));
        JPanel identity = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        identity.add(new JLabel("ID da câmera")); identity.add(idField);
        identity.add(new JLabel("Modelo")); identity.add(modelField);
        identity.add(new JLabel("Tipo")); identity.add(typeSelector);
        JPanel settings = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        settings.add(new JLabel("Resolução")); settings.add(resolutionSelector);
        settings.add(new JLabel("Ângulo de visão")); settings.add(angleSpinner); settings.add(install);
        add(identity); add(settings);
        typeSelector.addActionListener(
                event -> angleSpinner.setEnabled(typeSelector.getSelectedItem() == CameraKind.FIXED));
        install.addActionListener(event -> install());
    }

    JTextField idField() { return idField; }
    JTextField modelField() { return modelField; }
    JComboBox<CameraKind> typeSelector() { return typeSelector; }
    JComboBox<Resolution> resolutionSelector() { return resolutionSelector; }
    JSpinner angleSpinner() { return angleSpinner; }
    JButton installButton() { return install; }

    private void install() {
        CameraSpec specification = new CameraSpec(idField.getText(), modelField.getText(),
                (CameraKind) typeSelector.getSelectedItem(), (Resolution) resolutionSelector.getSelectedItem(),
                bufferSeconds, (Integer) angleSpinner.getValue());
        GymChangeReporter.attempt(() -> management.installCamera(selectedCourt.getAsInt(), specification),
                message -> errorSink.accept(PortugueseText.message(message)));
        onInstalled.run();
    }
}
