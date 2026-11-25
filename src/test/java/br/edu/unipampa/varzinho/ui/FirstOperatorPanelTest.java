package br.edu.unipampa.varzinho.ui;

import br.edu.unipampa.varzinho.application.OperatorAccess;
import br.edu.unipampa.varzinho.application.SignedInOperator;
import br.edu.unipampa.varzinho.application.port.RepositoryException;
import br.edu.unipampa.varzinho.infrastructure.persistence.InMemoryOperatorStore;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static br.edu.unipampa.varzinho.ui.SwingWork.awaitRepositoryWork;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FirstOperatorPanelTest {
    private final InMemoryOperatorStore store = new InMemoryOperatorStore();
    private final List<String> errors = new ArrayList<>();
    private final List<SignedInOperator> signedIn = new ArrayList<>();
    private final FirstOperatorPanel panel =
            new FirstOperatorPanel(new OperatorAccess(store), errors::add, signedIn::add);

    @Test
    void creatingTheFirstOperatorStoresItAndSignsItIn() throws RepositoryException {
        fill("Ana Lima", "ana", "Str0ng!pass");

        panel.createButton().doClick();
        awaitRepositoryWork();

        assertTrue(store.findByUsername("ana").isPresent());
        assertEquals(1, signedIn.size());
        assertEquals("Ana Lima", signedIn.get(0).getOperator().getName());
        assertTrue(errors.isEmpty(), errors.toString());
    }

    @Test
    void aWeakPasswordShowsTheRuleItBrokeAndStoresNothing() throws RepositoryException {
        fill("Ana Lima", "ana", "NoDigits!here");

        panel.createButton().doClick();
        awaitRepositoryWork();

        assertEquals(List.of("A senha precisa de um número."), errors);
        assertFalse(store.hasAny());
        assertTrue(signedIn.isEmpty());
    }

    @Test
    void aRefusedPasswordIsClearedSoItIsTypedAgain() {
        fill("Ana Lima", "ana", "short");

        panel.createButton().doClick();
        awaitRepositoryWork();

        assertEquals("ana", panel.usernameField().getText());
        assertEquals(0, panel.passwordField().getPassword().length);
    }

    @Test
    void creatingTheFirstOperatorStoresItOffTheSwingThreadWithTheButtonDisabled() throws Exception {
        GatedOperatorStore gated = new GatedOperatorStore();
        FirstOperatorPanel waiting = new FirstOperatorPanel(new OperatorAccess(gated), errors::add, signedIn::add);
        waiting.nameField().setText("Ana Lima");
        waiting.usernameField().setText("ana");
        waiting.passwordField().setText("Str0ng!pass");
        gated.gate.close();

        waiting.createButton().doClick();
        try {
            gated.gate.awaitBlocked();
            assertFalse(gated.gate.ranOnSwing());
            assertTrue(SwingWork.swingResponds());
            SwingWork.onSwing(() -> assertFalse(waiting.createButton().isEnabled()));
        } finally {
            gated.gate.open();
        }
        awaitRepositoryWork();

        assertEquals(1, signedIn.size());
        assertTrue(gated.findByUsername("ana").isPresent());
        assertTrue(waiting.createButton().isEnabled());
        assertTrue(errors.isEmpty(), errors.toString());
    }

    private void fill(String name, String username, String password) {
        panel.nameField().setText(name);
        panel.usernameField().setText(username);
        panel.passwordField().setText(password);
    }
}
