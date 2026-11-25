package br.edu.unipampa.varzinho.ui;

import br.edu.unipampa.varzinho.application.OperatorView;
import br.edu.unipampa.varzinho.application.SignedInOperator;
import br.edu.unipampa.varzinho.application.port.RepositoryException;
import net.miginfocom.swing.MigLayout;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The <b>Operadores</b> page: a signed-in operator registers another one, who signs in
 * on the next start. The password rules and the unique username belong to the model;
 * only the typed confirmation is checked here, since the model receives one password.
 */
final class OperatorsPanel extends JPanel {
    private static final Logger LOGGER = Logger.getLogger(OperatorsPanel.class.getName());
    private final SignedInOperator signedIn;
    private final Consumer<String> messages;
    private final JTextField nameField = new JTextField(20);
    private final JTextField usernameField = new JTextField(20);
    private final JPasswordField passwordField = new JPasswordField(20);
    private final JPasswordField confirmationField = new JPasswordField(20);
    private final JButton register = new JButton("Cadastrar operador");

    OperatorsPanel(SignedInOperator signedIn, Consumer<String> messages) {
        super(new MigLayout("insets 24, wrap 2", "[right][240:420,fill]", "[]16[][][][][]"));
        if (signedIn == null) throw new IllegalArgumentException("registering an operator needs a signed-in operator");
        if (messages == null) throw new IllegalArgumentException("registering an operator needs a message destination");
        this.signedIn = signedIn;
        this.messages = messages;
        VarZinhoTheme.styleCard(this);
        JLabel title = new JLabel("Operadores");
        VarZinhoTheme.styleTitle(title);
        add(title, "span 2, left");
        add(new JLabel("Nome")); add(nameField);
        add(new JLabel("Usuário")); add(usernameField);
        add(new JLabel("Senha")); add(passwordField);
        add(new JLabel("Confirmar senha")); add(confirmationField);
        add(register, "skip, growx 0, left");
        register.addActionListener(event -> register());
        EnterKey.submits(register, nameField, usernameField, passwordField, confirmationField);
    }

    JTextField nameField() { return nameField; }
    JTextField usernameField() { return usernameField; }
    JPasswordField passwordField() { return passwordField; }
    JPasswordField confirmationField() { return confirmationField; }
    JButton registerButton() { return register; }

    private void register() {
        char[] password = passwordField.getPassword();
        char[] confirmation = confirmationField.getPassword();
        boolean confirmed = Arrays.equals(password, confirmation);
        String chosen = new String(password);
        Arrays.fill(password, '\0');
        Arrays.fill(confirmation, '\0');
        if (!confirmed) {
            messages.accept("A confirmação não confere com a senha.");
            clearPasswords();
            return;
        }
        String name = nameField.getText();
        String username = usernameField.getText();
        // Hashing the password and writing the account both wait, so they run on a worker.
        RepositoryTask.run(List.of(register), "Cadastrando o operador…",
                () -> signedIn.register(name, username, chosen), this::registered, this::refused);
    }

    private void registered(OperatorView registered) {
        nameField.setText("");
        usernameField.setText("");
        clearPasswords();
        messages.accept("Operador @" + registered.getUsername() + " cadastrado.");
    }

    private void refused(Throwable failure) {
        clearPasswords();
        if (failure instanceof IllegalArgumentException) {
            messages.accept(PortugueseText.message(failure.getMessage()));
            return;
        }
        LOGGER.log(failure instanceof RepositoryException ? Level.WARNING : Level.SEVERE,
                "Could not register an operator", failure);
        messages.accept("Não foi possível salvar o operador. Verifique o armazenamento.");
    }

    private void clearPasswords() {
        passwordField.setText("");
        confirmationField.setText("");
    }
}
