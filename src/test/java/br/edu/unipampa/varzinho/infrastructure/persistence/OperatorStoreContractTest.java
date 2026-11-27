package br.edu.unipampa.varzinho.infrastructure.persistence;

import br.edu.unipampa.varzinho.application.port.OperatorStore;
import br.edu.unipampa.varzinho.application.port.RepositoryException;
import br.edu.unipampa.varzinho.domain.people.Operator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The behaviour every operator store offers, independently of where it keeps data. */
abstract class OperatorStoreContractTest {
    private static final String PASSWORD = "Strong1!";

    protected abstract OperatorStore createStore();

    @Test
    void anEmptyStoreSaysItHasNoOperator() throws RepositoryException {
        assertFalse(createStore().hasAny());
    }

    @Test
    void savingAnOperatorMakesTheStoreNonEmpty() throws RepositoryException {
        OperatorStore store = createStore();

        store.save(operator("inaurrara"));

        assertTrue(store.hasAny());
    }

    @Test
    void findsAnOperatorByUsernameRegardlessOfCase() throws RepositoryException {
        OperatorStore store = createStore();
        Operator expected = operator("inaurrara");
        store.save(expected);

        Operator found = store.findByUsername("INAURRARA").orElseThrow();

        assertEquals("Inaurrara Flores", found.getName());
        assertEquals("inaurrara", found.getUsername());
        assertEquals(expected.getPasswordHash(), found.getPasswordHash());
        assertTrue(found.checksPassword(PASSWORD));
    }

    @Test
    void anUnknownOrMissingUsernameIsEmpty() throws RepositoryException {
        OperatorStore store = createStore();
        store.save(operator("inaurrara"));

        assertTrue(store.findByUsername("unknown").isEmpty());
        assertTrue(store.findByUsername(null).isEmpty());
    }

    @Test
    void aDuplicateUsernameInAnotherCaseIsRefusedWithoutReplacingTheFirst() throws RepositoryException {
        OperatorStore store = createStore();
        Operator original = operator("inaurrara");
        store.save(original);

        IllegalArgumentException refusal = assertThrows(IllegalArgumentException.class,
                () -> store.save(new Operator("Another Name", "INAURRARA", "Another1!")));

        assertTrue(refusal.getMessage().contains("already taken"));
        Operator kept = store.findByUsername("inaurrara").orElseThrow();
        assertEquals(original.getName(), kept.getName());
        assertEquals(original.getPasswordHash(), kept.getPasswordHash());
    }

    @Test
    void aMissingOperatorIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> createStore().save(null));
    }

    @Test
    void anUnstorableTextIsRefusedBeforeAnythingChanges() throws RepositoryException {
        OperatorStore store = createStore();

        assertThrows(IllegalArgumentException.class,
                () -> store.save(new Operator("Flores, Inaurrara", "inaurrara", PASSWORD)));

        assertFalse(store.hasAny());
    }

    @Test
    void replacingAnOperatorKeepsOnlyItsNewPassword() throws RepositoryException {
        OperatorStore store = createStore();
        store.save(operator("inaurrara"));

        store.replace(new Operator("Inaurrara Flores", "inaurrara", "Renewed2@"));

        Operator found = store.findByUsername("inaurrara").orElseThrow();
        assertTrue(found.checksPassword("Renewed2@"));
        assertFalse(found.checksPassword(PASSWORD));
    }

    @Test
    void replacingAnUnknownOperatorIsRefusedAndStoresNothing() throws RepositoryException {
        OperatorStore store = createStore();
        store.save(operator("inaurrara"));

        IllegalArgumentException refusal = assertThrows(IllegalArgumentException.class,
                () -> store.replace(operator("nobody")));

        assertTrue(refusal.getMessage().contains("no operator has the username nobody"), refusal.getMessage());
        assertTrue(store.findByUsername("nobody").isEmpty());
    }

    @Test
    void replacingWithAMissingOperatorIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> createStore().replace(null));
    }

    private static Operator operator(String username) {
        return new Operator("Inaurrara Flores", username, PASSWORD);
    }
}
