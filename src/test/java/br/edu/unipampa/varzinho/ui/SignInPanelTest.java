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

import static br.edu.unipampa.varzinho.ui.SwingWork.awaitRepositoryWork;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
        awaitRepositoryWork();

        assertEquals(1, signedIn.size());
        assertEquals("ana", signedIn.get(0).getOperator().getUsername());
        assertTrue(errors.isEmpty(), errors.toString());
    }

    @Test
    void anUnknownUsernameAndAWrongPasswordShowTheSameMessage() {
        panel.usernameField().setText("nobody");
        panel.passwordField().setText(PASSWORD);
        panel.signInButton().doClick();
        awaitRepositoryWork();
        panel.usernameField().setText("ana");
        panel.passwordField().setText("Wr0ng!pass");
        panel.signInButton().doClick();
        awaitRepositoryWork();

        assertTrue(signedIn.isEmpty());
        assertEquals(2, errors.size());
        assertEquals(errors.get(0), errors.get(1));
    }

    @Test
    void aFailedSignInClearsThePasswordButKeepsTheUsername() {
        panel.usernameField().setText("ana");
        panel.passwordField().setText("Wr0ng!pass");

        panel.signInButton().doClick();
        awaitRepositoryWork();

        assertEquals("ana", panel.usernameField().getText());
        assertEquals(0, panel.passwordField().getPassword().length);
        panel.passwordField().setText(PASSWORD);
        panel.signInButton().doClick();
        awaitRepositoryWork();
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
        awaitRepositoryWork();

        assertTrue(signedIn.isEmpty());
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("ler os operadores"), errors.get(0));
    }

    @Test
    void aCompletedPasswordRecoveryFillsTheUsernameAndAsksForTheNewPassword() {
        SignInPanel recovering = new SignInPanel(new OperatorAccess(store), errors::add, signedIn::add,
                parent -> Optional.of("ana"));
        recovering.passwordField().setText("leftover");

        recovering.forgotPasswordButton().doClick();

        assertEquals("ana", recovering.usernameField().getText());
        assertEquals(0, recovering.passwordField().getPassword().length);
        assertEquals(List.of("Senha alterada. Entre com a nova senha."), errors);
        assertTrue(signedIn.isEmpty());
    }

    @Test
    void aPasswordRecoveryClosedHalfwayChangesNothing() {
        SignInPanel recovering = new SignInPanel(new OperatorAccess(store), errors::add, signedIn::add,
                parent -> Optional.empty());
        recovering.usernameField().setText("typed");

        recovering.forgotPasswordButton().doClick();

        assertEquals("typed", recovering.usernameField().getText());
        assertTrue(errors.isEmpty(), errors.toString());
    }

    @Test
    void signingInReadsTheAccountOffTheSwingThreadWithTheButtonsDisabled() throws Exception {
        GatedOperatorStore gated = new GatedOperatorStore();
        gated.save(new Operator("Ana Lima", "ana", PASSWORD));
        SignInPanel waiting = new SignInPanel(new OperatorAccess(gated), errors::add, signedIn::add);
        waiting.usernameField().setText("ana");
        waiting.passwordField().setText(PASSWORD);
        gated.gate.close();

        waiting.signInButton().doClick();
        try {
            gated.gate.awaitBlocked();
            assertFalse(gated.gate.ranOnSwing());
            assertTrue(SwingWork.swingResponds());
            SwingWork.onSwing(() -> {
                assertFalse(waiting.signInButton().isEnabled());
                assertFalse(waiting.forgotPasswordButton().isEnabled());
                assertTrue(signedIn.isEmpty());
            });
        } finally {
            gated.gate.open();
        }
        awaitRepositoryWork();

        assertEquals(1, signedIn.size());
        assertTrue(waiting.signInButton().isEnabled());
        assertTrue(waiting.forgotPasswordButton().isEnabled());
    }

    private static final class UnreadableStore implements OperatorStore {
        @Override
        public void save(Operator operator) throws RepositoryException {
            throw new RepositoryException("disk is gone");
        }

        @Override
        public void replace(Operator operator) throws RepositoryException {
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
