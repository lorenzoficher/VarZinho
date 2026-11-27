package br.edu.unipampa.varzinho.infrastructure.persistence;

import br.edu.unipampa.varzinho.application.port.OperatorStore;
import br.edu.unipampa.varzinho.application.port.RepositoryException;
import br.edu.unipampa.varzinho.domain.people.Operator;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CsvOperatorStoreTest extends OperatorStoreContractTest {
    private static final String PASSWORD = "Strong1!";

    @TempDir
    private Path directory;

    @Override
    protected OperatorStore createStore() {
        return store();
    }

    @Test
    void theFileContainsTheHashAndNeverThePassword() throws RepositoryException, IOException {
        Operator operator = new Operator("Inaurrara Flores", "inaurrara", PASSWORD);

        store().save(operator);

        String file = Files.readString(directory.resolve("operators.csv"));
        assertTrue(file.contains(operator.getPasswordHash()));
        assertFalse(file.contains(PASSWORD));
    }

    @Test
    void aNewStoreInstanceReadsTheOperatorFromDisk() throws RepositoryException {
        Operator operator = new Operator("Inaurrara Flores", "inaurrara", PASSWORD);
        store().save(operator);

        Operator reloaded = store().findByUsername("inaurrara").orElseThrow();

        assertTrue(reloaded.checksPassword(PASSWORD));
        assertEquals(operator.getPasswordHash(), reloaded.getPasswordHash());
    }

    @Test
    void aFileThatLostItsHeaderIsDamaged() throws IOException {
        Files.write(directory.resolve("operators.csv"), List.of("Inaurrara,inaurrara,hash"));

        assertThrows(RepositoryException.class, () -> store().hasAny());
    }

    @Test
    void anUnreadableOperatorLineIsDamaged() throws IOException {
        Files.write(directory.resolve("operators.csv"), List.of(CsvOperatorStore.HEADER,
                "Inaurrara Flores,inaurrara,not-a-password-hash"));

        assertThrows(RepositoryException.class, () -> store().findByUsername("inaurrara"));
    }

    @Test
    void anExistingEmptyFileIsDamagedRatherThanAnEmptyStore() throws IOException {
        Files.createFile(directory.resolve("operators.csv"));

        assertThrows(RepositoryException.class, () -> store().hasAny());
    }

    private CsvOperatorStore store() {
        return new CsvOperatorStore(directory.resolve("operators.csv"));
    }
}
