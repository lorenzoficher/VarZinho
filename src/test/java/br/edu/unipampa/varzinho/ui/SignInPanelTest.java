package br.edu.unipampa.varzinho.ui;

import br.edu.unipampa.varzinho.application.OperatorAccess;
import br.edu.unipampa.varzinho.application.SignedInOperator;
import br.edu.unipampa.varzinho.application.port.OperatorStore;
import br.edu.unipampa.varzinho.application.port.RepositoryException;
import br.edu.unipampa.varzinho.domain.people.Operator;
import br.edu.unipampa.varzinho.infrastructure.persistence.InMemoryOperatorStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SignInPanelTest {
    private static final String PASSWORD = "Str0ng!pass";

    private final InMemoryOperatorStore store = new InMemoryOperatorStore();
    private final List<String> errors = new ArrayList<>();
    private final List<SignedInOperator> signedIn = new ArrayList<>();
    private final SignInPanel panel = new SignInPanel(new OperatorAccess(store), errors::add, signedIn::add);

    @BeforeEach
    void storeAnOperator() throws RepositoryException {
        store.save(new Operator("Ana Lima", "ana", PASSWORD));
    }

    @Test
    void aCorrectSignInHandsOverTheSignedInOperatorOnce() {
        panel.usernameField().setText("ana");
        panel.passwordField().setText(PASSWORD);

        panel.signInButton().doClick();

        assertEquals(1, signedIn.size());
        assertEquals("ana", signedIn.get(0).getOperator().getUsername());
        assertTrue(errors.isEmpty(), errors.toString());
    }

    @Test
    void anUnknownUsernameAndAWrongPasswordShowTheSameMessage() {
        panel.usernameField().setText("nobody");
        panel.passwordField().setText(PASSWORD);
        panel.signInButton().doClick();
        panel.usernameField().setText("ana");
        panel.passwordField().setText("Wr0ng!pass");
        panel.signInButton().doClick();

        assertTrue(signedIn.isEmpty());
        assertEquals(2, errors.size());
        assertEquals(errors.get(0), errors.get(1));
    }

    @Test
    void aFailedSignInClearsThePasswordButKeepsTheUsername() {
        panel.usernameField().setText("ana");
        panel.passwordField().setText("Wr0ng!pass");

        panel.signInButton().doClick();

        assertEquals("ana", panel.usernameField().getText());
        assertEquals(0, panel.passwordField().getPassword().length);
        panel.passwordField().setText(PASSWORD);
        panel.signInButton().doClick();
        assertEquals(1, signedIn.size());
        assertEquals("ana", signedIn.get(0).getOperator().getUsername());
    }

    @Test
    void anUnreadableStoreIsReportedWithoutSigningIn() {
        SignInPanel unreadable = new SignInPanel(new OperatorAccess(new UnreadableStore()), errors::add,
                signedIn::add);
        unreadable.usernameField().setText("ana");
        unreadable.passwordField().setText(PASSWORD);

        unreadable.signInButton().doClick();

        assertTrue(signedIn.isEmpty());
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("ler os operadores"), errors.get(0));
    }

    private static final class UnreadableStore implements OperatorStore {
        @Override
        public void save(Operator operator) throws RepositoryException {
            throw new RepositoryException("disk is gone");
        }

        @Override
        public Optional<Operator> findByUsername(String username) throws RepositoryException {
            throw new RepositoryException("disk is gone");
        }

        @Override
        public boolean hasAny() throws RepositoryException {
            throw new RepositoryException("disk is gone");
        }
    }
}
