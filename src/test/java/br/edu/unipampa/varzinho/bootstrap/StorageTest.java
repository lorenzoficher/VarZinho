package br.edu.unipampa.varzinho.bootstrap;

import br.edu.unipampa.varzinho.application.port.RepositoryException;
import br.edu.unipampa.varzinho.domain.people.Operator;
import br.edu.unipampa.varzinho.infrastructure.persistence.CsvGymRepository;
import br.edu.unipampa.varzinho.infrastructure.persistence.CsvHighlightRepository;
import br.edu.unipampa.varzinho.infrastructure.persistence.CsvOperatorStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Needs no Docker: the database cases either stop before connecting or only ever reach a
 * port nobody answers on.
 */
class StorageTest {
    private static final String UNREACHABLE_URL = "jdbc:postgresql://127.0.0.1:1/varzinho?connectTimeout=2";

    @TempDir
    Path dataFolder;

    @Test
    void storesInCsvWhenTheCsvModeIsAskedFor() throws RepositoryException {
        Storage storage = Storage.fromEnvironment(Map.of("VARZINHO_STORAGE", "csv")::get, dataFolder);

        assertInstanceOf(CsvHighlightRepository.class, storage.highlights());
        assertInstanceOf(CsvGymRepository.class, storage.gym());
    }

    @Test
    void readsTheStorageModeWithoutMindingCase() throws RepositoryException {
        Storage storage = Storage.fromEnvironment(Map.of("VARZINHO_STORAGE", " CSV ")::get, dataFolder);

        assertInstanceOf(CsvHighlightRepository.class, storage.highlights());
    }

    @Test
    void keepsOperatorsInTheDataFolderInTheCsvMode() throws RepositoryException {
        Storage storage = Storage.fromEnvironment(Map.of("VARZINHO_STORAGE", "csv")::get, dataFolder);

        storage.operators().save(new Operator("Ana Lima", "ana", "Str0ng!pass"));

        assertTrue(new CsvOperatorStore(dataFolder.resolve("operators.csv")).hasAny());
    }

    @Test
    void choosesTheComposeDatabaseWhenNothingIsNamed() throws RepositoryException {
        Storage.Database database = Storage.databaseFrom(Map.<String, String>of()::get);

        assertEquals(new Storage.Database("jdbc:postgresql://localhost:5432/varzinho", "varzinho", "varzinho"),
                database);
    }

    @Test
    void treatsABlankDatabaseUrlAsUnset() throws RepositoryException {
        Storage.Database database = Storage.databaseFrom(Map.of("VARZINHO_DATABASE_URL", "  ")::get);

        assertEquals("jdbc:postgresql://localhost:5432/varzinho", database.url());
    }

    @Test
    void usesTheNamedDatabaseAndCredentialsOverTheDefaults() throws RepositoryException {
        Map<String, String> environment = Map.of(
                "VARZINHO_STORAGE", "postgresql",
                "VARZINHO_DATABASE_URL", UNREACHABLE_URL,
                "VARZINHO_DATABASE_USER", "arena",
                "VARZINHO_DATABASE_PASSWORD", "other");

        assertEquals(new Storage.Database(UNREACHABLE_URL, "arena", "other"), Storage.databaseFrom(environment::get));
    }

    @Test
    void refusesAStorageModeItDoesNotKnow() {
        Map<String, String> environment = Map.of("VARZINHO_STORAGE", "mysql");

        RepositoryException failure = assertThrows(RepositoryException.class,
                () -> Storage.fromEnvironment(environment::get, dataFolder));

        assertTrue(failure.getMessage().contains("VARZINHO_STORAGE"), failure.getMessage());
        assertTrue(failure.getMessage().contains("mysql"), failure.getMessage());
    }

    @Test
    void refusesTheCsvModeWhenADatabaseIsAlsoNamed() throws IOException {
        Map<String, String> environment = Map.of(
                "VARZINHO_STORAGE", "csv",
                "VARZINHO_DATABASE_URL", UNREACHABLE_URL,
                "VARZINHO_DATABASE_USER", "varzinho");

        RepositoryException failure = assertThrows(RepositoryException.class,
                () -> Storage.fromEnvironment(environment::get, dataFolder));

        assertTrue(failure.getMessage().contains("VARZINHO_DATABASE_URL"), failure.getMessage());
        try (Stream<Path> written = Files.list(dataFolder)) {
            assertEquals(0, written.count());
        }
    }

    @Test
    void refusesInsteadOfFallingBackToCsvWhenTheNamedDatabaseIsUnreachable() throws IOException {
        Map<String, String> environment = Map.of(
                "VARZINHO_DATABASE_URL", UNREACHABLE_URL,
                "VARZINHO_DATABASE_USER", "varzinho",
                "VARZINHO_DATABASE_PASSWORD", "secret-in-variable");

        RepositoryException failure = assertThrows(RepositoryException.class,
                () -> Storage.fromEnvironment(environment::get, dataFolder));

        assertTrue(failure.getMessage().contains(UNREACHABLE_URL), failure.getMessage());
        assertFalse(failure.getMessage().contains("secret-in-variable"), failure.getMessage());
        try (Stream<Path> written = Files.list(dataFolder)) {
            assertEquals(0, written.count());
        }
    }

    @Test
    void masksAPasswordCarriedInsideTheDatabaseUrl() {
        Map<String, String> environment = Map.of(
                "VARZINHO_DATABASE_URL", UNREACHABLE_URL + "&password=secret-in-url&ssl=false",
                "VARZINHO_DATABASE_USER", "varzinho");

        RepositoryException failure = assertThrows(RepositoryException.class,
                () -> Storage.fromEnvironment(environment::get, dataFolder));

        assertFalse(failure.getMessage().contains("secret-in-url"), failure.getMessage());
        assertTrue(failure.getMessage().contains("password=***&ssl=false"), failure.getMessage());
    }

    @Test
    void refusesADatabaseNamedWithoutAUser() {
        Map<String, String> environment = Map.of("VARZINHO_DATABASE_URL", UNREACHABLE_URL);

        RepositoryException failure = assertThrows(RepositoryException.class,
                () -> Storage.fromEnvironment(environment::get, dataFolder));

        assertTrue(failure.getMessage().contains("VARZINHO_DATABASE_USER"), failure.getMessage());
    }
}
