package br.edu.unipampa.varzinho.infrastructure.persistence;

import br.edu.unipampa.varzinho.application.port.GymRepository;
import br.edu.unipampa.varzinho.application.port.RepositoryException;
import br.edu.unipampa.varzinho.domain.capture.Camera;
import br.edu.unipampa.varzinho.domain.capture.FixedCamera;
import br.edu.unipampa.varzinho.domain.capture.StreamCamera;
import br.edu.unipampa.varzinho.domain.structure.Court;
import br.edu.unipampa.varzinho.domain.structure.Gym;
import br.edu.unipampa.varzinho.enums.Resolution;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CsvGymRepositoryTest extends GymRepositoryContractTest {

    @TempDir
    private Path directory;

    @Override
    protected GymRepository createRepository() {
        return repository();
    }

    @Test
    void aLineThatCannotBeReadIsReportedWithItsNumber() throws IOException {
        Path file = directory.resolve("gym.csv");
        Files.write(file, List.of(CsvGymRepository.HEADER, "gym,Arena,Rua A", "court,1",
                "fixed,1,fixed-1,Bullet,FULL_HD,30,ACTIVE,not-an-angle"));

        RepositoryException damage = assertThrows(RepositoryException.class, () -> repository().load());

        assertTrue(damage.getMessage().contains("line 4"), damage.getMessage());
    }

    @Test
    void aCameraOnACourtTheFileNeverDeclaredIsReportedWithItsNumber() throws IOException {
        Path file = directory.resolve("gym.csv");
        Files.write(file, List.of(CsvGymRepository.HEADER, "gym,Arena,Rua A",
                "fixed,7,fixed-1,Bullet,FULL_HD,30,ACTIVE,45"));

        RepositoryException damage = assertThrows(RepositoryException.class, () -> repository().load());

        assertTrue(damage.getMessage().contains("line 3"), damage.getMessage());
    }

    @Test
    void aFileThatLostItsHeaderIsRefused() throws IOException {
        Files.write(directory.resolve("gym.csv"), List.of("gym,Arena,Rua A"));

        assertThrows(RepositoryException.class, () -> repository().load());
    }

    @Test
    void textTheFileCannotHoldIsRefusedAndTheFileIsLeftAsItWas() throws RepositoryException, IOException {
        CsvGymRepository repository = repository();
        Gym saved = new Gym("Arena", "Rua A");
        repository.save(saved);
        List<String> before = Files.readAllLines(directory.resolve("gym.csv"));
        Gym gym = new Gym("Arena", "Rua A");
        gym.addCourt(new Court(1));
        gym.installCamera(1, new FixedCamera("fixed-1", "Bullet, 2nd gen", Resolution.HD, 30, 0));

        assertThrows(IllegalArgumentException.class, () -> repository.save(gym));

        assertEquals(before, Files.readAllLines(directory.resolve("gym.csv")));
        assertNull(repository.load().orElseThrow().findCourt(1));
    }

    @Test
    void aCourtHoldingOnlyAPhoneIsNotStored() throws RepositoryException {
        Gym gym = new Gym("Arena", "Rua A");
        gym.addCourt(new Court(1));
        gym.addCourt(new Court(2));
        gym.installCamera(1, new FixedCamera("fixed-1", "Bullet", Resolution.FULL_HD, 30, 45));
        gym.installCamera(2, phone());
        CsvGymRepository repository = repository();

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
        CsvGymRepository repository = repository();

        repository.save(gym);

        List<Camera> stored = repository.load().orElseThrow().findCourt(1).getCameras();
        assertEquals(List.of("fixed-1"), stored.stream().map(Camera::getId).toList());
    }

    private static StreamCamera phone() {
        return new StreamCamera("phone-1", "Phone", Resolution.HD, 30, "http://phone/video",
                (window, target) -> { });
    }

    private CsvGymRepository repository() {
        return new CsvGymRepository(directory.resolve("gym.csv"));
    }
}
