package br.edu.unipampa.varzinho.ui;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.util.List;

/** The single, persistent destination for operational feedback in the main window. */
final class MessageBar extends JPanel {
    private static final String READY = "Pronto para operar.";
    private final JLabel text = new JLabel(READY);

    MessageBar() {
        super(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        add(text, BorderLayout.CENTER);
    }

    void show(String message) {
        text.setText(message == null || message.isBlank() ? "Não foi possível concluir a operação." : message);
        text.setToolTipText(text.getText());
    }

    String message() {
        return text.getText();
    }

    void showStartupWarnings(List<String> warnings) {
        if (warnings.isEmpty()) return;
        String combined = String.join(" ", warnings);
        show(READY.equals(message()) ? combined : combined + " " + message());
    }
}
