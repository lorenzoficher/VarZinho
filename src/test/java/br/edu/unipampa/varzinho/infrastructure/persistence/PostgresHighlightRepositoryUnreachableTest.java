package br.edu.unipampa.varzinho.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import br.edu.unipampa.varzinho.application.port.RepositoryException;

import java.sql.SQLException;

import org.junit.jupiter.api.Test;

/** Needs no Docker: a database nobody answers for fails the same everywhere. */
class PostgresHighlightRepositoryUnreachableTest {

    @Test
    void openingAnUnreachableDatabaseRaisesRepositoryExceptionWithTheSqlCause() {
        RepositoryException failure = assertThrows(RepositoryException.class,
                () -> PostgresHighlightRepository.open(
                        "jdbc:postgresql://127.0.0.1:1/varzinho?connectTimeout=2", "varzinho", "varzinho"));

        assertInstanceOf(SQLException.class, failure.getCause());
    }
}
