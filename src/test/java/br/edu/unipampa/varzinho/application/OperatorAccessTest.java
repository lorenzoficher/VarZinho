package br.edu.unipampa.varzinho.application;

import br.edu.unipampa.varzinho.application.port.RepositoryException;
import br.edu.unipampa.varzinho.infrastructure.persistence.InMemoryOperatorStore;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OperatorAccessTest {
    private static final String PASSWORD = "Strong1!";
    private final InMemoryOperatorStore store = new InMemoryOperatorStore();
    private final OperatorAccess access = new OperatorAccess(store);

    @Test
    void theFirstOperatorCanBeCreatedOnlyWhileTheStoreIsEmpty() throws RepositoryException {
        assertTrue(access.needsFirstOperator());

        SignedInOperator signedIn = access.createFirst("Inaurrara Flores", "inaurrara", PASSWORD);

        assertEquals("Inaurrara Flores", signedIn.getOperator().getName());
        assertEquals("inaurrara", signedIn.getOperator().getUsername());
        assertFalse(access.needsFirstOperator());
        assertThrows(IllegalStateException.class,
                () -> access.createFirst("Another Operator", "another", "Another1!"));
        assertTrue(store.findByUsername("another").isEmpty());
    }

    @Test
    void theRightCredentialsSignAnOperatorIn() throws RepositoryException {
        access.createFirst("Inaurrara Flores", "inaurrara", PASSWORD);

        OperatorView operator = access.signIn("INAURRARA", PASSWORD).getOperator();

        assertEquals("Inaurrara Flores", operator.getName());
        assertEquals("inaurrara", operator.getUsername());
    }

    @Test
    void aWrongUsernameAndPasswordReceiveTheSameRefusal() throws RepositoryException {
        access.createFirst("Inaurrara Flores", "inaurrara", PASSWORD);

        IllegalArgumentException wrongUsername = assertThrows(IllegalArgumentException.class,
                () -> access.signIn("unknown", PASSWORD));
        IllegalArgumentException wrongPassword = assertThrows(IllegalArgumentException.class,
                () -> access.signIn("inaurrara", "Wrong1!x"));

        assertEquals(wrongUsername.getMessage(), wrongPassword.getMessage());
    }

    @Test
    void aSignedInOperatorRegistersAnotherOperator() throws RepositoryException {
        SignedInOperator signedIn = access.createFirst("Inaurrara Flores", "inaurrara", PASSWORD);

        OperatorView registered = signedIn.register("Lara Rios", "lara", "Another1!");

        assertEquals("Lara Rios", registered.getName());
        assertEquals("lara", registered.getUsername());
        assertTrue(store.findByUsername("lara").orElseThrow().checksPassword("Another1!"));
    }

    @Test
    void aWeakPasswordIsReportedAndNothingIsStored() throws RepositoryException {
        SignedInOperator signedIn = access.createFirst("Inaurrara Flores", "inaurrara", PASSWORD);

        IllegalArgumentException refusal = assertThrows(IllegalArgumentException.class,
                () -> signedIn.register("Lara Rios", "lara", "weak"));

        assertTrue(refusal.getMessage().contains("8 characters"));
        assertTrue(store.findByUsername("lara").isEmpty());
    }

    @Test
    void aTakenUsernameIsReportedAndTheOriginalAccountStays() throws RepositoryException {
        SignedInOperator signedIn = access.createFirst("Inaurrara Flores", "inaurrara", PASSWORD);

        IllegalArgumentException refusal = assertThrows(IllegalArgumentException.class,
                () -> signedIn.register("Someone Else", "INAURRARA", "Another1!"));

        assertTrue(refusal.getMessage().contains("already taken"));
        assertEquals("Inaurrara Flores", store.findByUsername("inaurrara").orElseThrow().getName());
    }
}
