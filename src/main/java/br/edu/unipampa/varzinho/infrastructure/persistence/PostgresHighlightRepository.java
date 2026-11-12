package br.edu.unipampa.varzinho.infrastructure.persistence;

import br.edu.unipampa.varzinho.application.port.HighlightRepository;
import br.edu.unipampa.varzinho.application.port.RepositoryException;
import br.edu.unipampa.varzinho.application.port.StorableHighlights;
import br.edu.unipampa.varzinho.domain.highlight.Highlight;
import br.edu.unipampa.varzinho.domain.highlight.VideoClip;
import br.edu.unipampa.varzinho.enums.Resolution;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Keeps highlights in a PostgreSQL table, reached through plain JDBC.
 *
 * <p>The table is private to this class, and the class creates it when it is missing, so
 * the Docker database and the tests share one schema without a migration tool. Every call
 * opens its own connection, so two instances on the same database always agree. Values
 * reach SQL only as bound parameters.
 */
public final class PostgresHighlightRepository implements HighlightRepository {

    // The capture instant takes two columns because TIMESTAMPTZ stops at microseconds and an
    // Instant carries nanoseconds. The position column keeps the order first saved.
    private static final String CREATE_TABLE = "CREATE TABLE IF NOT EXISTS highlight ("
            + "position BIGINT GENERATED ALWAYS AS IDENTITY, "
            + "id TEXT PRIMARY KEY, "
            + "captured_epoch_second BIGINT NOT NULL, "
            + "captured_nano INTEGER NOT NULL, "
            + "court_number INTEGER NOT NULL, "
            + "camera_id TEXT NOT NULL, "
            + "clip_path TEXT NOT NULL, "
            + "clip_duration_seconds INTEGER NOT NULL, "
            + "clip_resolution TEXT NOT NULL, "
            + "clip_size_mb DOUBLE PRECISION NOT NULL)";

    // A resave updates every column but position, so a replaced highlight keeps its place.
    private static final String UPSERT = "INSERT INTO highlight (id, captured_epoch_second, "
            + "captured_nano, court_number, camera_id, clip_path, clip_duration_seconds, "
            + "clip_resolution, clip_size_mb) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) "
            + "ON CONFLICT (id) DO UPDATE SET "
            + "captured_epoch_second = EXCLUDED.captured_epoch_second, "
            + "captured_nano = EXCLUDED.captured_nano, "
            + "court_number = EXCLUDED.court_number, "
            + "camera_id = EXCLUDED.camera_id, "
            + "clip_path = EXCLUDED.clip_path, "
            + "clip_duration_seconds = EXCLUDED.clip_duration_seconds, "
            + "clip_resolution = EXCLUDED.clip_resolution, "
            + "clip_size_mb = EXCLUDED.clip_size_mb";

    private static final String SELECT = "SELECT id, captured_epoch_second, captured_nano, "
            + "court_number, camera_id, clip_path, clip_duration_seconds, clip_resolution, "
            + "clip_size_mb FROM highlight";

    private final String url;
    private final String user;
    private final String password;

    private PostgresHighlightRepository(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
    }

    /**
     * Connects to the database and creates the highlight table when it is missing. A table
     * that already exists is left as it is, highlights included.
     *
     * @throws IllegalArgumentException if the url or the user is missing
     * @throws RepositoryException if the database cannot be reached or the table cannot be created
     */
    public static PostgresHighlightRepository open(String url, String user, String password)
            throws RepositoryException {
        if (url == null || url.isBlank() || user == null || user.isBlank()) {
            throw new IllegalArgumentException("the archive needs a database url and a user");
        }
        PostgresHighlightRepository repository = new PostgresHighlightRepository(url, user, password);
        repository.createTableIfMissing();
        return repository;
    }

    @Override
    public void save(Highlight highlight) throws RepositoryException {
        StorableHighlights.requireStorable(highlight);
        VideoClip clip = highlight.getClip();
        try (Connection connection = connect();
                PreparedStatement statement = connection.prepareStatement(UPSERT)) {
            statement.setString(1, highlight.getId());
            statement.setLong(2, highlight.getCapturedAt().getEpochSecond());
            statement.setInt(3, highlight.getCapturedAt().getNano());
            statement.setInt(4, highlight.getCourtNumber());
            statement.setString(5, highlight.getCameraId());
            statement.setString(6, clip.getFilePath());
            statement.setInt(7, clip.getDurationSeconds());
            statement.setString(8, clip.getResolution().name());
            statement.setDouble(9, clip.getSizeMb());
            statement.executeUpdate();
        } catch (SQLException cause) {
            throw new RepositoryException("could not save the highlight " + highlight.getId(), cause);
        }
    }

    @Override
    public Optional<Highlight> findById(String id) throws RepositoryException {
        if (id == null) {
            return Optional.empty();
        }
        try (Connection connection = connect();
                PreparedStatement statement = connection.prepareStatement(SELECT + " WHERE id = ?")) {
            statement.setString(1, id);
            return readAll(statement).stream().findFirst();
        } catch (SQLException cause) {
            throw new RepositoryException("could not look up the highlight " + id, cause);
        }
    }

    @Override
    public List<Highlight> findAll() throws RepositoryException {
        try (Connection connection = connect();
                PreparedStatement statement = connection.prepareStatement(SELECT + " ORDER BY position")) {
            return readAll(statement);
        } catch (SQLException cause) {
            throw new RepositoryException("could not read the highlights", cause);
        }
    }

    @Override
    public List<Highlight> findByCourt(int courtNumber) throws RepositoryException {
        try (Connection connection = connect();
                PreparedStatement statement = connection.prepareStatement(
                        SELECT + " WHERE court_number = ? ORDER BY position")) {
            statement.setInt(1, courtNumber);
            return readAll(statement);
        } catch (SQLException cause) {
            throw new RepositoryException("could not read the highlights of court " + courtNumber, cause);
        }
    }

    private void createTableIfMissing() throws RepositoryException {
        try (Connection connection = connect();
                Statement statement = connection.createStatement()) {
            statement.execute(CREATE_TABLE);
        } catch (SQLException cause) {
            throw new RepositoryException("could not prepare the highlight table at " + url, cause);
        }
    }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    private static List<Highlight> readAll(PreparedStatement statement)
            throws SQLException, RepositoryException {
        List<Highlight> highlights = new ArrayList<>();
        try (ResultSet rows = statement.executeQuery()) {
            while (rows.next()) {
                highlights.add(toHighlight(rows));
            }
        }
        return highlights;
    }

    private static Highlight toHighlight(ResultSet row) throws SQLException, RepositoryException {
        String id = row.getString("id");
        try {
            VideoClip clip = new VideoClip(row.getString("clip_path"), row.getInt("clip_duration_seconds"),
                    Resolution.valueOf(row.getString("clip_resolution")), row.getDouble("clip_size_mb"));
            Instant capturedAt = Instant.ofEpochSecond(
                    row.getLong("captured_epoch_second"), row.getInt("captured_nano"));
            return new Highlight(id, capturedAt, row.getInt("court_number"), row.getString("camera_id"), clip);
        } catch (IllegalArgumentException cause) {
            // A row edited by hand outside the application can break a rule the constructors keep.
            throw new RepositoryException("the stored highlight " + id + " is not a valid highlight", cause);
        }
    }
}
