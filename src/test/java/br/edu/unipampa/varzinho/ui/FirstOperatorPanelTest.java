package br.edu.unipampa.varzinho.ui;

import br.edu.unipampa.varzinho.application.OperatorAccess;
import br.edu.unipampa.varzinho.application.SignedInOperator;
import br.edu.unipampa.varzinho.application.port.RepositoryException;
import br.edu.unipampa.varzinho.infrastructure.persistence.InMemoryOperatorStore;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

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

        assertTrue(store.findByUsername("ana").isPresent());
        assertEquals(1, signedIn.size());
        assertEquals("Ana Lima", signedIn.get(0).getOperator().getName());
        assertTrue(errors.isEmpty(), errors.toString());
    }

    @Test
    void aWeakPasswordShowsTheRuleItBrokeAndStoresNothing() throws RepositoryException {
        fill("Ana Lima", "ana", "NoDigits!here");

        panel.createButton().doClick();

        assertEquals(List.of("a senha precisa de um número"), errors);
        assertFalse(store.hasAny());
        assertTrue(signedIn.isEmpty());
    }

    @Test
    void aRefusedPasswordIsClearedSoItIsTypedAgain() {
        fill("Ana Lima", "ana", "short");

        panel.createButton().doClick();

        assertEquals("ana", panel.usernameField().getText());
        assertEquals(0, panel.passwordField().getPassword().length);
    }

    private void fill(String name, String username, String password) {
        panel.nameField().setText(name);
        panel.usernameField().setText(username);
        panel.passwordField().setText(password);
    }
}
