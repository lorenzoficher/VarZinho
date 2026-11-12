package br.edu.unipampa.varzinho.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * The PostgreSQL gym adapter against a real database. Skipped, not failed, where Docker is
 * missing — exactly as {@link PostgresSmokeTest}.
 */
@Testcontainers(disabledWithoutDocker = true)
class PostgresGymRepositoryTest extends GymRepositoryContractTest {

    // Same image as docker-compose.yml, so the tests exercise the database the team runs.
    private static final String IMAGE = "postgres:16.15-alpine";

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(IMAGE);

    // One container serves the whole class, so each test starts from dropped tables and the
    // adapter has to create them again.
    @Override
    protected GymRepository createRepository() {
        execute("DROP TABLE IF EXISTS camera, court, gym CASCADE");
        execute("DROP FUNCTION IF EXISTS refuse_camera()");
        try {
            return open();
        } catch (RepositoryException cause) {
            throw new AssertionError("could not open the repository on the test database", cause);
        }
    }

    @Test
    void reopeningADatabaseThatHoldsAGymKeepsIt() throws RepositoryException {
        createRepository().save(new Gym("Arena", "Rua A"));

        GymRepository reopened = open();

        assertEquals("Arena", reopened.load().orElseThrow().getName());
    }

    @Test
    void aSaveThatFailsPartWayLeavesThePreviousGymUnchanged() throws RepositoryException {
        GymRepository repository = createRepository();
        Gym previous = new Gym("Arena", "Rua A");
        previous.addCourt(new Court(1));
        repository.save(previous);
        execute("CREATE FUNCTION refuse_camera() RETURNS trigger AS $$ BEGIN "
                + "RAISE EXCEPTION 'camera refused'; END; $$ LANGUAGE plpgsql");
        execute("CREATE TRIGGER refuse_camera BEFORE INSERT ON camera "
                + "FOR EACH ROW EXECUTE FUNCTION refuse_camera()");
        Gym replacement = new Gym("Arena Sul", "Rua B");
        replacement.addCourt(new Court(2));
        replacement.installCamera(2, new FixedCamera("fixed-1", "Bullet", Resolution.HD, 30, 0));

        RepositoryException failure = assertThrows(RepositoryException.class, () -> repository.save(replacement));

        assertInstanceOf(SQLException.class, failure.getCause());
        Gym loaded = repository.load().orElseThrow();
        assertEquals("Arena", loaded.getName());
        assertEquals(List.of(1), loaded.getCourts().stream().map(Court::getNumber).toList());
    }

    @Test
    void camerasComeBackInTheOrderTheyWereInstalled() throws RepositoryException {
        Gym gym = new Gym("Arena", "Rua A");
        gym.addCourt(new Court(1));
        gym.installCamera(1, new PtzCamera("z-ptz", "Domo", Resolution.HD, 30));
        gym.installCamera(1, new FixedCamera("a-fixed", "Bullet", Resolution.HD, 30, 0));
        GymRepository repository = createRepository();

        repository.save(gym);

        assertEquals(List.of("z-ptz", "a-fixed"), repository.load().orElseThrow().findCourt(1).getCameras()
                .stream().map(Camera::getId).toList());
    }

    @Test
    void aCourtHoldingOnlyAPhoneIsNotStored() throws RepositoryException {
        Gym gym = new Gym("Arena", "Rua A");
        gym.addCourt(new Court(1));
        gym.addCourt(new Court(2));
        gym.installCamera(1, new FixedCamera("fixed-1", "Bullet", Resolution.FULL_HD, 30, 45));
        gym.installCamera(2, phone());
        GymRepository repository = createRepository();

        repository.save(gym);

        assertEquals(List.of(1), repository.load().orElseThrow().getCourts().stream()
                .map(Court::getNumber).toList());
    }

    @Test
    void aPhoneSharingACourtLeavesTheOtherCamerasStored() throws RepositoryException {
        Gym gym = new Gym("Arena", "Rua A");
        gym.addCourt(new Court(1));
        gym.installCamera(1, new FixedCamera("fixed-1", "Bullet", Resolution.FULL_HD, 30, 45));
        gym.installCamera(1, phone());
        GymRepository repository = createRepository();

        repository.save(gym);

        List<Camera> stored = repository.load().orElseThrow().findCourt(1).getCameras();
        assertEquals(List.of("fixed-1"), stored.stream().map(Camera::getId).toList());
        assertEquals(CameraStatus.INACTIVE, stored.get(0).getStatus());
    }

    @Test
    void storesTextThatLooksLikeSqlOrHoldsACommaAsPlainText() throws RepositoryException {
        GymRepository repository = createRepository();
        String hostileName = "Arena'); DROP TABLE gym; --";
        Gym gym = new Gym(hostileName, "Rua A, 12");
        gym.addCourt(new Court(1));
        gym.installCamera(1, new FixedCamera("fixed-1", "Bullet, 2nd gen", Resolution.HD, 30, 0));

        repository.save(gym);

        Gym loaded = repository.load().orElseThrow();
        assertEquals(hostileName, loaded.getName());
        assertEquals("Rua A, 12", loaded.getAddress());
        assertEquals("Bullet, 2nd gen", loaded.findCourt(1).getCameras().get(0).getModel());
    }

    @Test
    void aStoredCameraTheDomainRefusesIsReportedAsDamage() throws RepositoryException {
        GymRepository repository = createRepository();
        Gym gym = new Gym("Arena", "Rua A");
        gym.addCourt(new Court(1));
        gym.installCamera(1, new FixedCamera("fixed-1", "Bullet", Resolution.HD, 30, 0));
        repository.save(gym);
        execute("UPDATE camera SET buffer_seconds = -5");

        assertThrows(RepositoryException.class, repository::load);
    }

    @Test
    void aFailingQueryRaisesRepositoryExceptionWithTheSqlCause() throws RepositoryException {
        GymRepository repository = createRepository();
        repository.save(new Gym("Arena", "Rua A"));
        execute("DROP TABLE camera");

        RepositoryException failure = assertThrows(RepositoryException.class, repository::load);

        assertInstanceOf(SQLException.class, failure.getCause());
    }

    private static StreamCamera phone() {
        return new StreamCamera("phone-1", "Phone", Resolution.HD, 30, "http://phone/video",
                (window, target) -> { });
    }

    private static GymRepository open() throws RepositoryException {
        return PostgresGymRepository.open(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
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
