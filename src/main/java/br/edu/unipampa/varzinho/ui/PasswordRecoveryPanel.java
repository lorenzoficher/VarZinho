package br.edu.unipampa.varzinho.ui;

import br.edu.unipampa.varzinho.application.OperatorAccess;
import br.edu.unipampa.varzinho.application.port.RepositoryException;
import net.miginfocom.swing.MigLayout;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.awt.CardLayout;
import java.util.Arrays;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The simulated "forgot my password" flow, in three steps: ask for the username and an
 * e-mail, say a recovery link was sent, then take the new password.
 *
 * <p>No e-mail leaves the machine. VarZinho has no mail server, so <b>Prosseguir</b>
 * stands in for the link the operator would have clicked. Whether the username exists,
 * the e-mail is well formed and the new password is strong enough is
 * {@link OperatorAccess}'s decision; only the typed confirmation is checked here.
 */
final class PasswordRecoveryPanel extends JPanel {
    static final String REQUEST_STEP = "request";
    static final String SENT_STEP = "sent";
    static final String NEW_PASSWORD_STEP = "new-password";

    private static final Logger LOGGER = Logger.getLogger(PasswordRecoveryPanel.class.getName());
    private static final String UNREADABLE = "Não foi possível ler os operadores. Verifique o armazenamento.";
    private static final String UNWRITABLE = "Não foi possível salvar a nova senha. Verifique o armazenamento.";

    private final OperatorAccess access;
    private final Consumer<String> messages;
    private final Consumer<String> onReset;
    private final CardLayout steps = new CardLayout();
    private final JTextField usernameField = new JTextField(20);
    private final JTextField emailField = new JTextField(20);
    private final JButton sendLink = new JButton("Enviar link de recuperação");
    private final JLabel sentNotice = new JLabel();
    private final JButton proceed = new JButton("Prosseguir");
    private final JPasswordField passwordField = new JPasswordField(20);
    private final JPasswordField confirmationField = new JPasswordField(20);
    private final JButton save = new JButton("Salvar nova senha");
    private String step = REQUEST_STEP;
    private String username;

    /**
     * @param messages where a refusal is shown to the operator
     * @param onReset told the username once its new password is saved
     */
    PasswordRecoveryPanel(OperatorAccess access, Consumer<String> messages, Consumer<String> onReset) {
        if (access == null) throw new IllegalArgumentException("recovering a password needs operator access");
        if (messages == null) throw new IllegalArgumentException("recovering a password needs a message destination");
        if (onReset == null) throw new IllegalArgumentException("recovering a password needs somewhere to go next");
        this.access = access;
        this.messages = messages;
        this.onReset = onReset;
        setLayout(steps);
        add(requestStep(), REQUEST_STEP);
        add(sentStep(), SENT_STEP);
        add(newPasswordStep(), NEW_PASSWORD_STEP);
        sendLink.addActionListener(event -> requestLink());
        proceed.addActionListener(event -> show(NEW_PASSWORD_STEP));
        save.addActionListener(event -> saveNewPassword());
    }

    String step() { return step; }
    JTextField usernameField() { return usernameField; }
    JTextField emailField() { return emailField; }
    JButton sendLinkButton() { return sendLink; }
    JLabel sentNotice() { return sentNotice; }
    JButton proceedButton() { return proceed; }
    JPasswordField passwordField() { return passwordField; }
    JPasswordField confirmationField() { return confirmationField; }
    JButton saveButton() { return save; }

    /** The button Enter presses on the step now showing. */
    JButton currentAction() {
        switch (step) {
            case SENT_STEP: return proceed;
            case NEW_PASSWORD_STEP: return save;
            default: return sendLink;
        }
    }

    private JPanel requestStep() {
        JPanel panel = stepPanel("Esqueceu a senha?",
                "Informe seu usuário e um e-mail para receber o link de recuperação.");
        panel.add(new JLabel("Usuário"));
        panel.add(usernameField, "h 38!");
        panel.add(new JLabel("E-mail"));
        panel.add(emailField, "h 38!");
        VarZinhoTheme.styleProminentAction(sendLink);
        panel.add(sendLink, "h 42!, gaptop 12");
        return panel;
    }

    private JPanel sentStep() {
        JPanel panel = stepPanel("Verifique seu e-mail", " ");
        panel.add(sentNotice);
        panel.add(new JLabel("Nesta demonstração nenhum e-mail é enviado: prossiga para criar a nova senha."));
        VarZinhoTheme.styleProminentAction(proceed);
        panel.add(proceed, "h 42!, gaptop 12");
        return panel;
    }

    private JPanel newPasswordStep() {
        JPanel panel = stepPanel("Crie uma nova senha",
                "Use ao menos 8 caracteres, com maiúscula, minúscula, número e símbolo.");
        panel.add(new JLabel("Nova senha"));
        panel.add(passwordField, "h 38!");
        panel.add(new JLabel("Confirmar nova senha"));
        panel.add(confirmationField, "h 38!");
        VarZinhoTheme.styleProminentAction(save);
        panel.add(save, "h 42!, gaptop 12");
        return panel;
    }

    private static JPanel stepPanel(String title, String help) {
        JPanel panel = new JPanel(new MigLayout("insets 28, fillx, wrap 1", "[360,grow,fill]"));
        JLabel heading = new JLabel(title);
        VarZinhoTheme.styleTitle(heading);
        panel.add(heading);
        panel.add(new JLabel(help), "gapbottom 12");
        return panel;
    }

    private void requestLink() {
        String typedUsername = usernameField.getText().strip();
        String email = emailField.getText().strip();
        try {
            access.requestPasswordReset(typedUsername, email);
        } catch (IllegalArgumentException refused) {
            messages.accept(PortugueseText.message(refused.getMessage()));
            return;
        } catch (RepositoryException unreadable) {
            LOGGER.log(Level.WARNING, "Could not read the operators for a password reset", unreadable);
            messages.accept(UNREADABLE);
            return;
        }
        username = typedUsername.toLowerCase(Locale.ROOT);
        sentNotice.setText("Enviamos um link de recuperação para " + email + ".");
        show(SENT_STEP);
    }

    private void saveNewPassword() {
        char[] password = passwordField.getPassword();
        char[] confirmation = confirmationField.getPassword();
        try {
            if (!Arrays.equals(password, confirmation)) {
                messages.accept("A confirmação não confere com a senha.");
                return;
            }
            access.resetPassword(username, new String(password));
            onReset.accept(username);
        } catch (IllegalArgumentException refused) {
            messages.accept(PortugueseText.message(refused.getMessage()));
        } catch (RepositoryException unwritable) {
            LOGGER.log(Level.WARNING, "Could not save a new operator password", unwritable);
            messages.accept(UNWRITABLE);
        } finally {
            Arrays.fill(password, '\0');
            Arrays.fill(confirmation, '\0');
            passwordField.setText("");
            confirmationField.setText("");
        }
    }

    private void show(String next) {
        step = next;
        steps.show(this, next);
        // The step just hidden took the focused control with it; without this the keyboard
        // is left with no component to type into or Tab from.
        firstControl().requestFocusInWindow();
    }

    private JComponent firstControl() {
        switch (step) {
            case SENT_STEP: return proceed;
            case NEW_PASSWORD_STEP: return passwordField;
            default: return usernameField;
        }
    }
}
