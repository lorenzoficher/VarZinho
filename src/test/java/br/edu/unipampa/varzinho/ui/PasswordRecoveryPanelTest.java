package br.edu.unipampa.varzinho.ui;

import br.edu.unipampa.varzinho.application.OperatorAccess;
import br.edu.unipampa.varzinho.application.port.RepositoryException;
import br.edu.unipampa.varzinho.domain.people.Operator;
import br.edu.unipampa.varzinho.infrastructure.persistence.InMemoryOperatorStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    private void requestLink(String username, String email) {
        panel.usernameField().setText(username);
        panel.emailField().setText(email);
        panel.sendLinkButton().doClick();
    }

    private void choosePassword(String password, String confirmation) {
        panel.passwordField().setText(password);
        panel.confirmationField().setText(confirmation);
        panel.saveButton().doClick();
    }
}
