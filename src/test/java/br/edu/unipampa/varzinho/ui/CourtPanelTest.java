package br.edu.unipampa.varzinho.ui;

import br.edu.unipampa.varzinho.application.CameraKind;
import br.edu.unipampa.varzinho.application.ManageGym;
import br.edu.unipampa.varzinho.application.port.GymRepository;
import br.edu.unipampa.varzinho.application.port.RepositoryException;
import br.edu.unipampa.varzinho.bootstrap.SampleGym;
import br.edu.unipampa.varzinho.domain.capture.Camera;
import br.edu.unipampa.varzinho.domain.capture.FixedCamera;
import br.edu.unipampa.varzinho.domain.capture.PtzCamera;
import br.edu.unipampa.varzinho.domain.highlight.Highlight;
import br.edu.unipampa.varzinho.domain.structure.Court;
import br.edu.unipampa.varzinho.domain.structure.Gym;
import br.edu.unipampa.varzinho.enums.CameraStatus;
import br.edu.unipampa.varzinho.enums.Resolution;
import br.edu.unipampa.varzinho.infrastructure.persistence.CsvGymRepository;
import br.edu.unipampa.varzinho.infrastructure.simulation.LiveFeed;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CourtPanelTest {
    private final Gym gym = SampleGym.build();
    private final List<String> errors = new ArrayList<>();
    private final CountingGymRepository gymRepository = new CountingGymRepository();
    private final CourtPanel panel = new CourtPanel(new ManageGym(gym, gymRepository), 30, errors::add);

    @Test
    void switchingCourtsShowsTheCamerasOfTheNewCourt() {
        assertEquals(2, panel.cameraLines().size());

        panel.selectCourt(2);

        assertEquals(List.of(gym.findCourt(2).getCameras().get(0).describe()), panel.cameraLines());
        assertEquals(2, panel.selectedCourtNumber());
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
        panel.typeSelector().setSelectedItem(CameraKind.FIXED);
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
        panel.typeSelector().setSelectedItem(CameraKind.PTZ);

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

        LiveFeed feed = new LiveFeed(gym);
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

        assertEquals(3, panel.selectedCourtNumber());
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
        panel.onCourtAdded(() -> announced.add(panel.selectedCourtNumber()));
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
    void aimingAFixedCameraIsRefusedEvenIfTheButtonWasLeftEnabled() {
        FixedCamera fixed = (FixedCamera) gym.findCourt(1).getCameras().get(0);
        CameraStatus statusBefore = fixed.getStatus();
        int angleBefore = fixed.getAngle();
        panel.selectCamera(0);
        // The disabled button is the screen's guarantee; this checks the one behind it.
        panel.moveButton().setEnabled(true);

        panel.moveButton().doClick();

        assertEquals(List.of("camera " + fixed.getId() + " cannot be aimed"), errors);
        assertSame(fixed, gym.findCourt(1).getCameras().get(0));
        assertEquals(statusBefore, fixed.getStatus());
        assertEquals(angleBefore, fixed.getAngle());
        assertEquals(0, gymRepository.saves);
        assertFalse(panel.moveButton().isEnabled());
    }

    @Test
    void aimingIsOnlyOfferedWhileAPtzIsSelected() {
        panel.selectCamera(0);
        assertFalse(panel.moveButton().isEnabled());

        panel.selectCamera(1);
        assertTrue(panel.moveButton().isEnabled());
    }

    @Test
    void everyChangeTheGymAcceptsIsSaved() {
        panel.courtNumberField().setText("3");
        panel.addCourtButton().doClick();
        panel.idField().setText("fixed-3");
        panel.modelField().setText("Fixed Lite");
        panel.installButton().doClick();
        panel.selectCamera(0);
        panel.startButton().doClick();
        panel.removeCameraButton().doClick();

        assertEquals(4, gymRepository.saves);
    }

    @Test
    void aRefusedChangeIsNotSaved() {
        panel.courtNumberField().setText("1");
        panel.addCourtButton().doClick();
        panel.installButton().doClick();
        panel.selectCamera(0);
        panel.maintenanceButton().doClick();
        panel.startButton().doClick();

        assertEquals(1, gymRepository.saves);
        assertEquals(3, errors.size());
    }

    @Test
    void movingTheSelectedCameraTakesItToTheChosenCourt() {
        Camera ptz = gym.findCourt(1).getCameras().get(1);

        panel.selectCamera(1);
        panel.moveTargetSelector().setSelectedItem(2);
        panel.moveToCourtButton().doClick();

        assertEquals(1, panel.cameraLines().size());
        assertTrue(gym.findCourt(2).getCameras().contains(ptz));
        assertEquals(CameraStatus.INACTIVE, ptz.getStatus());
        assertEquals(1, gymRepository.saves);
    }

    @Test
    void movingACameraToTheCourtItIsOnReportsTheDomainRefusal() {
        panel.selectCamera(0);
        panel.moveTargetSelector().setSelectedItem(1);

        panel.moveToCourtButton().doClick();

        assertEquals(List.of("camera fixed-1 is already on court 1"), errors);
    }

    @Test
    void aCourtAddedLaterCanReceiveACamera() {
        panel.courtNumberField().setText("5");
        panel.addCourtButton().doClick();
        panel.selectCourt(1);

        panel.selectCamera(0);
        panel.moveTargetSelector().setSelectedItem(5);
        panel.moveToCourtButton().doClick();

        assertEquals(1, gym.findCourt(5).getCameras().size());
    }

    @Test
    void aSaveTheStorageRefusesIsReportedWhileTheChangeStays() {
        gymRepository.failing = true;
        panel.courtNumberField().setText("3");

        panel.addCourtButton().doClick();

        assertEquals(List.of("The gym could not be saved: disk unplugged"), errors);
        assertEquals(3, panel.selectedCourtNumber());
        assertEquals(3, gym.getCourts().size());
    }

    @Test
    void aValueTheStorageCannotHoldLeavesTheCameraInstalledAndLaterCourtsListed(@TempDir Path folder) {
        CourtPanel csvPanel = new CourtPanel(
                new ManageGym(gym, new CsvGymRepository(folder.resolve("gym.csv"))), 30, errors::add);
        csvPanel.selectCourt(2);
        csvPanel.idField().setText("fixed-9");
        csvPanel.modelField().setText("Bullet, 2nd gen");
        csvPanel.typeSelector().setSelectedItem(CameraKind.FIXED);
        csvPanel.resolutionSelector().setSelectedItem(Resolution.HD);

        csvPanel.installButton().doClick();
        csvPanel.courtNumberField().setText("3");
        csvPanel.addCourtButton().doClick();

        assertEquals(2, errors.size());
        assertTrue(errors.get(0).startsWith("The gym could not be saved: "), errors.get(0));
        assertTrue(gym.findCourt(2).getCameras().stream().anyMatch(camera -> camera.getId().equals("fixed-9")));
        assertEquals(3, csvPanel.selectedCourtNumber());
        assertEquals(3, gym.getCourts().size());
    }

    private static final class CountingGymRepository implements GymRepository {
        private int saves;
        private boolean failing;

        @Override
        public void save(Gym gym) throws RepositoryException {
            if (failing) throw new RepositoryException("disk unplugged");
            saves++;
        }

        @Override
        public Optional<Gym> load() {
            return Optional.empty();
        }
    }
}
