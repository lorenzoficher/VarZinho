package br.edu.unipampa.varzinho.ui;

import br.edu.unipampa.varzinho.application.OperatorAccess;
import br.edu.unipampa.varzinho.application.port.RepositoryException;
import br.edu.unipampa.varzinho.domain.people.Operator;
import br.edu.unipampa.varzinho.infrastructure.persistence.InMemoryOperatorStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.GraphicsEnvironment;

import java.util.ArrayList;
import java.util.List;

import static br.edu.unipampa.varzinho.ui.SwingWork.awaitRepositoryWork;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

class PasswordRecoveryPanelTest {
    private static final String OLD_PASSWORD = "Str0ng!pass";
    private static final String NEW_PASSWORD = "Renewed2@";

    private final InMemoryOperatorStore store = new InMemoryOperatorStore();
    private final OperatorAccess access = new OperatorAccess(store);
    private final List<String> messages = new ArrayList<>();
    private final List<String> resetUsernames = new ArrayList<>();
    private final PasswordRecoveryPanel panel = new PasswordRecoveryPanel(access, messages::add, resetUsernames::add);

    @BeforeEach
    void storeAnOperator() throws RepositoryException {
        store.save(new Operator("Ana Lima", "ana", OLD_PASSWORD));
    }

    @Test
    void startsByAskingForTheUsernameAndTheEmail() {
        assertEquals(PasswordRecoveryPanel.REQUEST_STEP, panel.step());
    }

    @Test
    void aRequestSaysTheLinkWentToTheTypedEmailWithoutSendingAnything() {
        requestLink("ana", "qualquer@exemplo.com");

        assertEquals(PasswordRecoveryPanel.SENT_STEP, panel.step());
        assertTrue(panel.sentNotice().getText().contains("qualquer@exemplo.com"), panel.sentNotice().getText());
        assertTrue(messages.isEmpty(), messages.toString());
    }

    @Test
    void proceedingFromTheSentLinkOpensTheNewPasswordStep() {
        requestLink("ana", "qualquer@exemplo.com");

        panel.proceedButton().doClick();

        assertEquals(PasswordRecoveryPanel.NEW_PASSWORD_STEP, panel.step());
    }

    @Test
    void theWholeFlowReplacesThePasswordAndHandsBackTheUsername() {
        requestLink("ANA", "qualquer@exemplo.com");
        panel.proceedButton().doClick();

        choosePassword(NEW_PASSWORD, NEW_PASSWORD);

        assertEquals(List.of("ana"), resetUsernames);
        assertDoesNotThrow(() -> access.signIn("ana", NEW_PASSWORD));
        assertThrows(IllegalArgumentException.class, () -> access.signIn("ana", OLD_PASSWORD));
    }

    @Test
    void anUnknownUsernameStaysOnTheRequestStepAndSaysSo() {
        requestLink("ninguem", "qualquer@exemplo.com");

        assertEquals(PasswordRecoveryPanel.REQUEST_STEP, panel.step());
        assertEquals(List.of("Nenhum operador usa o usuário ninguem."), messages);
    }

    @Test
    void aMalformedEmailStaysOnTheRequestStepAndSaysSo() {
        requestLink("ana", "sem-arroba");

        assertEquals(PasswordRecoveryPanel.REQUEST_STEP, panel.step());
        assertEquals(List.of("Informe um e-mail válido."), messages);
    }

    @Test
    void aMismatchedConfirmationKeepsTheOldPassword() {
        requestLink("ana", "qualquer@exemplo.com");
        panel.proceedButton().doClick();

        choosePassword(NEW_PASSWORD, "Different3#");

        assertEquals(PasswordRecoveryPanel.NEW_PASSWORD_STEP, panel.step());
        assertEquals(List.of("A confirmação não confere com a senha."), messages);
        assertTrue(resetUsernames.isEmpty());
        assertDoesNotThrow(() -> access.signIn("ana", OLD_PASSWORD));
    }

    @Test
    void aWeakPasswordIsRefusedInPortugueseAndClearsBothFields() {
        requestLink("ana", "qualquer@exemplo.com");
        panel.proceedButton().doClick();

        choosePassword("fraca", "fraca");

        assertEquals(List.of("A senha precisa ter pelo menos 8 caracteres."), messages);
        assertEquals(0, panel.passwordField().getPassword().length);
        assertEquals(0, panel.confirmationField().getPassword().length);
        assertTrue(resetUsernames.isEmpty());
    }

    @Test
    void eachStepTakesTheFocusSoTheKeyboardIsNotLeftWithNowhereToGo() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "focus needs a window on screen");
        JFrame frame = new JFrame();
        try {
            SwingUtilities.invokeAndWait(() -> {
                frame.add(panel);
                frame.pack();
                frame.setVisible(true);
                panel.usernameField().requestFocusInWindow();
            });

            SwingUtilities.invokeAndWait(() -> {
                panel.usernameField().setText("ana");
                panel.emailField().setText("qualquer@exemplo.com");
                panel.sendLinkButton().doClick();
            });
            awaitRepositoryWork();
            assertFocusMovesTo(frame, panel.proceedButton());

            SwingUtilities.invokeAndWait(() -> panel.proceedButton().doClick());
            assertFocusMovesTo(frame, panel.passwordField());
        } finally {
            SwingUtilities.invokeAndWait(frame::dispose);
        }
    }

    @Test
    void requestingALinkReadsTheOperatorsOffTheSwingThreadWithTheButtonDisabled() throws Exception {
        GatedOperatorStore gated = gatedStoreWithAna();
        PasswordRecoveryPanel waiting = new PasswordRecoveryPanel(new OperatorAccess(gated), messages::add,
                resetUsernames::add);
        waiting.usernameField().setText("ana");
        waiting.emailField().setText("qualquer@exemplo.com");
        gated.gate.close();

        waiting.sendLinkButton().doClick();
        try {
            gated.gate.awaitBlocked();
            assertFalse(gated.gate.ranOnSwing());
            assertTrue(SwingWork.swingResponds());
            SwingWork.onSwing(() -> {
                assertFalse(waiting.sendLinkButton().isEnabled());
                assertEquals(PasswordRecoveryPanel.REQUEST_STEP, waiting.step());
            });
        } finally {
            gated.gate.open();
        }
        awaitRepositoryWork();

        assertEquals(PasswordRecoveryPanel.SENT_STEP, waiting.step());
        assertTrue(waiting.sendLinkButton().isEnabled());
    }

    @Test
    void savingANewPasswordWritesOffTheSwingThreadWithTheButtonDisabled() throws Exception {
        GatedOperatorStore gated = gatedStoreWithAna();
        PasswordRecoveryPanel waiting = new PasswordRecoveryPanel(new OperatorAccess(gated), messages::add,
                resetUsernames::add);
        waiting.usernameField().setText("ana");
        waiting.emailField().setText("qualquer@exemplo.com");
        waiting.sendLinkButton().doClick();
        awaitRepositoryWork();
        waiting.proceedButton().doClick();
        waiting.passwordField().setText(NEW_PASSWORD);
        waiting.confirmationField().setText(NEW_PASSWORD);
        gated.gate.close();

        waiting.saveButton().doClick();
        try {
            gated.gate.awaitBlocked();
            assertFalse(gated.gate.ranOnSwing());
            assertTrue(SwingWork.swingResponds());
            SwingWork.onSwing(() -> {
                assertFalse(waiting.saveButton().isEnabled());
                assertEquals(0, waiting.passwordField().getPassword().length);
            });
        } finally {
            gated.gate.open();
        }
        awaitRepositoryWork();

        assertEquals(List.of("ana"), resetUsernames);
        assertDoesNotThrow(() -> new OperatorAccess(gated).signIn("ana", NEW_PASSWORD));
        assertTrue(waiting.saveButton().isEnabled());
    }

    private static GatedOperatorStore gatedStoreWithAna() throws RepositoryException {
        GatedOperatorStore gated = new GatedOperatorStore();
        gated.save(new Operator("Ana Lima", "ana", OLD_PASSWORD));
        return gated;
    }

    private static void assertFocusMovesTo(JFrame frame, Component expected) throws Exception {
        long deadline = System.currentTimeMillis() + 3000;
        while (frame.getMostRecentFocusOwner() != expected && System.currentTimeMillis() < deadline) {
            Thread.sleep(50);
        }
        assertSame(expected, frame.getMostRecentFocusOwner());
    }

    private void requestLink(String username, String email) {
        panel.usernameField().setText(username);
        panel.emailField().setText(email);
        panel.sendLinkButton().doClick();
        awaitRepositoryWork();
    }

    private void choosePassword(String password, String confirmation) {
        panel.passwordField().setText(password);
        panel.confirmationField().setText(confirmation);
        panel.saveButton().doClick();
        awaitRepositoryWork();
    }
}
