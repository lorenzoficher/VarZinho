package br.edu.unipampa.varzinho.ui;

import br.edu.unipampa.varzinho.application.OperatorAccess;
import br.edu.unipampa.varzinho.application.SignedInOperator;
import br.edu.unipampa.varzinho.application.port.RepositoryException;
import br.edu.unipampa.varzinho.infrastructure.persistence.CsvOperatorStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static br.edu.unipampa.varzinho.ui.SwingWork.awaitRepositoryWork;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OperatorsPanelTest {
    @TempDir
    Path folder;
    private Path file;
    private final List<String> messages = new ArrayList<>();
    private OperatorsPanel panel;

    @BeforeEach
    void signInTheFirstOperator() throws RepositoryException {
        file = folder.resolve("operators.csv");
        SignedInOperator signedIn = new OperatorAccess(new CsvOperatorStore(file))
                .createFirst("Ana Lima", "ana", "Str0ng!pass");
        panel = new OperatorsPanel(signedIn, messages::add);
    }

    @Test
    void aRegisteredOperatorSignsInOnTheNextStart() throws RepositoryException {
        fill("Bruno Souza", "bruno", "Other1!pass", "Other1!pass");

        panel.registerButton().doClick();
        awaitRepositoryWork();

        OperatorAccess nextStart = new OperatorAccess(new CsvOperatorStore(file));
        assertEquals("Bruno Souza", nextStart.signIn("bruno", "Other1!pass").getOperator().getName());
        assertEquals(List.of("Operador @bruno cadastrado."), messages);
    }

    @Test
    void aRegisteredOperatorLeavesTheFormEmptyForTheNext() {
        fill("Bruno Souza", "bruno", "Other1!pass", "Other1!pass");

        panel.registerButton().doClick();
        awaitRepositoryWork();

        assertEquals("", panel.nameField().getText());
        assertEquals("", panel.usernameField().getText());
        assertEquals(0, panel.passwordField().getPassword().length);
        assertEquals(0, panel.confirmationField().getPassword().length);
    }

    @Test
    void aConfirmationThatDiffersIsRefusedBeforeAnythingIsStored() throws RepositoryException {
        fill("Bruno Souza", "bruno", "Other1!pass", "Other1!pasz");

        panel.registerButton().doClick();
        awaitRepositoryWork();

        assertEquals(List.of("A confirmação não confere com a senha."), messages);
        assertFalse(new CsvOperatorStore(file).findByUsername("bruno").isPresent());
    }

    @Test
    void aWeakPasswordShowsItsReasonAndStoresNothing() throws RepositoryException {
        fill("Bruno Souza", "bruno", "nodigits!here", "nodigits!here");

        panel.registerButton().doClick();
        awaitRepositoryWork();

        assertEquals(List.of("A senha precisa de uma letra maiúscula."), messages);
        assertFalse(new CsvOperatorStore(file).findByUsername("bruno").isPresent());
    }

    @Test
    void aTakenUsernameShowsItsReasonAndStoresNothing() throws RepositoryException {
        fill("Outra Ana", "ANA", "Other1!pass", "Other1!pass");

        panel.registerButton().doClick();
        awaitRepositoryWork();

        assertEquals(List.of("O usuário ana já está em uso."), messages);
        assertEquals("Ana Lima", new OperatorAccess(new CsvOperatorStore(file)).signIn("ana", "Str0ng!pass")
                .getOperator().getName());
    }

    @Test
    void aRefusalClearsBothPasswordsButKeepsTheNames() {
        fill("Bruno Souza", "bruno", "short", "short");

        panel.registerButton().doClick();
        awaitRepositoryWork();

        assertEquals("Bruno Souza", panel.nameField().getText());
        assertEquals("bruno", panel.usernameField().getText());
        assertEquals(0, panel.passwordField().getPassword().length);
        assertEquals(0, panel.confirmationField().getPassword().length);
    }

    @Test
    void registeringStoresTheOperatorOffTheSwingThreadWithTheButtonDisabled() throws Exception {
        GatedOperatorStore gated = new GatedOperatorStore();
        SignedInOperator first = new OperatorAccess(gated).createFirst("Ana Lima", "ana", "Str0ng!pass");
        OperatorsPanel waiting = new OperatorsPanel(first, messages::add);
        waiting.nameField().setText("Bruno Souza");
        waiting.usernameField().setText("bruno");
        waiting.passwordField().setText("Other1!pass");
        waiting.confirmationField().setText("Other1!pass");
        gated.gate.close();

        waiting.registerButton().doClick();
        try {
            gated.gate.awaitBlocked();
            assertFalse(gated.gate.ranOnSwing());
            assertTrue(SwingWork.swingResponds());
            SwingWork.onSwing(() -> assertFalse(waiting.registerButton().isEnabled()));
        } finally {
            gated.gate.open();
        }
        awaitRepositoryWork();

        assertEquals(List.of("Operador @bruno cadastrado."), messages);
        assertTrue(waiting.registerButton().isEnabled());
    }

    private void fill(String name, String username, String password, String confirmation) {
        panel.nameField().setText(name);
        panel.usernameField().setText(username);
        panel.passwordField().setText(password);
        panel.confirmationField().setText(confirmation);
    }
}
