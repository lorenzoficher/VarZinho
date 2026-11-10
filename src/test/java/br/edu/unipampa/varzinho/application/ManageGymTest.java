package br.edu.unipampa.varzinho.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.edu.unipampa.varzinho.domain.structure.Court;
import br.edu.unipampa.varzinho.domain.structure.Gym;
import br.edu.unipampa.varzinho.domain.capture.FixedCamera;
import br.edu.unipampa.varzinho.domain.capture.Frame;
import br.edu.unipampa.varzinho.domain.capture.PtzCamera;
import br.edu.unipampa.varzinho.enums.CameraStatus;
import br.edu.unipampa.varzinho.enums.Resolution;
import br.edu.unipampa.varzinho.infrastructure.persistence.InMemoryGymRepository;

import java.util.List;
import java.time.Instant;
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

    @Test
    void changesCameraStateAndRemovesItByCourtNumberAndId() throws Exception {
        Gym gym = gymWithCourts(1);
        FixedCamera camera = fixedCamera("fixed-1");
        gym.installCamera(1, camera);
        InMemoryGymRepository repository = new InMemoryGymRepository();
        ManageGym management = new ManageGym(gym, repository);

        CourtView active = management.startRecording(1, "fixed-1");
        assertEquals(CameraStatus.ACTIVE, camera.getStatus());
        assertTrue(active.getCameras().get(0).getDescription().contains("ACTIVE"));

        management.stopRecording(1, "fixed-1");
        assertEquals(CameraStatus.INACTIVE, camera.getStatus());

        management.sendToMaintenance(1, "fixed-1");
        assertEquals(CameraStatus.MAINTENANCE, camera.getStatus());

        CourtView empty = management.removeCamera(1, "fixed-1");
        assertTrue(empty.getCameras().isEmpty());
        assertSame(gym, repository.load().orElseThrow());
    }

    @Test
    void refusedStateChangeDoesNotSaveTheGym() {
        Gym gym = gymWithCourts(1);
        FixedCamera camera = fixedCamera("fixed-1");
        camera.sendToMaintenance();
        gym.installCamera(1, camera);
        InMemoryGymRepository repository = new InMemoryGymRepository();
        ManageGym management = new ManageGym(gym, repository);

        assertThrows(IllegalStateException.class,
                () -> management.startRecording(1, "fixed-1"));

        assertTrue(repository.load().isEmpty());
    }

    @Test
    void anUnknownCameraIsRefusedByItsId() {
        ManageGym management = new ManageGym(gymWithCourts(1), new InMemoryGymRepository());

        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> management.startRecording(1, "missing"));

        assertEquals("camera missing is not installed on court 1", failure.getMessage());
    }

    @Test
    void movesACameraByCourtNumbersAndIdThenSavesBothSides() throws Exception {
        Gym gym = gymWithCourts(1, 2);
        PtzCamera camera = ptzCamera("ptz-1");
        camera.startRecording();
        gym.installCamera(1, camera);
        InMemoryGymRepository repository = new InMemoryGymRepository();
        ManageGym management = new ManageGym(gym, repository);

        CourtView source = management.moveCamera(1, "ptz-1", 2);

        assertTrue(source.getCameras().isEmpty());
        assertEquals(List.of("ptz-1"), cameraIds(management.court(2)));
        assertEquals(CameraStatus.INACTIVE, camera.getStatus());
        assertSame(gym, repository.load().orElseThrow());
    }

    @Test
    void aimsOnlyAnAimableCameraAndReturnsItsNewDescription() throws Exception {
        Gym gym = gymWithCourts(1);
        gym.installCamera(1, ptzCamera("ptz-1"));
        InMemoryGymRepository repository = new InMemoryGymRepository();
        ManageGym management = new ManageGym(gym, repository);

        CourtView court = management.aimCamera(1, "ptz-1", 90, -10, 3);

        CameraView camera = court.getCameras().get(0);
        assertTrue(camera.isAimable());
        assertTrue(camera.getDescription().contains("pan 90 tilt -10 zoom 3"));
        assertSame(gym, repository.load().orElseThrow());
    }

    @Test
    void refusingToAimAFixedCameraDoesNotSaveTheGym() {
        Gym gym = gymWithCourts(1);
        gym.installCamera(1, fixedCamera("fixed-1"));
        InMemoryGymRepository repository = new InMemoryGymRepository();
        ManageGym management = new ManageGym(gym, repository);

        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> management.aimCamera(1, "fixed-1", 90, 0, 2));

        assertEquals("camera fixed-1 cannot be aimed", failure.getMessage());
        assertTrue(repository.load().isEmpty());
    }

    @Test
    void courtViewReportsRecordedSecondsWithoutExposingItsCameraList() throws Exception {
        Gym gym = gymWithCourts(1);
        FixedCamera camera = fixedCamera("fixed-1");
        gym.installCamera(1, camera);
        camera.startRecording();
        Court court = gym.findCourt(1);
        court.record(new Frame(Instant.parse("2026-11-10T20:00:00Z"), 1));
        court.record(new Frame(Instant.parse("2026-11-10T20:00:01Z"), 2));

        CourtView view = new ManageGym(gym, new InMemoryGymRepository()).court(1);

        assertEquals(2, view.getSecondsRecorded());
        assertThrows(UnsupportedOperationException.class, () -> view.getCameras().clear());
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

    private static FixedCamera fixedCamera(String id) {
        return new FixedCamera(id, "Fixed", Resolution.HD, 30, 0);
    }

    private static PtzCamera ptzCamera(String id) {
        return new PtzCamera(id, "PTZ", Resolution.FULL_HD, 30);
    }

    private static List<Integer> courtNumbers(List<CourtView> courts) {
        return courts.stream().map(CourtView::getNumber).collect(Collectors.toList());
    }

    private static List<String> cameraIds(CourtView court) {
        return court.getCameras().stream().map(CameraView::getId).collect(Collectors.toList());
    }
}
