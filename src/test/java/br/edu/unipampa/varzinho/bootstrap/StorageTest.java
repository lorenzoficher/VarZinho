package br.edu.unipampa.varzinho.bootstrap;

import br.edu.unipampa.varzinho.application.port.RepositoryException;
import br.edu.unipampa.varzinho.infrastructure.persistence.CsvGymRepository;
import br.edu.unipampa.varzinho.infrastructure.persistence.CsvHighlightRepository;
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

/** Needs no Docker: the database case only ever reaches a port nobody answers on. */
class StorageTest {
    private static final String UNREACHABLE_URL = "jdbc:postgresql://127.0.0.1:1/varzinho?connectTimeout=2";

    @TempDir
    Path dataFolder;

    @Test
    void storesInCsvWhenNoDatabaseIsNamed() throws RepositoryException {
        Storage storage = Storage.fromEnvironment(Map.<String, String>of()::get, dataFolder);

        assertInstanceOf(CsvHighlightRepository.class, storage.highlights());
        assertInstanceOf(CsvGymRepository.class, storage.gym());
    }

    @Test
    void storesInCsvWhenTheDatabaseUrlIsBlank() throws RepositoryException {
        Storage storage = Storage.fromEnvironment(Map.of("VARZINHO_DATABASE_URL", "  ")::get, dataFolder);

        assertInstanceOf(CsvHighlightRepository.class, storage.highlights());
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
