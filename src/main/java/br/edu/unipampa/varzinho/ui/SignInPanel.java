package br.edu.unipampa.varzinho.ui;

import br.edu.unipampa.varzinho.application.OperatorAccess;
import br.edu.unipampa.varzinho.application.SignedInOperator;
import br.edu.unipampa.varzinho.application.port.RepositoryException;
import net.miginfocom.swing.MigLayout;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.awt.Component;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Asks a stored operator for a username and a password. Whether the credentials are
 * right is {@link OperatorAccess}'s decision; the panel only relays its answer, so a
 * refusal never says which of the two was wrong.
 *
 * <p><b>Esqueci minha senha</b> opens the simulated password recovery.
 */
final class SignInPanel extends JPanel {
    /** Runs the password recovery over this panel, answering the username it reset. */
    @FunctionalInterface
    interface PasswordRecovery {
        Optional<String> recover(Component parent);
    }

    private static final Logger LOGGER = Logger.getLogger(SignInPanel.class.getName());
    private final OperatorAccess access;
    private final Consumer<String> errorSink;
    private final Consumer<SignedInOperator> onSignedIn;
    private final PasswordRecovery recovery;
    private final JTextField usernameField = new JTextField(16);
    private final JPasswordField passwordField = new JPasswordField(16);
    private final JButton signIn = new JButton("Entrar");
    private final JButton forgotPassword = new JButton("Esqueci minha senha");

    SignInPanel(OperatorAccess access, Consumer<String> errorSink, Consumer<SignedInOperator> onSignedIn) {
        this(access, errorSink, onSignedIn, parent -> PasswordRecoveryDialog.ask(parent, access));
    }

    SignInPanel(OperatorAccess access, Consumer<String> errorSink, Consumer<SignedInOperator> onSignedIn,
                PasswordRecovery recovery) {
        super(new MigLayout("insets 28, fillx, wrap 1", "[360,grow,fill]", "[][]18[][]12[][]24[]8[]"));
        if (access == null) throw new IllegalArgumentException("signing in needs operator access");
        if (errorSink == null) throw new IllegalArgumentException("signing in needs an error destination");
        if (onSignedIn == null) throw new IllegalArgumentException("signing in needs somewhere to go next");
        this.access = access;
        this.errorSink = errorSink;
        this.onSignedIn = onSignedIn;
        if (recovery == null) throw new IllegalArgumentException("signing in needs a password recovery");
        this.recovery = recovery;
        JLabel brand = new JLabel("VARZINHO ARENA");
        VarZinhoTheme.styleEyebrow(brand);
        JLabel title = new JLabel("Bem-vindo de volta");
        VarZinhoTheme.styleTitle(title);
        JLabel help = new JLabel("Entre para gerenciar quadras e salvar melhores momentos.");
        add(brand);
        add(title);
        add(help);
        add(new JLabel("Usuário"));
        add(usernameField, "h 38!");
        add(new JLabel("Senha"));
        add(passwordField, "h 38!");
        VarZinhoTheme.styleProminentAction(signIn);
        add(signIn, "h 42!");
        signIn.addActionListener(event -> signIn());
        forgotPassword.putClientProperty("JButton.buttonType", "borderless");
        add(forgotPassword, "growx 0, center");
        forgotPassword.addActionListener(event -> recoverPassword());
    }

    JTextField usernameField() { return usernameField; }
    JPasswordField passwordField() { return passwordField; }
    JButton signInButton() { return signIn; }
    JButton forgotPasswordButton() { return forgotPassword; }

    private void recoverPassword() {
        recovery.recover(this).ifPresent(username -> {
            usernameField.setText(username);
            passwordField.setText("");
            passwordField.requestFocusInWindow();
            errorSink.accept("Senha alterada. Entre com a nova senha.");
        });
    }

    // Hashing the password and reading the account both wait, so they run on a worker.
    private void signIn() {
        String username = usernameField.getText();
        char[] typed = passwordField.getPassword();
        String password = new String(typed);
        Arrays.fill(typed, '\0');
        RepositoryTask.run(List.of(signIn, forgotPassword), "Entrando…", () -> access.signIn(username, password),
                onSignedIn, this::refused);
    }

    private void refused(Throwable failure) {
        if (failure instanceof IllegalArgumentException) {
            passwordField.setText("");
            errorSink.accept(PortugueseText.message(failure.getMessage()));
            return;
        }
        LOGGER.log(failure instanceof RepositoryException ? Level.WARNING : Level.SEVERE,
                "Could not read the operator account", failure);
        errorSink.accept("Não foi possível ler os operadores. Verifique o armazenamento.");
    }
}
