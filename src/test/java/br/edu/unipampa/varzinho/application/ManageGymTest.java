package br.edu.unipampa.varzinho.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.edu.unipampa.varzinho.domain.structure.Court;
import br.edu.unipampa.varzinho.domain.structure.Gym;
import br.edu.unipampa.varzinho.enums.Resolution;
import br.edu.unipampa.varzinho.infrastructure.persistence.InMemoryGymRepository;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

class ManageGymTest {

    @Test
    void addsAndSavesACourtThenListsCourtsInNumberOrder() throws Exception {
        Gym gym = gymWithCourts(4, 1);
        InMemoryGymRepository repository = new InMemoryGymRepository();
        ManageGym management = new ManageGym(gym, repository);

        CourtView added = management.addCourt(3);

        assertEquals(3, added.getNumber());
        assertEquals(List.of(1, 3, 4), courtNumbers(management.courts()));
        assertSame(gym, repository.load().orElseThrow());
    }

    @Test
    void installsEachCameraKindAndReturnsViewsInsteadOfEntities() throws Exception {
        Gym gym = gymWithCourts(1);
        InMemoryGymRepository repository = new InMemoryGymRepository();
        ManageGym management = new ManageGym(gym, repository);

        management.installCamera(1, spec("fixed-1", CameraKind.FIXED, 45));
        CourtView court = management.installCamera(1, spec("ptz-1", CameraKind.PTZ, 0));

        assertEquals(List.of("fixed-1", "ptz-1"), cameraIds(court));
        assertFalse(court.getCameras().get(0).isAimable());
        assertTrue(court.getCameras().get(1).isAimable());
        assertTrue(court.getCameras().get(0).getDescription().contains("fixed"));
        assertTrue(court.getCameras().get(1).getDescription().contains("PTZ"));
        assertSame(gym, repository.load().orElseThrow());
    }

    @Test
    void refusedChangeIsNotSaved() {
        Gym gym = gymWithCourts(1);
        InMemoryGymRepository repository = new InMemoryGymRepository();
        ManageGym management = new ManageGym(gym, repository);

        assertThrows(IllegalArgumentException.class, () -> management.addCourt(1));

        assertTrue(repository.load().isEmpty());
    }

    @Test
    void anUnknownCourtIsRefusedWithoutExposingANull() {
        ManageGym management = new ManageGym(gymWithCourts(1), new InMemoryGymRepository());

        IllegalArgumentException failure =
                assertThrows(IllegalArgumentException.class, () -> management.court(9));

        assertEquals("court 9 is not registered", failure.getMessage());
    }

    private static Gym gymWithCourts(int... numbers) {
        Gym gym = new Gym("VarZinho Arena", "100 Sports Avenue");
        for (int number : numbers) {
            gym.addCourt(new Court(number));
        }
        return gym;
    }

    private static CameraSpec spec(String id, CameraKind kind, int angle) {
        return new CameraSpec(id, "Model " + id, kind, Resolution.FULL_HD, 30, angle);
    }

    private static List<Integer> courtNumbers(List<CourtView> courts) {
        return courts.stream().map(CourtView::getNumber).collect(Collectors.toList());
    }

    private static List<String> cameraIds(CourtView court) {
        return court.getCameras().stream().map(CameraView::getId).collect(Collectors.toList());
    }
}
