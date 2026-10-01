package br.edu.unipampa.varzinho.repository;

import br.edu.unipampa.varzinho.domain.capture.Camera;
import br.edu.unipampa.varzinho.domain.capture.FixedCamera;
import br.edu.unipampa.varzinho.domain.capture.PtzCamera;
import br.edu.unipampa.varzinho.domain.capture.StreamCamera;
import br.edu.unipampa.varzinho.domain.structure.Court;
import br.edu.unipampa.varzinho.domain.structure.Gym;
import br.edu.unipampa.varzinho.enums.CameraStatus;
import br.edu.unipampa.varzinho.enums.Resolution;
import br.edu.unipampa.varzinho.exception.RepositoryException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CsvGymRepositoryTest {

    @TempDir
    private Path directory;

    @Test
    void aSavedGymLoadsBackWithItsCourtsCamerasAimAndStatus() throws RepositoryException {
        Gym gym = new Gym("Arena Norte", "Rua das Flores 12");
        gym.addCourt(new Court(1));
        gym.addCourt(new Court(4));
        FixedCamera fixed = new FixedCamera("fixed-1", "Bullet", Resolution.FULL_HD, 30, 45);
        PtzCamera ptz = new PtzCamera("ptz-1", "Domo", Resolution.ULTRA_HD, 20);
        FixedCamera bench = new FixedCamera("fixed-2", "Bullet", Resolution.HD, 30, 90);
        gym.installCamera(1, fixed);
        gym.installCamera(1, ptz);
        gym.installCamera(4, bench);
        fixed.startRecording();
        ptz.moveTo(120, -15, 4);
        bench.sendToMaintenance();
        CsvGymRepository repository = repository();

        repository.save(gym);
        Gym loaded = repository.load().orElseThrow();

        assertEquals("Arena Norte", loaded.getName());
        assertEquals("Rua das Flores 12", loaded.getAddress());
        assertEquals(List.of(1, 4), loaded.getCourts().stream().map(Court::getNumber).sorted().toList());
        List<Camera> courtOne = loaded.findCourt(1).getCameras();
        FixedCamera loadedFixed = assertInstanceOf(FixedCamera.class, courtOne.get(0));
        assertEquals("fixed-1", loadedFixed.getId());
        assertEquals("Bullet", loadedFixed.getModel());
        assertEquals(Resolution.FULL_HD, loadedFixed.getResolution());
        assertEquals(30, loadedFixed.getBufferSeconds());
        assertEquals(45, loadedFixed.getAngle());
        assertEquals(CameraStatus.ACTIVE, loadedFixed.getStatus());
        PtzCamera loadedPtz = assertInstanceOf(PtzCamera.class, courtOne.get(1));
        assertEquals(20, loadedPtz.getBufferSeconds());
        assertEquals(List.of(120, -15, 4), List.of(loadedPtz.getPan(), loadedPtz.getTilt(), loadedPtz.getZoom()));
        assertEquals(CameraStatus.INACTIVE, loadedPtz.getStatus());
        assertEquals(CameraStatus.MAINTENANCE, loaded.findCourt(4).getCameras().get(0).getStatus());
    }

    @Test
    void aGymWithoutAFileLoadsAsNothing() throws RepositoryException {
        assertTrue(repository().load().isEmpty());
    }

    @Test
    void savingAgainReplacesWhatWasSavedBefore() throws RepositoryException {
        CsvGymRepository repository = repository();
        Gym gym = new Gym("Arena", "Rua A");
        gym.addCourt(new Court(1));
        repository.save(gym);
        gym.addCourt(new Court(2));

        repository.save(gym);

        assertEquals(2, repository.load().orElseThrow().getCourts().size());
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
