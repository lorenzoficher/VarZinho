package br.edu.unipampa.varzinho.infrastructure.persistence;

import br.edu.unipampa.varzinho.application.port.GymRepository;
import br.edu.unipampa.varzinho.application.port.RepositoryException;
import br.edu.unipampa.varzinho.domain.capture.Camera;
import br.edu.unipampa.varzinho.domain.capture.FixedCamera;
import br.edu.unipampa.varzinho.domain.capture.PtzCamera;
import br.edu.unipampa.varzinho.domain.structure.Court;
import br.edu.unipampa.varzinho.domain.structure.Gym;
import br.edu.unipampa.varzinho.enums.CameraStatus;
import br.edu.unipampa.varzinho.enums.Resolution;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What every {@link GymRepository} must do, whatever it stores into. Each
 * implementation's test extends this class and supplies a fresh, empty repository, so
 * any adapter that passes can stand in for the others.
 */
abstract class GymRepositoryContractTest {

    protected abstract GymRepository createRepository();

    @Test
    void anEmptyRepositoryLoadsNothing() throws RepositoryException {
        assertTrue(createRepository().load().isEmpty());
    }

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
        GymRepository repository = createRepository();

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
    void savingAgainReplacesWhatWasSavedBefore() throws RepositoryException {
        GymRepository repository = createRepository();
        repository.save(new Gym("Arena", "Rua A"));
        Gym replacement = new Gym("Arena Sul", "Rua B");
        replacement.addCourt(new Court(1));
        replacement.addCourt(new Court(2));

        repository.save(replacement);

        Gym loaded = repository.load().orElseThrow();
        assertEquals("Arena Sul", loaded.getName());
        assertEquals(2, loaded.getCourts().size());
    }
}
