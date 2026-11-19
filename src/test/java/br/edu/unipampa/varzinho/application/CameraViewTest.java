package br.edu.unipampa.varzinho.application;

import br.edu.unipampa.varzinho.domain.capture.FixedCamera;
import br.edu.unipampa.varzinho.domain.capture.PtzCamera;
import br.edu.unipampa.varzinho.domain.capture.StreamCamera;
import br.edu.unipampa.varzinho.domain.structure.Court;
import br.edu.unipampa.varzinho.domain.structure.Gym;
import br.edu.unipampa.varzinho.enums.CameraStatus;
import br.edu.unipampa.varzinho.enums.Resolution;
import br.edu.unipampa.varzinho.infrastructure.persistence.InMemoryGymRepository;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CameraViewTest {
    @Test
    void managementReturnsStructuredCameraFactsWithoutParsingItsDescription() {
        Gym gym = new Gym("Arena", "Rua A");
        gym.addCourt(new Court(1));
        FixedCamera camera = new FixedCamera("fixed-1", "Bullet", Resolution.FULL_HD, 30, 45);
        camera.sendToMaintenance();
        gym.installCamera(1, camera);

        CameraView view = new ManageGym(gym, new InMemoryGymRepository()).court(1).getCameras().get(0);

        assertEquals("fixed-1", view.getId());
        assertEquals(CameraKind.FIXED, view.getKind());
        assertEquals(Resolution.FULL_HD, view.getResolution());
        assertEquals(CameraStatus.MAINTENANCE, view.getStatus());
        assertFalse(view.isAimable());
        assertFalse(view.getPan().isPresent());
        assertFalse(view.getTilt().isPresent());
        assertFalse(view.getZoom().isPresent());
    }

    @Test
    void aimingAndChangingStatusDoNotMutateAnAlreadyReturnedCameraView() throws Exception {
        Gym gym = new Gym("Arena", "Rua A");
        gym.addCourt(new Court(1));
        PtzCamera camera = new PtzCamera("ptz-1", "Dome", Resolution.HD, 30);
        camera.moveTo(90, -20, 3);
        gym.installCamera(1, camera);
        ManageGym management = new ManageGym(gym, new InMemoryGymRepository());
        CameraView before = management.court(1).getCameras().get(0);

        management.aimCamera(1, "ptz-1", 180, 10, 5);
        CameraView after = management.startRecording(1, "ptz-1").getCameras().get(0);

        assertEquals(CameraKind.PTZ, before.getKind());
        assertTrue(before.isAimable());
        assertEquals(Resolution.HD, before.getResolution());
        assertEquals(90, before.getPan().orElseThrow());
        assertEquals(-20, before.getTilt().orElseThrow());
        assertEquals(3, before.getZoom().orElseThrow());
        assertEquals(CameraStatus.INACTIVE, before.getStatus());
        assertEquals(180, after.getPan().orElseThrow());
        assertEquals(10, after.getTilt().orElseThrow());
        assertEquals(5, after.getZoom().orElseThrow());
        assertEquals(CameraStatus.ACTIVE, after.getStatus());
    }

    @Test
    void aPhoneHasItsOwnKindAndReportsEachStatusWithoutPretendingToBePtz() throws Exception {
        Gym gym = new Gym("Arena", "Rua A");
        StreamCamera camera = new StreamCamera("phone-1", "Phone", Resolution.HD, 30,
                "http://phone/video", (window, target) -> { });
        ManageGym management = new ManageGym(gym, new InMemoryGymRepository());

        CourtView connected = new ConnectStreamCamera(gym, camera).connect();
        CameraView active = management.court(connected.getNumber()).getCameras().get(0);

        assertEquals("phone-1", active.getId());
        assertEquals(CameraKind.STREAM, active.getKind());
        assertEquals(Resolution.HD, active.getResolution());
        assertEquals(CameraStatus.ACTIVE, active.getStatus());
        assertFalse(active.isAimable());
        assertFalse(active.getPan().isPresent());
        assertFalse(active.getTilt().isPresent());
        assertFalse(active.getZoom().isPresent());
        CameraView stopped = management.stopRecording(connected.getNumber(), "phone-1").getCameras().get(0);
        CameraView maintained = management.sendToMaintenance(connected.getNumber(), "phone-1").getCameras().get(0);
        assertEquals(CameraStatus.INACTIVE, stopped.getStatus());
        assertEquals(CameraStatus.MAINTENANCE, maintained.getStatus());
        assertEquals(CameraStatus.ACTIVE, active.getStatus());
    }
}
