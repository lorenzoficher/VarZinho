package br.edu.unipampa.varzinho.ui;

import br.edu.unipampa.varzinho.application.OperatorAccess;
import br.edu.unipampa.varzinho.application.SignedInOperator;
import br.edu.unipampa.varzinho.application.port.RepositoryException;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Window;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * The gate in front of the main window: the sign-in form, or, on a run with no operator
 * stored yet, the form that creates the first one. It has no sign-out — closing the main
 * window is what ends a session.
 */
public final class SignInDialog extends JDialog {
    private SignedInOperator signedIn;

    private SignInDialog(OperatorAccess access) throws RepositoryException {
        // Owned by no window at all: a null Frame owner would hang it on Swing's hidden
        // shared frame, and the window manager would then leave it off the taskbar, where
        // it is lost the moment another window takes focus.
        super((Window) null, ModalityType.APPLICATION_MODAL);
        JLabel feedback = new JLabel(" ");
        feedback.getAccessibleContext().setAccessibleName("Mensagem de acesso");
        JPanel form = formFor(access, message -> {
            feedback.setText(message);
            pack();
        }, this::admit);
        if (form instanceof FirstOperatorPanel firstForm) {
            setTitle("VarZinho Arena");
            getRootPane().setDefaultButton(firstForm.createButton());
        } else {
            setTitle("VarZinho Arena");
            getRootPane().setDefaultButton(((SignInPanel) form).signInButton());
        }
        add(form, BorderLayout.CENTER);
        feedback.setBorder(BorderFactory.createEmptyBorder(0, 10, 8, 10));
        add(feedback, BorderLayout.SOUTH);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        pack();
        setResizable(false);
        setLocationRelativeTo(null);
    }

    /**
     * Shows the gate and waits until it closes.
     *
     * @param access the operator store used to sign in and to create the first operator
     * @return the operator who signed in, or empty when the gate was closed without one
     * @throws RepositoryException if the operator store cannot tell whether any operator
     *         exists yet
     */
    public static Optional<SignedInOperator> ask(OperatorAccess access) throws RepositoryException {
        if (access == null) throw new IllegalArgumentException("the sign-in gate needs operator access");
        SignInDialog dialog = new SignInDialog(access);
        dialog.setVisible(true);
        return Optional.ofNullable(dialog.signedIn);
    }

    static JPanel formFor(OperatorAccess access, Consumer<String> errorSink, Consumer<SignedInOperator> onSignedIn)
            throws RepositoryException {
        if (access.needsFirstOperator()) {
            return new FirstOperatorPanel(access, errorSink, onSignedIn);
        }
        return new SignInPanel(access, errorSink, onSignedIn);
    }

    private void admit(SignedInOperator operator) {
        signedIn = operator;
        dispose();
    }

}
