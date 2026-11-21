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
import java.util.Arrays;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Asks a stored operator for a username and a password. Whether the credentials are
 * right is {@link OperatorAccess}'s decision; the panel only relays its answer, so a
 * refusal never says which of the two was wrong.
 */
final class SignInPanel extends JPanel {
    private static final Logger LOGGER = Logger.getLogger(SignInPanel.class.getName());
    private final OperatorAccess access;
    private final Consumer<String> errorSink;
    private final Consumer<SignedInOperator> onSignedIn;
    private final JTextField usernameField = new JTextField(16);
    private final JPasswordField passwordField = new JPasswordField(16);
    private final JButton signIn = new JButton("Entrar");

    SignInPanel(OperatorAccess access, Consumer<String> errorSink, Consumer<SignedInOperator> onSignedIn) {
        super(new MigLayout("insets 28, fillx, wrap 1", "[360,grow,fill]", "[][]18[][]12[][]24[]"));
        if (access == null) throw new IllegalArgumentException("signing in needs operator access");
        if (errorSink == null) throw new IllegalArgumentException("signing in needs an error destination");
        if (onSignedIn == null) throw new IllegalArgumentException("signing in needs somewhere to go next");
        this.access = access;
        this.errorSink = errorSink;
        this.onSignedIn = onSignedIn;
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
            errorSink.accept(PortugueseText.message(refused.getMessage()));
        } catch (RepositoryException unreadable) {
            LOGGER.log(Level.WARNING, "Could not read the operator account", unreadable);
            errorSink.accept("Não foi possível ler os operadores. Verifique o armazenamento.");
        } finally {
            Arrays.fill(typed, '\0');
        }
    }
}
