package br.edu.unipampa.varzinho.application;

import br.edu.unipampa.varzinho.domain.capture.FixedCamera;
import br.edu.unipampa.varzinho.domain.capture.PtzCamera;
import br.edu.unipampa.varzinho.domain.capture.StreamCamera;
import br.edu.unipampa.varzinho.domain.structure.Court;
import br.edu.unipampa.varzinho.domain.structure.Gym;
import br.edu.unipampa.varzinho.enums.CameraStatus;
import br.edu.unipampa.varzinho.enums.Resolution;
import br.edu.unipampa.varzinho.infrastructure.persistence.InMemoryGymRepository;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CourtViewTest {
    @Test
    void keepsTheNameAndSportTheCourtHadWhenTheSnapshotWasTaken() throws Exception {
        Gym gym = new Gym("Arena", "Rua A");
        gym.addCourt(new Court(1));
        ManageGym management = new ManageGym(gym, new InMemoryGymRepository());
        CourtView named = management.renameCourt(1, "Quadra coberta");
        CourtView withSport = management.chooseSport(1, "Futsal");

        management.renameCourt(1, "");

        assertEquals(Optional.of("Quadra coberta"), named.getDisplayName());
        assertEquals(Optional.empty(), named.getSport());
        assertEquals(Optional.of("Futsal"), withSport.getSport());
        assertEquals(Optional.empty(), management.court(1).getDisplayName());
    }

    @Test
    void countsAnEmptyCourtAndKeepsTheOldSnapshotWhenACameraIsInstalled() throws Exception {
        Gym gym = new Gym("Arena", "Rua A");
        gym.addCourt(new Court(1));
        ManageGym management = new ManageGym(gym, new InMemoryGymRepository());
        CourtView empty = management.court(1);

        CourtView installed = management.installCamera(1,
                new CameraSpec("fixed-1", "Bullet", CameraKind.FIXED, Resolution.HD, 30, 0));

        assertEquals(0, empty.getCameraCount());
        assertEquals(0, empty.getRecordingCameraCount());
        assertEquals(1, installed.getCameraCount());
        assertEquals(0, installed.getRecordingCameraCount());
        assertThrows(UnsupportedOperationException.class, () -> installed.getCameras().clear());
    }

    @Test
    void countsOnlyRecordingCamerasIncludingThePhoneAndKeepsOldCountsAfterChanges() throws Exception {
        Gym gym = new Gym("Arena", "Rua A");
        gym.addCourt(new Court(1));
        gym.installCamera(1, new FixedCamera("fixed-1", "Bullet", Resolution.HD, 30, 0));
        PtzCamera ptz = new PtzCamera("ptz-1", "Dome", Resolution.HD, 30);
        ptz.sendToMaintenance();
        gym.installCamera(1, ptz);
        StreamCamera phone = new StreamCamera("phone-1", "Phone", Resolution.HD, 30,
                "http://phone/video", (window, target) -> { });
        phone.startRecording();
        gym.installCamera(1, phone);
        ManageGym management = new ManageGym(gym, new InMemoryGymRepository());
        CourtView before = management.courts().get(0);

        CourtView started = management.startRecording(1, "fixed-1");
        CourtView removed = management.removeCamera(1, "phone-1");

        assertEquals(3, before.getCameraCount());
        assertEquals(1, before.getRecordingCameraCount());
        assertEquals(CameraStatus.INACTIVE, before.getCameras().get(0).getStatus());
        assertEquals(3, started.getCameraCount());
        assertEquals(2, started.getRecordingCameraCount());
        assertEquals(2, removed.getCameraCount());
        assertEquals(1, removed.getRecordingCameraCount());
    }
}
