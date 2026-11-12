package br.edu.unipampa.varzinho.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Proves a real PostgreSQL is reachable through the JDBC driver. Skipped, not failed,
 * where Docker is missing — exactly as the FFmpeg test skips without FFmpeg.
 */
@Testcontainers(disabledWithoutDocker = true)
class PostgresSmokeTest {

    // Same image as docker-compose.yml, so the tests exercise the database the team runs.
    private static final String IMAGE = "postgres:16-alpine";

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(IMAGE);

    @Test
    void answersAQueryThroughTheJdbcDriver() throws SQLException {
        try (Connection connection = DriverManager.getConnection(
                        POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
                Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery("SELECT 1")) {
            assertTrue(result.next());
            assertEquals(1, result.getInt(1));
        }
    }
}
