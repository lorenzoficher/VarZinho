package br.edu.unipampa.varzinho.ui;

import br.edu.unipampa.varzinho.application.OperatorAccess;
import br.edu.unipampa.varzinho.application.SignedInOperator;
import br.edu.unipampa.varzinho.application.port.RepositoryException;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.awt.GridLayout;
import java.util.Arrays;
import java.util.function.Consumer;

/**
 * Asks a stored operator for a username and a password. Whether the credentials are
 * right is {@link OperatorAccess}'s decision; the panel only relays its answer, so a
 * refusal never says which of the two was wrong.
 */
final class SignInPanel extends JPanel {
    private final OperatorAccess access;
    private final Consumer<String> errorSink;
    private final Consumer<SignedInOperator> onSignedIn;
    private final JTextField usernameField = new JTextField(16);
    private final JPasswordField passwordField = new JPasswordField(16);
    private final JButton signIn = new JButton("Sign in");

    SignInPanel(OperatorAccess access, Consumer<String> errorSink, Consumer<SignedInOperator> onSignedIn) {
        super(new GridLayout(3, 2, 6, 6));
        if (access == null) throw new IllegalArgumentException("signing in needs operator access");
        if (errorSink == null) throw new IllegalArgumentException("signing in needs an error destination");
        if (onSignedIn == null) throw new IllegalArgumentException("signing in needs somewhere to go next");
        this.access = access;
        this.errorSink = errorSink;
        this.onSignedIn = onSignedIn;
        add(new JLabel("Username")); add(usernameField);
        add(new JLabel("Password")); add(passwordField);
        add(new JLabel()); add(signIn);
        signIn.addActionListener(event -> signIn());
    }

    JTextField usernameField() { return usernameField; }
    JPasswordField passwordField() { return passwordField; }
    JButton signInButton() { return signIn; }

    private void signIn() {
        char[] typed = passwordField.getPassword();
        try {
            onSignedIn.accept(access.signIn(usernameField.getText(), new String(typed)));
        } catch (IllegalArgumentException refused) {
            passwordField.setText("");
            errorSink.accept(refused.getMessage());
        } catch (RepositoryException unreadable) {
            errorSink.accept("The operators could not be read: " + unreadable.getMessage());
        } finally {
            Arrays.fill(typed, '\0');
        }
    }
}
