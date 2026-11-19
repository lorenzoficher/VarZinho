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
 * Creates the first operator account, on a run where none is stored yet, and signs it
 * in. The password rules belong to the model; a refusal is shown in the model's words.
 */
final class FirstOperatorPanel extends JPanel {
    private final OperatorAccess access;
    private final Consumer<String> errorSink;
    private final Consumer<SignedInOperator> onSignedIn;
    private final JTextField nameField = new JTextField(16);
    private final JTextField usernameField = new JTextField(16);
    private final JPasswordField passwordField = new JPasswordField(16);
    private final JButton create = new JButton("Criar operador");

    FirstOperatorPanel(OperatorAccess access, Consumer<String> errorSink, Consumer<SignedInOperator> onSignedIn) {
        super(new GridLayout(4, 2, 6, 6));
        if (access == null) throw new IllegalArgumentException("creating an operator needs operator access");
        if (errorSink == null) throw new IllegalArgumentException("creating an operator needs an error destination");
        if (onSignedIn == null) throw new IllegalArgumentException("creating an operator needs somewhere to go next");
        this.access = access;
        this.errorSink = errorSink;
        this.onSignedIn = onSignedIn;
        add(new JLabel("Nome")); add(nameField);
        add(new JLabel("Usuário")); add(usernameField);
        add(new JLabel("Senha")); add(passwordField);
        add(new JLabel()); add(create);
        create.addActionListener(event -> create());
    }

    JTextField nameField() { return nameField; }
    JTextField usernameField() { return usernameField; }
    JPasswordField passwordField() { return passwordField; }
    JButton createButton() { return create; }

    private void create() {
        char[] typed = passwordField.getPassword();
        try {
            onSignedIn.accept(access.createFirst(nameField.getText(), usernameField.getText(), new String(typed)));
        } catch (IllegalArgumentException | IllegalStateException refused) {
            passwordField.setText("");
            errorSink.accept(PortugueseText.message(refused.getMessage()));
        } catch (RepositoryException unwritable) {
            errorSink.accept("Não foi possível salvar o operador. Verifique o armazenamento.");
        } finally {
            Arrays.fill(typed, '\0');
        }
    }
}
