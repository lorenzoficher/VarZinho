package br.edu.unipampa.varzinho.ui;

import br.edu.unipampa.varzinho.domain.capture.Camera;
import br.edu.unipampa.varzinho.domain.structure.Gym;
import br.edu.unipampa.varzinho.enums.CameraStatus;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CourtPanelTest {
    private final Gym gym = SampleGym.build();
    private final List<String> errors = new ArrayList<>();
    private final CourtPanel panel = new CourtPanel(gym, errors::add);

    @Test
    void switchingCourtsShowsTheCamerasOfTheNewCourt() {
        assertEquals(2, panel.cameraLines().size());

        panel.selectCourt(2);

        assertEquals(List.of(gym.findCourt(2).getCameras().get(0).describe()), panel.cameraLines());
        assertEquals(2, panel.selectedCourt().getNumber());
    }

    @Test
    void stopSwitchesOffTheSelectedCameraAndItsLineFollows() {
        Camera camera = gym.findCourt(1).getCameras().get(1);

        panel.selectCamera(1);
        panel.stopButton().doClick();

        assertEquals(CameraStatus.INACTIVE, camera.getStatus());
        assertEquals(camera.describe(), panel.cameraLines().get(1));
        assertEquals(CameraStatus.ACTIVE, gym.findCourt(1).getCameras().get(0).getStatus());
    }

    @Test
    void startingACameraInMaintenanceReportsTheDomainRefusal() {
        Camera camera = gym.findCourt(1).getCameras().get(0);
        panel.selectCamera(0);
        panel.maintenanceButton().doClick();

        panel.startButton().doClick();

        assertEquals(CameraStatus.MAINTENANCE, camera.getStatus());
        assertEquals(List.of("camera " + camera.getId() + " is under maintenance and cannot record"), errors);
    }
}
