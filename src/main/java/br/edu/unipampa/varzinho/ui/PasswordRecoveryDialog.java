package br.edu.unipampa.varzinho.ui;

import br.edu.unipampa.varzinho.application.OperatorAccess;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Component;
import java.util.Optional;

/** The window that holds the simulated password recovery, over the sign-in gate. */
final class PasswordRecoveryDialog extends JDialog {
    private String resetUsername;

    PasswordRecoveryDialog(Component parent, OperatorAccess access) {
        super(SwingUtilities.getWindowAncestor(parent), "Recuperar senha", ModalityType.APPLICATION_MODAL);
        JLabel feedback = new JLabel(" ");
        feedback.getAccessibleContext().setAccessibleName("Mensagem de recuperação");
        feedback.setBorder(BorderFactory.createEmptyBorder(0, 10, 8, 10));
        PasswordRecoveryPanel recovery = new PasswordRecoveryPanel(access, message -> {
            feedback.setText(message);
            pack();
        }, this::finish);
        // Swing runs a button's listeners newest first, so the panel has not changed step
        // yet when these fire; the default button is moved once it has.
        recovery.sendLinkButton().addActionListener(event -> SwingUtilities.invokeLater(() -> pressEnterOn(recovery)));
        recovery.proceedButton().addActionListener(event -> SwingUtilities.invokeLater(() -> pressEnterOn(recovery)));
        add(recovery, BorderLayout.CENTER);
        add(feedback, BorderLayout.SOUTH);
        pressEnterOn(recovery);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        EscapeKey.closes(this);
        pack();
        setResizable(false);
        setLocationRelativeTo(parent);
    }

    /**
     * Shows the recovery and waits until it closes.
     *
     * @return the username whose password was replaced; empty when the window was closed
     *     before the new password was saved
     */
    static Optional<String> ask(Component parent, OperatorAccess access) {
        PasswordRecoveryDialog dialog = new PasswordRecoveryDialog(parent, access);
        dialog.setVisible(true);
        return Optional.ofNullable(dialog.resetUsername);
    }

    private void pressEnterOn(PasswordRecoveryPanel recovery) {
        getRootPane().setDefaultButton(recovery.currentAction());
    }

    private void finish(String username) {
        resetUsername = username;
        dispose();
    }
}
