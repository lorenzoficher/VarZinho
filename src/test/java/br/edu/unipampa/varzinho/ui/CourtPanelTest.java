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

import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import java.awt.Component;
import java.awt.event.KeyEvent;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.awt.KeyboardFocusManager;
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
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static br.edu.unipampa.varzinho.ui.SwingWork.awaitRepositoryWork;

class CourtPanelTest {
    private final Gym gym = SampleGym.build();
    private final List<String> errors = new ArrayList<>();
    private final GatedGymRepository gymRepository = new GatedGymRepository();
    private final CourtPanel panel = new CourtPanel(new ManageGym(gym, gymRepository), 30, errors::add);

    @Test
    void captureShieldsInputWithoutDisablingAndRepaintingTheWindow() {
        var root = new javax.swing.JRootPane();
        root.setContentPane(panel);
        var original = root.getGlassPane();
        panel.pauseChanges();
        try {
            assertTrue(root.getGlassPane().isVisible());
            assertTrue(panel.installButton().isEnabled());
            assertTrue(panel.isEnabled());
        } finally {
            panel.resumeChanges();
        }
        assertEquals(original, root.getGlassPane());
        assertFalse(root.getGlassPane().isVisible());
    }

    @Test
    void manualInstallationOffersOnlyKindsThatNeedNoStreamConnection() {
        assertEquals(2, panel.typeSelector().getItemCount());
        assertEquals(CameraKind.FIXED, panel.typeSelector().getItemAt(0));
        assertEquals(CameraKind.PTZ, panel.typeSelector().getItemAt(1));
    }

    @Test
    void composesOnePanelForEachCourtAreaResponsibility() {
        assertEquals(1, descendantsOfType(panel, CourtSelectionPanel.class));
        assertEquals(1, descendantsOfType(panel, CameraListPanel.class));
        assertEquals(1, descendantsOfType(panel, CameraInstallPanel.class));
        assertEquals(1, descendantsOfType(panel, CameraPositionPanel.class));
    }

    @Test
    void switchingCourtsShowsTheCamerasOfTheNewCourt() {
        assertEquals(2, panel.cameraLines().size());

        panel.selectCourt(2);

        assertEquals(List.of("fixed-2 · Fixa · Full HD (1920x1080) · Ativa"), panel.cameraLines());
        assertEquals(2, panel.selectedCourtNumber());
    }

    @Test
    void showsTheCamerasAsATableOfCameraKindResolutionAndStatus() {
        assertEquals(List.of("Câmera", "Tipo", "Resolução", "Status"), panel.cameraColumns());
        assertEquals(List.of("ptz-1", "PTZ", "HD (1280x720)", "Gravando"), panel.cameraRow(1));
    }

    @Test
    void eachStatusShowsItsOwnBadge() {
        panel.selectCamera(1);
        press(panel.stopButton());
        assertEquals(List.of(StatusBadge.RECORDING, StatusBadge.STOPPED), panel.statusBadges());

        panel.selectCamera(0);
        press(panel.maintenanceButton());
        assertEquals(List.of(StatusBadge.MAINTENANCE, StatusBadge.STOPPED), panel.statusBadges());

        panel.selectCamera(1);
        press(panel.startButton());
        assertEquals(List.of(StatusBadge.MAINTENANCE, StatusBadge.RECORDING), panel.statusBadges());
    }

    @Test
    void theBadgeComesFromTheStatusAloneForEveryStatus() {
        assertEquals(StatusBadge.RECORDING, StatusBadge.of(CameraStatus.ACTIVE));
        assertEquals(StatusBadge.STOPPED, StatusBadge.of(CameraStatus.INACTIVE));
        assertEquals(StatusBadge.MAINTENANCE, StatusBadge.of(CameraStatus.MAINTENANCE));
    }

    @Test
    void theCameraTableLeavesF2ToTheSaveShortcut() {
        JTable table = descendantOfType(panel.courtPage(), JTable.class);

        Object binding = table.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .get(KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0));

        assertTrue(binding == null || "none".equals(binding), String.valueOf(binding));
    }

    @Test
    void tabLeavesTheCameraTableInsteadOfWalkingItsCells() {
        assertTabLeaves(descendantOfType(panel.courtPage(), JTable.class));
    }

    static void assertTabLeaves(JTable table) {
        assertTrue(table.getFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS)
                .contains(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0)), "Tab stays inside the table");
        assertTrue(table.getFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS)
                .contains(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, KeyEvent.SHIFT_DOWN_MASK)),
                "Shift+Tab stays inside the table");
    }

    @Test
    void changingACameraTellsWhoeverFollowsTheCameras() {
        List<String> changes = new ArrayList<>();
        panel.onCamerasChanged(() -> changes.add("changed"));

        panel.selectCamera(0);
        press(panel.stopButton());
        press(panel.startButton());
        press(panel.maintenanceButton());

        assertEquals(3, changes.size());
    }

    @Test
    void stopSwitchesOffTheSelectedCameraAndItsLineFollows() {
        Camera camera = gym.findCourt(1).orElseThrow().getCameras().get(1);

        panel.selectCamera(1);
        press(panel.stopButton());

        assertEquals(CameraStatus.INACTIVE, camera.getStatus());
        assertEquals("ptz-1 · PTZ · HD (1280x720) · Parada · panorâmica 0 inclinação 0 zoom 1", panel.cameraLines().get(1));
        assertEquals(CameraStatus.ACTIVE, gym.findCourt(1).orElseThrow().getCameras().get(0).getStatus());
    }

    @Test
    void startingACameraInMaintenanceReportsTheDomainRefusal() {
        Camera camera = gym.findCourt(1).orElseThrow().getCameras().get(0);
        panel.selectCamera(0);
        press(panel.maintenanceButton());

        press(panel.startButton());

        assertEquals(CameraStatus.MAINTENANCE, camera.getStatus());
        assertEquals(List.of("A câmera " + camera.getId() + " está em manutenção e não pode gravar."), errors);
    }

    @Test
    void courtsPageRegistersTwoCamerasInSequenceAndPreservesThemOnDisk(@TempDir Path directory)
            throws RepositoryException {
        Path store = directory.resolve("gym.csv");
        CourtPanel courts = new CourtPanel(new ManageGym(gym, new CsvGymRepository(store)), 30, errors::add);
        var page = courts.courtPage();
        courts.cameraPage();
        assertTrue(javax.swing.SwingUtilities.isDescendingFrom(courts.installButton(), page));
        courts.courtNumberField().setText("3");
        press(courts.addCourtButton());
        courts.idField().setText("gol-norte");
        courts.modelField().setText("Modelo A");
        press(courts.installButton());
        assertEquals("", courts.idField().getText());
        assertEquals("Modelo A", courts.modelField().getText());
        courts.idField().setText("gol-norte");
        press(courts.installButton());
        assertEquals("gol-norte", courts.idField().getText());
        assertEquals(1, errors.size());
        courts.idField().setText("gol-sul");
        courts.typeSelector().setSelectedItem(CameraKind.PTZ);
        press(courts.installButton());
        var saved = new CsvGymRepository(store).load().orElseThrow();
        assertEquals(List.of("gol-norte", "gol-sul"),
                saved.findCourt(3).orElseThrow().getCameras().stream().map(Camera::getId).toList());
        assertEquals(3, courts.selectedCourtNumber());
        assertEquals(2, saved.findCourt(1).orElseThrow().getCameras().size());
    }

    @Test
    void installingAFixedCameraAddsItToTheSelectedCourtAsInactive() {
        panel.selectCourt(2);
        panel.idField().setText("fixed-3");
        panel.modelField().setText("Fixed Lite");
        panel.typeSelector().setSelectedItem(CameraKind.FIXED);
        panel.resolutionSelector().setSelectedItem(Resolution.HD);

        press(panel.installButton());

        List<Camera> cameras = gym.findCourt(2).orElseThrow().getCameras();
        assertEquals(2, cameras.size());
        Camera installed = cameras.get(1);
        assertInstanceOf(FixedCamera.class, installed);
        assertEquals("fixed-3", installed.getId());
        assertEquals(CameraStatus.INACTIVE, installed.getStatus());
        assertEquals("fixed-3 · Fixa · HD (1280x720) · Parada", panel.cameraLines().get(1));
        assertTrue(errors.isEmpty());
    }

    @Test
    void aCourtsNameAndSportAreStoredInTheGymNotKeptByThePanel() {
        List<String> saved = new ArrayList<>();
        panel.selectCourt(2);

        panel.renameSelected(panel.addCourtButton(), " Quadra Azul ", () -> saved.add("name"));
        awaitRepositoryWork();
        panel.chooseSport(panel.addCourtButton(), "Vôlei", () -> saved.add("sport"));
        awaitRepositoryWork();

        Court court = gym.findCourt(2).orElseThrow();
        assertEquals(java.util.Optional.of("Quadra Azul"), court.getDisplayName());
        assertEquals(java.util.Optional.of("Vôlei"), court.getSport());
        assertEquals("Quadra Azul", panel.nameOf(2));
        assertEquals("Vôlei", panel.sportOf(2));
        assertEquals(List.of("name", "sport"), saved);
        assertEquals(2, gymRepository.saves());
    }

    @Test
    void aNameTheStorageCouldNotKeepIsReportedAndNotShown() {
        List<String> saved = new ArrayList<>();
        gymRepository.failFromNowOn();

        panel.renameSelected(panel.addCourtButton(), "Quadra Azul", () -> saved.add("name"));
        awaitRepositoryWork();

        assertEquals("Quadra " + panel.selectedCourtNumber(), panel.nameOf(panel.selectedCourtNumber()));
        assertTrue(saved.isEmpty());
        assertEquals(1, errors.size());
    }

    @Test
    void aSportOutsideTheDashboardChoicesStillShowsOnTheCourtCard() {
        gym.findCourt(1).orElseThrow().assignSport("Futsal");

        panel.refreshCourtOverview();

        assertEquals("Futsal", panel.sportOf(1));
        assertTrue(errors.isEmpty());
    }

    @Test
    void enterInTheAngleInstallsTheCamera() {
        panel.selectCourt(2);
        panel.idField().setText("fixed-3");
        panel.modelField().setText("Fixed Lite");
        panel.typeSelector().setSelectedItem(CameraKind.FIXED);

        ((JSpinner.DefaultEditor) panel.angleSpinner().getEditor()).getTextField().postActionEvent();
        awaitRepositoryWork();

        assertEquals("fixed-3", gym.findCourt(2).orElseThrow().getCameras().get(1).getId());
    }

    @Test
    void installingAPtzCameraGivesTheCourtOneMoreCamera() {
        panel.idField().setText("ptz-2");
        panel.modelField().setText("PTZ Lite");
        panel.typeSelector().setSelectedItem(CameraKind.PTZ);

        press(panel.installButton());

        List<Camera> cameras = gym.findCourt(1).orElseThrow().getCameras();
        assertEquals(3, cameras.size());
        assertInstanceOf(PtzCamera.class, cameras.get(2));
        assertEquals(3, panel.cameraLines().size());
    }

    @Test
    void aCameraWithoutAnIdIsRefusedWithTheDomainMessageAndNothingIsInstalled() {
        panel.idField().setText(" ");
        panel.modelField().setText("Fixed Lite");

        press(panel.installButton());

        assertEquals(List.of("A câmera precisa de um identificador."), errors);
        assertEquals(2, gym.findCourt(1).orElseThrow().getCameras().size());
        assertEquals("Fixed Lite", panel.modelField().getText());
    }

    @Test
    void installingAnIdAnotherCourtAlreadyUsesReportsTheDomainRefusal() {
        panel.selectCourt(2);
        panel.idField().setText("fixed-1");
        panel.modelField().setText("Fixed Lite");

        press(panel.installButton());

        assertEquals(List.of("A câmera fixed-1 já está instalada na quadra 1."), errors);
        assertEquals(1, gym.findCourt(2).orElseThrow().getCameras().size());
    }

    @Test
    void aCameraInstalledFromTheWindowRecordsAndIsCreditedWithTheHighlight() {
        panel.selectCourt(2);
        panel.selectCamera(0);
        press(panel.stopButton());
        panel.idField().setText("fixed-3");
        panel.modelField().setText("Fixed Lite");
        press(panel.installButton());
        panel.selectCamera(1);
        press(panel.startButton());

        LiveFeed feed = new LiveFeed(gym);
        Instant start = Instant.parse("2026-09-30T12:00:00Z");
        for (int second = 0; second < Court.DEFAULT_CAPTURE_SECONDS; second++) {
            feed.tick(start.plusSeconds(second));
        }
        Highlight highlight = gym.findCourt(2).orElseThrow().triggerCapture();

        assertEquals("fixed-3", highlight.getCameraId());
    }

    @Test
    void addingACourtListsItAndSelectsIt() {
        panel.courtNumberField().setText("3");

        press(panel.addCourtButton());

        assertEquals(3, panel.selectedCourtNumber());
        assertTrue(panel.cameraLines().isEmpty());
        assertEquals(3, gym.getCourts().size());
        assertTrue(errors.isEmpty());
    }

    @Test
    void enterInTheCourtNumberAddsTheCourt() {
        panel.courtNumberField().setText("3");

        panel.courtNumberField().postActionEvent();
        awaitRepositoryWork();

        assertEquals(3, panel.selectedCourtNumber());
        assertTrue(errors.isEmpty());
    }

    @Test
    void aRepeatedCourtNumberIsRefusedWithTheDomainMessage() {
        panel.courtNumberField().setText("2");

        press(panel.addCourtButton());

        assertEquals(List.of("O número da quadra já está cadastrado."), errors);
        assertEquals(2, gym.getCourts().size());
    }

    @Test
    void aCourtNumberBelowOneIsRefusedWithTheDomainMessage() {
        panel.courtNumberField().setText("0");

        press(panel.addCourtButton());

        assertEquals(List.of("O número da quadra deve ser positivo."), errors);
    }

    @Test
    void textThatIsNotANumberAsksForOne() {
        panel.courtNumberField().setText("three");

        press(panel.addCourtButton());

        assertEquals(List.of("Digite o número da quadra usando algarismos."), errors);
        assertEquals(2, gym.getCourts().size());
    }

    @Test
    void addingACourtTellsWhoeverListensForNewCourts() {
        List<Integer> announced = new ArrayList<>();
        panel.onCourtAdded(() -> announced.add(panel.selectedCourtNumber()));
        panel.courtNumberField().setText("4");

        press(panel.addCourtButton());

        assertEquals(List.of(4), announced);
    }

    @Test
    void removingAnEmptyCourtDropsItFromEverySelectorAndSelectsAnother() {
        panel.courtNumberField().setText("3");
        press(panel.addCourtButton());

        panel.removeSelectedCourt(number -> true);
        awaitRepositoryWork();

        assertTrue(gym.findCourt(3).isEmpty());
        assertFalse(panel.courtNumbers().contains(3));
        assertFalse(panel.moveTargets().contains(3));
        assertTrue(gym.findCourt(panel.selectedCourtNumber()).isPresent());
        assertTrue(errors.isEmpty());
    }

    @Test
    void aDeclinedConfirmationKeepsTheCourt() {
        panel.courtNumberField().setText("3");
        press(panel.addCourtButton());

        panel.removeSelectedCourt(number -> false);
        awaitRepositoryWork();

        assertEquals(3, panel.selectedCourtNumber());
        assertEquals(3, gym.getCourts().size());
    }

    @Test
    void aCourtWithCamerasIsRefusedInPortuguese() {
        panel.selectCourt(1);

        panel.removeSelectedCourt(number -> true);
        awaitRepositoryWork();

        assertEquals(List.of("A quadra 1 ainda tem câmeras. Remova ou mova as câmeras antes de removê-la."), errors);
        assertEquals(2, gym.getCourts().size());
    }

    @Test
    void removingACourtTellsWhoeverListensForCourtChanges() {
        List<Integer> announced = new ArrayList<>();
        panel.courtNumberField().setText("4");
        press(panel.addCourtButton());
        panel.onCourtRemoved(() -> announced.add(gym.getCourts().size()));

        panel.removeSelectedCourt(number -> true);
        awaitRepositoryWork();

        assertEquals(List.of(2), announced);
    }

    @Test
    void removingTheSelectedCameraTakesItOffTheCourt() {
        Camera kept = gym.findCourt(1).orElseThrow().getCameras().get(0);

        panel.selectCamera(1);
        press(panel.removeCameraButton());

        assertEquals(List.of(kept), gym.findCourt(1).orElseThrow().getCameras());
        assertEquals(List.of("fixed-1 · Fixa · Full HD (1920x1080) · Ativa"), panel.cameraLines());
    }

    @Test
    void removingWithNoCameraSelectedAsksForOneAndRemovesNothing() {
        press(panel.removeCameraButton());

        assertEquals(List.of("Selecione uma câmera primeiro."), errors);
        assertEquals(2, gym.findCourt(1).orElseThrow().getCameras().size());
    }

    @Test
    void movingTheSelectedPtzAimsItAndItsLineFollows() {
        PtzCamera ptz = (PtzCamera) gym.findCourt(1).orElseThrow().getCameras().get(1);
        panel.selectCamera(1);
        panel.panSpinner().setValue(90);
        panel.tiltSpinner().setValue(-10);
        panel.zoomSpinner().setValue(3);

        press(panel.moveButton());

        assertEquals(90, ptz.getPan());
        assertEquals(-10, ptz.getTilt());
        assertEquals(3, ptz.getZoom());
        assertEquals("ptz-1 · PTZ · HD (1280x720) · Ativa · panorâmica 90 inclinação -10 zoom 3", panel.cameraLines().get(1));
    }

    @Test
    void enterInAnAimValueMovesTheSelectedPtz() {
        PtzCamera ptz = (PtzCamera) gym.findCourt(1).orElseThrow().getCameras().get(1);
        panel.selectCamera(1);
        panel.panSpinner().setValue(90);

        ((JSpinner.DefaultEditor) panel.panSpinner().getEditor()).getTextField().postActionEvent();
        awaitRepositoryWork();

        assertEquals(90, ptz.getPan());
    }

    @Test
    void theCameraDialogsCloseWithEscape() {
        assumeFalse(GraphicsEnvironment.isHeadless(), "a dialog needs a window system");
        panel.selectCamera(1);
        JDialog configure = panel.configureDialog(panel.selectedCamera().orElseThrow());
        JDialog install = panel.installDialog();

        assertEquals(null, configure.getRootPane().getDefaultButton(),
                "two forms share this dialog, so Enter must stay with each form's own fields");
        assertEquals("Cadastrar câmera", install.getRootPane().getDefaultButton().getText());
        EscapeKeyTest.pressEscape(configure);
        EscapeKeyTest.pressEscape(install);
        assertFalse(configure.isDisplayable());
        assertFalse(install.isDisplayable());
    }

    @Test
    void aimingPastTheHeadsReachIsRefusedWithTheDomainMessage() {
        PtzCamera ptz = (PtzCamera) gym.findCourt(1).orElseThrow().getCameras().get(1);
        panel.selectCamera(1);
        panel.panSpinner().setValue(400);

        press(panel.moveButton());

        assertEquals(List.of("A panorâmica deve estar entre 0 e 359. Valor informado: 400."), errors);
        assertEquals(0, ptz.getPan());
    }

    @Test
    void aimingAFixedCameraIsRefusedEvenIfTheButtonWasLeftEnabled() {
        FixedCamera fixed = (FixedCamera) gym.findCourt(1).orElseThrow().getCameras().get(0);
        CameraStatus statusBefore = fixed.getStatus();
        int angleBefore = fixed.getAngle();
        panel.selectCamera(0);
        // The disabled button is the screen's guarantee; this checks the one behind it.
        panel.moveButton().setEnabled(true);

        press(panel.moveButton());

        assertEquals(List.of("A câmera " + fixed.getId() + " não pode ser apontada."), errors);
        assertSame(fixed, gym.findCourt(1).orElseThrow().getCameras().get(0));
        assertEquals(statusBefore, fixed.getStatus());
        assertEquals(angleBefore, fixed.getAngle());
        assertEquals(0, gymRepository.saves());
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
        press(panel.addCourtButton());
        panel.idField().setText("fixed-3");
        panel.modelField().setText("Fixed Lite");
        press(panel.installButton());
        panel.selectCamera(0);
        press(panel.startButton());
        press(panel.removeCameraButton());

        assertEquals(4, gymRepository.saves());
    }

    @Test
    void aRefusedChangeIsNotSaved() {
        panel.courtNumberField().setText("1");
        press(panel.addCourtButton());
        press(panel.installButton());
        panel.selectCamera(0);
        press(panel.maintenanceButton());
        press(panel.startButton());

        assertEquals(1, gymRepository.saves());
        assertEquals(3, errors.size());
    }

    @Test
    void movingTheSelectedCameraTakesItToTheChosenCourt() {
        Camera ptz = gym.findCourt(1).orElseThrow().getCameras().get(1);

        panel.selectCamera(1);
        panel.moveTargetSelector().setSelectedItem(2);
        press(panel.moveToCourtButton());

        assertEquals(1, panel.cameraLines().size());
        assertTrue(gym.findCourt(2).orElseThrow().getCameras().contains(ptz));
        assertEquals(CameraStatus.INACTIVE, ptz.getStatus());
        assertEquals(1, gymRepository.saves());
    }

    @Test
    void movingACameraToTheCourtItIsOnReportsTheDomainRefusal() {
        panel.selectCamera(0);
        panel.moveTargetSelector().setSelectedItem(1);

        press(panel.moveToCourtButton());

        assertEquals(List.of("A câmera fixed-1 já está na quadra 1."), errors);
    }

    @Test
    void aCourtAddedLaterCanReceiveACamera() {
        panel.courtNumberField().setText("5");
        press(panel.addCourtButton());
        panel.selectCourt(1);

        panel.selectCamera(0);
        panel.moveTargetSelector().setSelectedItem(5);
        press(panel.moveToCourtButton());

        assertEquals(1, gym.findCourt(5).orElseThrow().getCameras().size());
    }

    @Test
    void aSaveTheStorageRefusesIsReportedAndTheCourtIsNotAdded() {
        gymRepository.failFromNowOn();
        int selected = panel.selectedCourtNumber();
        panel.courtNumberField().setText("3");

        press(panel.addCourtButton());

        assertEquals(List.of(GymChanges.NOT_SAVED), errors);
        assertEquals(selected, panel.selectedCourtNumber());
        assertFalse(panel.courtNumbers().contains(3));
        assertEquals(2, gym.getCourts().size());
    }

    @Test
    void aCameraTheStorageCannotHoldIsNotInstalledAndLaterChangesStillSave(@TempDir Path folder) {
        CourtPanel csvPanel = new CourtPanel(
                new ManageGym(gym, new CsvGymRepository(folder.resolve("gym.csv"))), 30, errors::add);
        csvPanel.selectCourt(2);
        csvPanel.idField().setText("fixed-9");
        csvPanel.modelField().setText("Bullet, 2nd gen");
        csvPanel.typeSelector().setSelectedItem(CameraKind.FIXED);
        csvPanel.resolutionSelector().setSelectedItem(Resolution.HD);

        press(csvPanel.installButton());
        csvPanel.courtNumberField().setText("3");
        press(csvPanel.addCourtButton());

        assertEquals(List.of(GymChanges.NOT_SAVED), errors);
        assertFalse(gym.findCourt(2).orElseThrow().getCameras().stream().anyMatch(camera -> camera.getId().equals("fixed-9")));
        assertEquals(3, csvPanel.selectedCourtNumber());
        assertEquals(3, gym.getCourts().size());
    }

    private static <T extends Component> T descendantOfType(Container root, Class<T> type) {
        for (Component component : root.getComponents()) {
            if (type.isInstance(component)) return type.cast(component);
            if (component instanceof Container child) {
                T found = descendantOfType(child, type);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static int descendantsOfType(Container root, Class<? extends Component> type) {
        int matches = 0;
        for (Component component : root.getComponents()) {
            if (type.isInstance(component)) matches++;
            if (component instanceof Container child) matches += descendantsOfType(child, type);
        }
        return matches;
    }

    @Test
    void installingACameraStoresTheGymOffTheSwingThread() throws Exception {
        panel.idField().setText("gol-leste");
        panel.modelField().setText("Modelo A");

        assertStoresOffSwingWhileDisabled(panel.installButton(), panel.installButton()::doClick);

        assertEquals(3, gym.findCourt(1).orElseThrow().getCameras().size());
    }

    @Test
    void addingACourtStoresTheGymOffTheSwingThread() throws Exception {
        panel.courtNumberField().setText("7");

        assertStoresOffSwingWhileDisabled(panel.addCourtButton(), panel.addCourtButton()::doClick);

        assertEquals(7, panel.selectedCourtNumber());
    }

    @Test
    void removingACourtStoresTheGymOffTheSwingThread() throws Exception {
        panel.courtNumberField().setText("7");
        press(panel.addCourtButton());

        assertStoresOffSwingWhileDisabled(panel.removeCourtButton(), () -> panel.removeSelectedCourt(number -> true));

        assertFalse(panel.courtNumbers().contains(7));
    }

    @Test
    void everyCameraStateButtonStoresTheGymOffTheSwingThread() throws Exception {
        panel.selectCamera(1);
        assertStoresOffSwingWhileDisabled(panel.stopButton(), panel.stopButton()::doClick);
        assertStoresOffSwingWhileDisabled(panel.startButton(), panel.startButton()::doClick);
        assertStoresOffSwingWhileDisabled(panel.maintenanceButton(), panel.maintenanceButton()::doClick);
        assertStoresOffSwingWhileDisabled(panel.removeCameraButton(), panel.removeCameraButton()::doClick);

        assertEquals(1, gym.findCourt(1).orElseThrow().getCameras().size());
    }

    @Test
    void movingAndAimingStoreTheGymOffTheSwingThread() throws Exception {
        panel.selectCamera(1);
        panel.panSpinner().setValue(90);
        assertStoresOffSwingWhileDisabled(panel.moveButton(), panel.moveButton()::doClick);

        panel.moveTargetSelector().setSelectedItem(2);
        assertStoresOffSwingWhileDisabled(panel.moveToCourtButton(), panel.moveToCourtButton()::doClick);

        assertEquals(2, gym.findCourt(2).orElseThrow().getCameras().size());
    }

    @Test
    void aFailedSaveGivesTheButtonBackWithTheSameMessage() {
        gymRepository.failFromNowOn();
        panel.selectCamera(1);

        press(panel.stopButton());

        assertTrue(panel.stopButton().isEnabled());
        assertEquals(List.of(GymChanges.NOT_SAVED), errors);
    }

    @Test
    void aChangeAskedForWhileACaptureHoldsTheModelIsIgnored() {
        CaptureActivity activity = new CaptureActivity();
        GatedGymRepository repository = new GatedGymRepository();
        CourtPanel court = new CourtPanel(new ManageGym(SampleGym.build(), repository), 30, errors::add, activity);
        activity.begin();
        court.courtNumberField().setText("7");

        press(court.addCourtButton());

        assertEquals(0, repository.saves());
        assertFalse(court.courtNumbers().contains(7));
    }

    private void assertStoresOffSwingWhileDisabled(javax.swing.AbstractButton trigger, Runnable action)
            throws InterruptedException {
        gymRepository.gate.close();
        int before = gymRepository.saves();
        action.run();
        try {
            gymRepository.gate.awaitBlocked();
            assertFalse(gymRepository.gate.ranOnSwing());
            assertTrue(SwingWork.swingResponds());
            SwingWork.onSwing(() -> assertFalse(trigger.isEnabled()));
        } finally {
            gymRepository.gate.open();
        }
        awaitRepositoryWork();
        assertEquals(before + 1, gymRepository.saves());
        assertEquals(List.of(), errors);
        SwingWork.onSwing(() -> assertTrue(trigger.isEnabled()));
    }

    private static void press(javax.swing.AbstractButton button) {
        button.doClick();
        awaitRepositoryWork();
    }
}
