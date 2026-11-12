package br.edu.unipampa.varzinho.infrastructure.persistence;

import br.edu.unipampa.varzinho.application.port.GymRepository;
import br.edu.unipampa.varzinho.application.port.RepositoryException;
import br.edu.unipampa.varzinho.domain.capture.Camera;
import br.edu.unipampa.varzinho.domain.capture.FixedCamera;
import br.edu.unipampa.varzinho.domain.capture.PtzCamera;
import br.edu.unipampa.varzinho.domain.capture.StreamCamera;
import br.edu.unipampa.varzinho.domain.structure.Court;
import br.edu.unipampa.varzinho.domain.structure.Gym;
import br.edu.unipampa.varzinho.enums.CameraStatus;
import br.edu.unipampa.varzinho.enums.Resolution;
import br.edu.unipampa.varzinho.exception.DomainException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Keeps the gym's structure in three PostgreSQL tables — the gym, its courts and their
 * cameras — reached through plain JDBC.
 *
 * <p>The tables are private to this class, and the class creates them when they are
 * missing. Saving replaces the whole gym inside one transaction, so the database never
 * holds half of one. Loading rebuilds the gym only through the domain's own behaviour, so
 * rows edited by hand cannot produce a gym the domain would have refused. Values reach SQL
 * only as bound parameters.
 */
public final class PostgresGymRepository implements GymRepository {

    private static final String FIXED = "fixed";
    private static final String PTZ = "ptz";

    // The gym table holds at most one row: the primary key can only ever be TRUE.
    private static final String CREATE_GYM = "CREATE TABLE IF NOT EXISTS gym ("
            + "singleton BOOLEAN PRIMARY KEY DEFAULT TRUE CHECK (singleton), "
            + "name TEXT NOT NULL, "
            + "address TEXT NOT NULL)";

    private static final String CREATE_COURT = "CREATE TABLE IF NOT EXISTS court ("
            + "number INTEGER PRIMARY KEY)";

    // Position keeps the order cameras were installed in. Each kind fills only its own aim
    // columns, so a row cannot describe a camera that is half fixed and half PTZ.
    private static final String CREATE_CAMERA = "CREATE TABLE IF NOT EXISTS camera ("
            + "court_number INTEGER NOT NULL REFERENCES court (number), "
            + "position INTEGER NOT NULL, "
            + "id TEXT NOT NULL, "
            + "kind TEXT NOT NULL, "
            + "model TEXT NOT NULL, "
            + "resolution TEXT NOT NULL, "
            + "buffer_seconds INTEGER NOT NULL, "
            + "status TEXT NOT NULL, "
            + "angle INTEGER, "
            + "pan INTEGER, "
            + "tilt INTEGER, "
            + "zoom INTEGER, "
            + "PRIMARY KEY (court_number, position), "
            + "CHECK ((kind = '" + FIXED + "' AND angle IS NOT NULL "
            + "AND pan IS NULL AND tilt IS NULL AND zoom IS NULL) "
            + "OR (kind = '" + PTZ + "' AND angle IS NULL "
            + "AND pan IS NOT NULL AND tilt IS NOT NULL AND zoom IS NOT NULL)))";

    private static final String INSERT_GYM = "INSERT INTO gym (name, address) VALUES (?, ?)";
    private static final String INSERT_COURT = "INSERT INTO court (number) VALUES (?)";
    private static final String INSERT_CAMERA = "INSERT INTO camera (court_number, position, id, kind, "
            + "model, resolution, buffer_seconds, status, angle, pan, tilt, zoom) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String SELECT_GYM = "SELECT name, address FROM gym";
    private static final String SELECT_COURTS = "SELECT number FROM court ORDER BY number";
    private static final String SELECT_CAMERAS = "SELECT court_number, id, kind, model, resolution, "
            + "buffer_seconds, status, angle, pan, tilt, zoom FROM camera ORDER BY court_number, position";

    private final String url;
    private final String user;
    private final String password;

    private PostgresGymRepository(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
    }

    /**
     * Connects to the database and creates the gym's tables when they are missing. Tables
     * that already exist are left as they are, the stored gym included.
     *
     * @throws IllegalArgumentException if the url or the user is missing
     * @throws RepositoryException if the database cannot be reached or the tables cannot be created
     */
    public static PostgresGymRepository open(String url, String user, String password)
            throws RepositoryException {
        if (url == null || url.isBlank() || user == null || user.isBlank()) {
            throw new IllegalArgumentException("the gym needs a database url and a user");
        }
        PostgresGymRepository repository = new PostgresGymRepository(url, user, password);
        repository.createTablesIfMissing();
        return repository;
    }

    /**
     * {@inheritDoc}
     *
     * <p>If any statement fails, the transaction is rolled back and the gym stored before
     * stays exactly as it was.
     */
    @Override
    public void save(Gym gym) throws RepositoryException {
        if (gym == null) {
            throw new IllegalArgumentException("there is no gym to save");
        }
        List<Court> courts = storableCourts(gym);
        try (Connection connection = connect()) {
            connection.setAutoCommit(false);
            try {
                deleteEverything(connection);
                insertGym(connection, gym);
                insertCourts(connection, courts);
                connection.commit();
            } catch (SQLException | RuntimeException failure) {
                rollback(connection, failure);
                throw failure;
            }
        } catch (SQLException cause) {
            throw new RepositoryException("could not save the gym " + gym.getName(), cause);
        }
    }

    // A stream camera is runtime equipment: it is installed afresh on every start, and
    // storing it would make this repository build the tool that writes its clips. A court
    // that exists only to hold one goes with it, or every start would add another.
    private static List<Court> storableCourts(Gym gym) {
        List<Court> courts = new ArrayList<>(gym.getCourts());
        courts.removeIf(PostgresGymRepository::holdsOnlyStreams);
        courts.sort(Comparator.comparingInt(Court::getNumber));
        for (Court court : courts) {
            for (Camera camera : court.getCameras()) {
                requireStorable(camera);
            }
        }
        return courts;
    }

    private static boolean holdsOnlyStreams(Court court) {
        List<Camera> cameras = court.getCameras();
        return !cameras.isEmpty() && cameras.stream().allMatch(StreamCamera.class::isInstance);
    }

    private static void requireStorable(Camera camera) {
        if (!(camera instanceof StreamCamera || camera instanceof FixedCamera || camera instanceof PtzCamera)) {
            throw new IllegalArgumentException("cannot store a camera of kind " + camera.getClass().getSimpleName());
        }
    }

    private static void deleteEverything(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("DELETE FROM camera");
            statement.executeUpdate("DELETE FROM court");
            statement.executeUpdate("DELETE FROM gym");
        }
    }

    private static void insertGym(Connection connection, Gym gym) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(INSERT_GYM)) {
            statement.setString(1, gym.getName());
            statement.setString(2, gym.getAddress());
            statement.executeUpdate();
        }
    }

    private static void insertCourts(Connection connection, List<Court> courts) throws SQLException {
        try (PreparedStatement courtRow = connection.prepareStatement(INSERT_COURT);
                PreparedStatement cameraRow = connection.prepareStatement(INSERT_CAMERA)) {
            for (Court court : courts) {
                courtRow.setInt(1, court.getNumber());
                courtRow.executeUpdate();
                int position = 0;
                for (Camera camera : court.getCameras()) {
                    if (!(camera instanceof StreamCamera)) {
                        insertCamera(cameraRow, court.getNumber(), position++, camera);
                    }
                }
            }
        }
    }

    private static void insertCamera(PreparedStatement row, int courtNumber, int position, Camera camera)
            throws SQLException {
        row.setInt(1, courtNumber);
        row.setInt(2, position);
        row.setString(3, camera.getId());
        row.setString(5, camera.getModel());
        row.setString(6, camera.getResolution().name());
        row.setInt(7, camera.getBufferSeconds());
        row.setString(8, camera.getStatus().name());
        if (camera instanceof PtzCamera) {
            PtzCamera ptz = (PtzCamera) camera;
            row.setString(4, PTZ);
            row.setNull(9, Types.INTEGER);
            row.setInt(10, ptz.getPan());
            row.setInt(11, ptz.getTilt());
            row.setInt(12, ptz.getZoom());
        } else {
            row.setString(4, FIXED);
            row.setInt(9, ((FixedCamera) camera).getAngle());
            row.setNull(10, Types.INTEGER);
            row.setNull(11, Types.INTEGER);
            row.setNull(12, Types.INTEGER);
        }
        row.executeUpdate();
    }

    private static void rollback(Connection connection, Exception failure) {
        try {
            connection.rollback();
        } catch (SQLException rollbackFailure) {
            // The original failure explains the save; the rollback failing too travels with it.
            failure.addSuppressed(rollbackFailure);
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>The three tables are read in one snapshot, so a save committed halfway through a
     * load cannot mix two gyms.
     */
    @Override
    public Optional<Gym> load() throws RepositoryException {
        try (Connection connection = connect()) {
            connection.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
            connection.setReadOnly(true);
            connection.setAutoCommit(false);
            try {
                return readGym(connection);
            } finally {
                connection.rollback();
            }
        } catch (SQLException cause) {
            throw new RepositoryException("could not read the gym", cause);
        }
    }

    private static Optional<Gym> readGym(Connection connection) throws SQLException, RepositoryException {
        try (Statement statement = connection.createStatement()) {
            Gym gym;
            try (ResultSet row = statement.executeQuery(SELECT_GYM)) {
                if (!row.next()) {
                    return Optional.empty();
                }
                gym = new Gym(row.getString("name"), row.getString("address"));
            }
            try (ResultSet rows = statement.executeQuery(SELECT_COURTS)) {
                while (rows.next()) {
                    gym.addCourt(new Court(rows.getInt("number")));
                }
            }
            try (ResultSet rows = statement.executeQuery(SELECT_CAMERAS)) {
                while (rows.next()) {
                    install(gym, rows);
                }
            }
            return Optional.of(gym);
        } catch (IllegalArgumentException | IllegalStateException | DomainException refused) {
            // A row edited by hand outside the application can break a rule the domain keeps;
            // half a gym would be a gym nobody built, so the whole load fails.
            throw new RepositoryException("the stored gym is not valid: " + refused.getMessage(), refused);
        }
    }

    private static void install(Gym gym, ResultSet row) throws SQLException {
        String kind = row.getString("kind");
        String id = row.getString("id");
        String model = row.getString("model");
        Resolution resolution = Resolution.valueOf(row.getString("resolution"));
        int bufferSeconds = row.getInt("buffer_seconds");
        Camera camera;
        if (FIXED.equals(kind)) {
            camera = new FixedCamera(id, model, resolution, bufferSeconds, row.getInt("angle"));
        } else if (PTZ.equals(kind)) {
            PtzCamera ptz = new PtzCamera(id, model, resolution, bufferSeconds);
            ptz.moveTo(row.getInt("pan"), row.getInt("tilt"), row.getInt("zoom"));
            camera = ptz;
        } else {
            throw new IllegalArgumentException("an unknown kind '" + kind + "'");
        }
        gym.installCamera(row.getInt("court_number"), camera);
        restore(camera, CameraStatus.valueOf(row.getString("status")));
    }

    private static void restore(Camera camera, CameraStatus status) {
        switch (status) {
            case ACTIVE: camera.startRecording(); break;
            case MAINTENANCE: camera.sendToMaintenance(); break;
            case INACTIVE: break;
            default: throw new IllegalArgumentException("no way to restore a camera to " + status);
        }
    }

    private void createTablesIfMissing() throws RepositoryException {
        try (Connection connection = connect();
                Statement statement = connection.createStatement()) {
            statement.execute(CREATE_GYM);
            statement.execute(CREATE_COURT);
            statement.execute(CREATE_CAMERA);
        } catch (SQLException cause) {
            throw new RepositoryException("could not prepare the gym tables at " + url, cause);
        }
    }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }
}
