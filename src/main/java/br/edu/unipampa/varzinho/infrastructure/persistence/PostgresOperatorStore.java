package br.edu.unipampa.varzinho.infrastructure.persistence;

import br.edu.unipampa.varzinho.application.port.OperatorStore;
import br.edu.unipampa.varzinho.application.port.RepositoryException;
import br.edu.unipampa.varzinho.application.port.StorableOperators;
import br.edu.unipampa.varzinho.domain.people.Operator;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;
import java.util.Optional;

/** Keeps operator accounts in PostgreSQL through plain JDBC. */
public final class PostgresOperatorStore implements OperatorStore {
    private static final String CREATE_TABLE = "CREATE TABLE IF NOT EXISTS operator_account ("
            + "name TEXT NOT NULL, username TEXT NOT NULL, password_hash TEXT NOT NULL)";
    private static final String CREATE_INDEX = "CREATE UNIQUE INDEX IF NOT EXISTS operator_username_unique "
            + "ON operator_account (lower(username))";
    // Only a collision on the username is a domain refusal. Other SQL failures still propagate.
    private static final String INSERT = "INSERT INTO operator_account (name, username, password_hash) "
            + "VALUES (?, ?, ?) ON CONFLICT (lower(username)) DO NOTHING";
    private static final String SELECT = "SELECT name, username, password_hash FROM operator_account "
            + "WHERE lower(username) = ?";
    private final String url;
    private final String user;
    private final String password;

    private PostgresOperatorStore(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
    }

    /**
     * Opens the store, creating its table and case-insensitive unique index if missing.
     * Existing accounts are retained. Each operation opens its own connection.
     *
     * @throws IllegalArgumentException if the url or user is missing
     * @throws RepositoryException if the database cannot be reached or prepared
     */
    public static PostgresOperatorStore open(String url, String user, String password)
            throws RepositoryException {
        if (url == null || url.isBlank() || user == null || user.isBlank()) {
            throw new IllegalArgumentException("the operator store needs a database url and a user");
        }
        PostgresOperatorStore store = new PostgresOperatorStore(url, user, password);
        store.createTableIfMissing();
        return store;
    }

    @Override
    public void save(Operator operator) throws RepositoryException {
        StorableOperators.requireStorable(operator);
        try (Connection connection = connect(); PreparedStatement statement = connection.prepareStatement(INSERT)) {
            statement.setString(1, operator.getName());
            statement.setString(2, operator.getUsername());
            statement.setString(3, operator.getPasswordHash());
            if (statement.executeUpdate() == 0) {
                throw new IllegalArgumentException("username " + operator.getUsername() + " is already taken");
            }
        } catch (SQLException cause) {
            throw new RepositoryException("could not save the operator " + operator.getUsername(), cause);
        }
    }

    @Override
    public Optional<Operator> findByUsername(String username) throws RepositoryException {
        if (username == null) return Optional.empty();
        try (Connection connection = connect(); PreparedStatement statement = connection.prepareStatement(SELECT)) {
            statement.setString(1, username.toLowerCase(Locale.ROOT));
            try (ResultSet rows = statement.executeQuery()) {
                if (!rows.next()) return Optional.empty();
                try {
                    return Optional.of(Operator.restore(rows.getString("name"), rows.getString("username"),
                            rows.getString("password_hash")));
                } catch (IllegalArgumentException cause) {
                    throw new RepositoryException("the stored operator " + username + " is not valid", cause);
                }
            }
        } catch (SQLException cause) {
            throw new RepositoryException("could not look up the operator " + username, cause);
        }
    }

    @Override
    public boolean hasAny() throws RepositoryException {
        try (Connection connection = connect(); PreparedStatement statement = connection.prepareStatement(
                "SELECT EXISTS (SELECT 1 FROM operator_account)"); ResultSet rows = statement.executeQuery()) {
            rows.next();
            return rows.getBoolean(1);
        } catch (SQLException cause) {
            throw new RepositoryException("could not check for operator accounts", cause);
        }
    }

    private void createTableIfMissing() throws RepositoryException {
        try (Connection connection = connect()) {
            connection.setAutoCommit(false);
            try (Statement statement = connection.createStatement()) {
                statement.execute(CREATE_TABLE);
                statement.execute(CREATE_INDEX);
                connection.commit();
            } catch (SQLException cause) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackFailure) {
                    cause.addSuppressed(rollbackFailure);
                }
                throw cause;
            }
        } catch (SQLException cause) {
            throw new RepositoryException("could not prepare the operator store", cause);
        }
    }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }
}
