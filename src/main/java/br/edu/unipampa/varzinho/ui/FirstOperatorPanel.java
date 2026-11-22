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
 * Creates the first operator account, on a run where none is stored yet, and signs it
 * in. The password rules belong to the model; a refusal is shown in the model's words.
 */
final class FirstOperatorPanel extends JPanel {
    private static final Logger LOGGER = Logger.getLogger(FirstOperatorPanel.class.getName());
    private final OperatorAccess access;
    private final Consumer<String> errorSink;
    private final Consumer<SignedInOperator> onSignedIn;
    private final JTextField nameField = new JTextField(16);
    private final JTextField usernameField = new JTextField(16);
    private final JPasswordField passwordField = new JPasswordField(16);
    private final JButton create = new JButton("Criar operador");

    FirstOperatorPanel(OperatorAccess access, Consumer<String> errorSink, Consumer<SignedInOperator> onSignedIn) {
        super(new MigLayout("insets 28, fillx, wrap 1", "[360,grow,fill]", "[][]18[][]12[][]12[][]24[]"));
        if (access == null) throw new IllegalArgumentException("creating an operator needs operator access");
        if (errorSink == null) throw new IllegalArgumentException("creating an operator needs an error destination");
        if (onSignedIn == null) throw new IllegalArgumentException("creating an operator needs somewhere to go next");
        this.access = access;
        this.errorSink = errorSink;
        this.onSignedIn = onSignedIn;
        JLabel brand = new JLabel("VARZINHO ARENA");
        VarZinhoTheme.styleEyebrow(brand);
        JLabel title = new JLabel("Crie o primeiro operador");
        VarZinhoTheme.styleTitle(title);
        add(brand);
        add(title);
        add(new JLabel("Nome")); add(nameField, "h 38!");
        add(new JLabel("Usuário")); add(usernameField, "h 38!");
        add(new JLabel("Senha")); add(passwordField, "h 38!");
        VarZinhoTheme.styleProminentAction(create);
        add(create, "h 42!");
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
            LOGGER.log(Level.WARNING, "Could not create the first operator account", unwritable);
            errorSink.accept("Não foi possível salvar o operador. Verifique o armazenamento.");
        } finally {
            Arrays.fill(typed, '\0');
        }
    }
}
