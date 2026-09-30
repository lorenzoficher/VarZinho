package br.edu.unipampa.varzinho.ui;

import br.edu.unipampa.varzinho.domain.capture.Camera;
import br.edu.unipampa.varzinho.domain.capture.FixedCamera;
import br.edu.unipampa.varzinho.domain.capture.PtzCamera;
import br.edu.unipampa.varzinho.domain.highlight.Highlight;
import br.edu.unipampa.varzinho.domain.structure.Court;
import br.edu.unipampa.varzinho.domain.structure.Gym;
import br.edu.unipampa.varzinho.enums.CameraStatus;
import br.edu.unipampa.varzinho.enums.Resolution;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CourtPanelTest {
    private final Gym gym = SampleGym.build();
    private final List<String> errors = new ArrayList<>();
    private final CourtPanel panel = new CourtPanel(gym, 30, errors::add);

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

    @Test
    void installingAFixedCameraAddsItToTheSelectedCourtAsInactive() {
        panel.selectCourt(2);
        panel.idField().setText("fixed-3");
        panel.modelField().setText("Fixed Lite");
        panel.typeSelector().setSelectedItem(CourtPanel.FIXED);
        panel.resolutionSelector().setSelectedItem(Resolution.HD);

        panel.installButton().doClick();

        List<Camera> cameras = gym.findCourt(2).getCameras();
        assertEquals(2, cameras.size());
        Camera installed = cameras.get(1);
        assertInstanceOf(FixedCamera.class, installed);
        assertEquals("fixed-3", installed.getId());
        assertEquals(CameraStatus.INACTIVE, installed.getStatus());
        assertEquals(installed.describe(), panel.cameraLines().get(1));
        assertTrue(errors.isEmpty());
    }

    @Test
    void installingAPtzCameraGivesTheCourtOneMoreCamera() {
        panel.idField().setText("ptz-2");
        panel.modelField().setText("PTZ Lite");
        panel.typeSelector().setSelectedItem(CourtPanel.PTZ);

        panel.installButton().doClick();

        List<Camera> cameras = gym.findCourt(1).getCameras();
        assertEquals(3, cameras.size());
        assertInstanceOf(PtzCamera.class, cameras.get(2));
        assertEquals(3, panel.cameraLines().size());
    }

    @Test
    void aCameraWithoutAnIdIsRefusedWithTheDomainMessageAndNothingIsInstalled() {
        panel.idField().setText(" ");
        panel.modelField().setText("Fixed Lite");

        panel.installButton().doClick();

        assertEquals(List.of("a camera needs an identifier"), errors);
        assertEquals(2, gym.findCourt(1).getCameras().size());
        assertEquals("Fixed Lite", panel.modelField().getText());
    }

    @Test
    void installingAnIdAnotherCourtAlreadyUsesReportsTheDomainRefusal() {
        panel.selectCourt(2);
        panel.idField().setText("fixed-1");
        panel.modelField().setText("Fixed Lite");

        panel.installButton().doClick();

        assertEquals(List.of("camera fixed-1 is already installed on court 1"), errors);
        assertEquals(1, gym.findCourt(2).getCameras().size());
    }

    @Test
    void aCameraInstalledFromTheWindowRecordsAndIsCreditedWithTheHighlight() {
        panel.selectCourt(2);
        panel.selectCamera(0);
        panel.stopButton().doClick();
        panel.idField().setText("fixed-3");
        panel.modelField().setText("Fixed Lite");
        panel.installButton().doClick();
        panel.selectCamera(1);
        panel.startButton().doClick();

        LiveFeed feed = new LiveFeed(gym, 30);
        Instant start = Instant.parse("2026-09-30T12:00:00Z");
        for (int second = 0; second < Court.DEFAULT_CAPTURE_SECONDS; second++) {
            feed.tick(start.plusSeconds(second));
        }
        Highlight highlight = gym.findCourt(2).triggerCapture();

        assertEquals("fixed-3", highlight.getCameraId());
    }

    @Test
    void addingACourtListsItAndSelectsIt() {
        panel.courtNumberField().setText("3");

        panel.addCourtButton().doClick();

        assertEquals(3, panel.selectedCourt().getNumber());
        assertTrue(panel.cameraLines().isEmpty());
        assertEquals(3, gym.getCourts().size());
        assertTrue(errors.isEmpty());
    }

    @Test
    void aRepeatedCourtNumberIsRefusedWithTheDomainMessage() {
        panel.courtNumberField().setText("2");

        panel.addCourtButton().doClick();

        assertEquals(List.of("court number is already registered"), errors);
        assertEquals(2, gym.getCourts().size());
    }

    @Test
    void aCourtNumberBelowOneIsRefusedWithTheDomainMessage() {
        panel.courtNumberField().setText("0");

        panel.addCourtButton().doClick();

        assertEquals(List.of("a court number must be positive"), errors);
    }

    @Test
    void textThatIsNotANumberAsksForOne() {
        panel.courtNumberField().setText("three");

        panel.addCourtButton().doClick();

        assertEquals(List.of("Type the court number as digits."), errors);
        assertEquals(2, gym.getCourts().size());
    }

    @Test
    void addingACourtTellsWhoeverListensForNewCourts() {
        List<Integer> announced = new ArrayList<>();
        panel.onCourtAdded(() -> announced.add(panel.selectedCourt().getNumber()));
        panel.courtNumberField().setText("4");

        panel.addCourtButton().doClick();

        assertEquals(List.of(4), announced);
    }

    @Test
    void removingTheSelectedCameraTakesItOffTheCourt() {
        Camera kept = gym.findCourt(1).getCameras().get(0);

        panel.selectCamera(1);
        panel.removeCameraButton().doClick();

        assertEquals(List.of(kept), gym.findCourt(1).getCameras());
        assertEquals(List.of(kept.describe()), panel.cameraLines());
    }

    @Test
    void removingWithNoCameraSelectedAsksForOneAndRemovesNothing() {
        panel.removeCameraButton().doClick();

        assertEquals(List.of("Select a camera first."), errors);
        assertEquals(2, gym.findCourt(1).getCameras().size());
    }

    @Test
    void movingTheSelectedPtzAimsItAndItsLineFollows() {
        PtzCamera ptz = (PtzCamera) gym.findCourt(1).getCameras().get(1);
        panel.selectCamera(1);
        panel.panSpinner().setValue(90);
        panel.tiltSpinner().setValue(-10);
        panel.zoomSpinner().setValue(3);

        panel.moveButton().doClick();

        assertEquals(90, ptz.getPan());
        assertEquals(-10, ptz.getTilt());
        assertEquals(3, ptz.getZoom());
        assertEquals(ptz.describe(), panel.cameraLines().get(1));
    }

    @Test
    void aimingPastTheHeadsReachIsRefusedWithTheDomainMessage() {
        PtzCamera ptz = (PtzCamera) gym.findCourt(1).getCameras().get(1);
        panel.selectCamera(1);
        panel.panSpinner().setValue(400);

        panel.moveButton().doClick();

        assertEquals(List.of("pan runs from 0 to 359, not 400"), errors);
        assertEquals(0, ptz.getPan());
    }

    @Test
    void aimingIsOnlyOfferedWhileAPtzIsSelected() {
        panel.selectCamera(0);
        assertFalse(panel.moveButton().isEnabled());

        panel.selectCamera(1);
        assertTrue(panel.moveButton().isEnabled());
    }

    @Test
    void everyChangeToTheGymIsAnnounced() {
        List<String> changes = new ArrayList<>();
        panel.onGymChanged(() -> changes.add("changed"));

        panel.courtNumberField().setText("3");
        panel.addCourtButton().doClick();
        panel.idField().setText("fixed-3");
        panel.modelField().setText("Fixed Lite");
        panel.installButton().doClick();
        panel.selectCamera(0);
        panel.startButton().doClick();
        panel.removeCameraButton().doClick();

        assertEquals(4, changes.size());
    }

    @Test
    void aRefusedChangeIsNotAnnounced() {
        List<String> changes = new ArrayList<>();
        panel.onGymChanged(() -> changes.add("changed"));

        panel.courtNumberField().setText("1");
        panel.addCourtButton().doClick();
        panel.installButton().doClick();
        panel.selectCamera(0);
        panel.maintenanceButton().doClick();
        panel.startButton().doClick();

        assertEquals(1, changes.size());
        assertEquals(3, errors.size());
    }
}
