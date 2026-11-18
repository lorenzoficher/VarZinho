package br.edu.unipampa.varzinho.infrastructure.persistence;

import br.edu.unipampa.varzinho.application.port.OperatorStore;
import br.edu.unipampa.varzinho.application.port.RepositoryException;
import br.edu.unipampa.varzinho.domain.people.Operator;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Runs the shared store contract against PostgreSQL; skipped without Docker. */
@Testcontainers(disabledWithoutDocker = true)
class PostgresOperatorStoreTest extends OperatorStoreContractTest {
    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16.15-alpine");

    @Override
    protected OperatorStore createStore() {
        try (Connection connection = connect(); Statement statement = connection.createStatement()) {
            statement.execute("DROP TABLE IF EXISTS operator_account");
            return open();
        } catch (SQLException | RepositoryException cause) {
            throw new AssertionError("could not prepare the operator store", cause);
        }
    }

    @Test
    void reopeningTheStoreKeepsTheAccountAndItsOriginalHash() throws RepositoryException {
        OperatorStore store = createStore();
        Operator operator = operator("alice");
        store.save(operator);

        Operator restored = open().findByUsername("ALICE").orElseThrow();

        assertEquals(operator.getName(), restored.getName());
        assertEquals(operator.getPasswordHash(), restored.getPasswordHash());
        assertTrue(restored.checksPassword("Strong1!"));
    }

    @Test
    void theTableStoresOnlyIdentityAndTheEncodedHash() throws RepositoryException, SQLException {
        OperatorStore store = createStore();
        Operator operator = operator("alice");
        store.save(operator);

        try (Connection connection = connect(); Statement statement = connection.createStatement()) {
            try (ResultSet columns = statement.executeQuery("SELECT column_name FROM information_schema.columns "
                    + "WHERE table_schema = 'public' AND table_name = 'operator_account' ORDER BY ordinal_position")) {
                List<String> names = new ArrayList<>();
                while (columns.next()) names.add(columns.getString(1));
                assertEquals(List.of("name", "username", "password_hash"), names);
            }
            try (ResultSet row = statement.executeQuery("SELECT password_hash FROM operator_account")) {
                assertTrue(row.next());
                assertEquals(operator.getPasswordHash(), row.getString(1));
                assertFalse(row.getString(1).contains("Strong1!"));
            }
        }
    }

    @Test
    void theDatabaseRefusesCaseVariantsEvenWhenSqlBypassesTheAdapter() throws RepositoryException, SQLException {
        OperatorStore store = createStore();
        Operator original = operator("alice");
        store.save(original);

        try (Connection connection = connect(); PreparedStatement insert = connection.prepareStatement(
                "INSERT INTO operator_account (name, username, password_hash) VALUES (?, ?, ?)")) {
            insert.setString(1, "Another Operator");
            insert.setString(2, "ALICE");
            insert.setString(3, original.getPasswordHash());

            SQLException failure = assertThrows(SQLException.class, insert::executeUpdate);

            assertEquals("23505", failure.getSQLState());
        }
        assertEquals(original.getName(), store.findByUsername("alice").orElseThrow().getName());
    }

    @Test
    void twoWindowsCannotBothSaveTheSameUsername() throws Exception {
        OperatorStore firstStore = createStore();
        OperatorStore secondStore = open();
        Operator first = new Operator("First Operator", "alice", "Strong1!");
        Operator second = new Operator("Second Operator", "ALICE", "Another1!");
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch release = new CountDownLatch(1);
        ExecutorService workers = Executors.newFixedThreadPool(2);
        try {
            Future<Boolean> firstSave = workers.submit(() -> saveTogether(firstStore, first, ready, release));
            Future<Boolean> secondSave = workers.submit(() -> saveTogether(secondStore, second, ready, release));
            assertTrue(ready.await(5, TimeUnit.SECONDS));
            release.countDown();

            boolean firstWon = firstSave.get(10, TimeUnit.SECONDS);
            boolean secondWon = secondSave.get(10, TimeUnit.SECONDS);

            assertTrue(firstWon ^ secondWon, "exactly one insert must succeed");
            Operator winner = firstWon ? first : second;
            Operator restored = open().findByUsername("alice").orElseThrow();
            assertEquals(winner.getName(), restored.getName());
            assertEquals(winner.getPasswordHash(), restored.getPasswordHash());
            try (Connection connection = connect(); Statement statement = connection.createStatement();
                    ResultSet count = statement.executeQuery("SELECT count(*) FROM operator_account")) {
                assertTrue(count.next());
                assertEquals(1, count.getInt(1));
            }
        } finally {
            release.countDown();
            workers.shutdownNow();
            assertTrue(workers.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    @Test
    void failingOperationsPreserveTheirSqlCauses() throws RepositoryException, SQLException {
        OperatorStore store = createStore();
        Operator operator = operator("alice");
        try (Connection connection = connect(); Statement statement = connection.createStatement()) {
            statement.execute("DROP TABLE operator_account");
        }

        assertInstanceOf(SQLException.class,
                assertThrows(RepositoryException.class, () -> store.save(operator)).getCause());
        assertInstanceOf(SQLException.class,
                assertThrows(RepositoryException.class, () -> store.findByUsername("alice")).getCause());
        assertInstanceOf(SQLException.class,
                assertThrows(RepositoryException.class, store::hasAny).getCause());
    }

    @Test
    void anInvalidStoredHashIsReportedAsARepositoryFailure() throws RepositoryException, SQLException {
        OperatorStore store = createStore();
        try (Connection connection = connect(); Statement statement = connection.createStatement()) {
            statement.execute("INSERT INTO operator_account VALUES ('Alice', 'alice', 'invalid-hash')");
        }

        RepositoryException failure = assertThrows(RepositoryException.class,
                () -> store.findByUsername("alice"));

        assertInstanceOf(IllegalArgumentException.class, failure.getCause());
    }

    private static boolean saveTogether(OperatorStore store, Operator operator,
                                        CountDownLatch ready, CountDownLatch release)
            throws InterruptedException, RepositoryException {
        ready.countDown();
        if (!release.await(5, TimeUnit.SECONDS)) throw new AssertionError("inserts were not released");
        try {
            store.save(operator);
            return true;
        } catch (IllegalArgumentException duplicate) {
            assertTrue(duplicate.getMessage().contains("already taken"));
            return false;
        }
    }

    private static Operator operator(String username) {
        return new Operator("Alice Operator", username, "Strong1!");
    }

    private static PostgresOperatorStore open() throws RepositoryException {
        return PostgresOperatorStore.open(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    }

    private static Connection connect() throws SQLException {
        return DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    }
}
