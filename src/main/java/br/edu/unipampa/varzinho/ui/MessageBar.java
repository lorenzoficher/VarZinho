package br.edu.unipampa.varzinho.ui;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;

/** The single, persistent destination for operational feedback in the main window. */
final class MessageBar extends JPanel {
    private final JLabel text = new JLabel("Pronto para operar.");

    MessageBar() {
        super(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        add(text, BorderLayout.CENTER);
    }

    void show(String message) {
        text.setText(message == null || message.isBlank() ? "Não foi possível concluir a operação." : message);
    }

    String message() {
        return text.getText();
    }
}
