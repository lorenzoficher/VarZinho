package br.edu.unipampa.varzinho.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import br.edu.unipampa.varzinho.application.port.HighlightRepository;
import br.edu.unipampa.varzinho.application.port.RepositoryException;
import br.edu.unipampa.varzinho.domain.highlight.Highlight;
import br.edu.unipampa.varzinho.domain.highlight.VideoClip;
import br.edu.unipampa.varzinho.enums.Resolution;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * The PostgreSQL adapter against a real database. Skipped, not failed, where Docker is
 * missing — exactly as {@link PostgresSmokeTest}.
 */
@Testcontainers(disabledWithoutDocker = true)
class PostgresHighlightRepositoryTest extends HighlightRepositoryContractTest {

    // Same image as docker-compose.yml, so the tests exercise the database the team runs.
    private static final String IMAGE = "postgres:16.15-alpine";

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(IMAGE);

    // One container serves the whole class, so each contract test starts from a dropped table
    // and the adapter has to create it again.
    @Override
    protected HighlightRepository createRepository() {
        execute("DROP TABLE IF EXISTS highlight");
        try {
            return open();
        } catch (RepositoryException cause) {
            throw new AssertionError("could not open the repository on the test database", cause);
        }
    }

    @Test
    void reopeningADatabaseThatHoldsHighlightsKeepsThem() throws RepositoryException {
        HighlightRepository first = createRepository();
        first.save(highlight("h-001", 3));

        HighlightRepository second = open();

        assertEquals("h-001", second.findById("h-001").orElseThrow().getId());
        assertEquals(1, first.findAll().size());
    }

    @Test
    void readsBackEveryFieldIncludingTheNanosecondsOfTheCapture() throws RepositoryException {
        HighlightRepository repository = createRepository();
        VideoClip clip = new VideoClip("/clips/h-001.mp4", 28, Resolution.HD, 17.25);
        Highlight saved = new Highlight("h-001", Instant.parse("2026-09-12T20:14:33.123456789Z"), 5,
                "cam-b2", clip);

        repository.save(saved);
        Highlight read = repository.findById("h-001").orElseThrow();

        assertEquals(saved.getCapturedAt(), read.getCapturedAt());
        assertEquals(saved.getCourtNumber(), read.getCourtNumber());
        assertEquals(saved.getCameraId(), read.getCameraId());
        assertEquals(clip.getFilePath(), read.getClip().getFilePath());
        assertEquals(clip.getDurationSeconds(), read.getClip().getDurationSeconds());
        assertEquals(clip.getResolution(), read.getClip().getResolution());
        assertEquals(clip.getSizeMb(), read.getClip().getSizeMb());
    }

    @Test
    void storesAnIdThatLooksLikeSqlAsPlainText() throws RepositoryException {
        HighlightRepository repository = createRepository();
        String hostileId = "h-001'); DROP TABLE highlight; --";

        repository.save(highlight(hostileId, 3));

        assertEquals(List.of(hostileId), List.of(repository.findAll().get(0).getId()));
        assertEquals(hostileId, repository.findById(hostileId).orElseThrow().getId());
    }

    @Test
    void aFailingQueryRaisesRepositoryExceptionWithTheSqlCause() {
        HighlightRepository repository = createRepository();
        execute("DROP TABLE highlight");

        RepositoryException failure = assertThrows(RepositoryException.class, repository::findAll);

        assertInstanceOf(SQLException.class, failure.getCause());
    }

    private static HighlightRepository open() throws RepositoryException {
        return PostgresHighlightRepository.open(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    }

    private static void execute(String sql) {
        try (Connection connection = DriverManager.getConnection(
                        POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
                Statement statement = connection.createStatement()) {
            statement.execute(sql);
        } catch (SQLException cause) {
            throw new AssertionError("could not prepare the test database", cause);
        }
    }
}
