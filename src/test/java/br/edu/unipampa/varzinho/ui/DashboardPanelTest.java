package br.edu.unipampa.varzinho.ui;

import br.edu.unipampa.varzinho.application.*;
import br.edu.unipampa.varzinho.bootstrap.SampleGym;
import br.edu.unipampa.varzinho.infrastructure.persistence.InMemoryGymRepository;
import br.edu.unipampa.varzinho.infrastructure.persistence.InMemoryHighlightRepository;
import br.edu.unipampa.varzinho.infrastructure.simulation.LiveFeed;
import org.junit.jupiter.api.Test;
import javax.swing.SwingUtilities;
import java.time.Instant;
import java.time.ZoneId;
import static org.junit.jupiter.api.Assertions.*;

class DashboardPanelTest {
    @Test
    void registersASimulatedPtzFromTheEmptyControlAndSavesItsAim(
            @org.junit.jupiter.api.io.TempDir java.nio.file.Path folder) throws Exception {
        var store = new br.edu.unipampa.varzinho.infrastructure.persistence.CsvGymRepository(folder.resolve("gym.csv"));
        var gym = SampleGym.build();
        var management = new ManageGym(gym, store);
        var archive = new InMemoryHighlightRepository();
        var operator = new OperatorAccess(new br.edu.unipampa.varzinho.infrastructure.persistence.InMemoryOperatorStore())
                .createFirst("Operadora", "operadora", "Senha123!");
        var found = new java.util.concurrent.atomic.AtomicReference<QuickPtzPanel>();
        SwingWork.onSwing(() -> {
            var workspace = new ArenaWorkspace("Arena", operator, management, new CaptureHighlight(gym, archive),
                    new BrowseArchive(archive), 30, new CaptureActivity(), new MessageBar());
            workspace.selectCourt(2);
            found.set(descendants(workspace).filter(QuickPtzPanel.class::isInstance)
                    .map(QuickPtzPanel.class::cast).findFirst().orElseThrow());
            assertTrue(button(found.get(), "←").isEnabled());
            button(found.get(), "←").doClick(0);
            button(found.get(), "↑").doClick(0);
            button(found.get(), "+").doClick(0);
            assertEquals(1, management.court(2).getCameraCount());
            found.get().refresh();
            button(found.get(), "Salvar posição").doClick(0);
        });
        SwingWork.awaitRepositoryWork();
        SwingWork.onSwing(() -> button(found.get(), "Salvar posição").doClick(0));
        SwingWork.awaitRepositoryWork();
        SwingWork.onSwing(() -> assertEquals(2, management.court(2).getCameraCount()));
        var saved = new ManageGym(store.load().orElseThrow(), store).court(2).getCameras().stream()
                .filter(CameraView::isAimable).findFirst().orElseThrow();
        assertEquals(355, saved.getPan().orElseThrow());
        assertEquals(5, saved.getTilt().orElseThrow());
        assertEquals(2, saved.getZoom().orElseThrow());
        assertFalse(saved.isRecording());
    }

    @Test
    void selectsThePhoneCourtEvenWhenConnectedAfterTheWindowOpened() throws Exception {
        var gym = SampleGym.build();
        var management = new ManageGym(gym, new InMemoryGymRepository());
        SwingUtilities.invokeAndWait(() -> {
            var courts = new CourtPanel(management, 30, message -> fail(message));
            var incoming = gym.openNextCourt();
            courts.selectCourt(incoming.getNumber());
            assertEquals(incoming.getNumber(), courts.selectedCourtNumber());
        });
    }
    @Test
    void choosingACameraByItsNameKeepsTheControlAndOnlyUpdatesSelection() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            var gym = new ManageGym(SampleGym.build(), new InMemoryGymRepository());
            var courts = new CourtPanel(gym, 30, message -> fail(message));
            var devices = courts.devices();
            var ptz = descendants(devices).filter(javax.swing.JRadioButton.class::isInstance)
                    .map(javax.swing.JRadioButton.class::cast).filter(b -> b.getText().equals("ptz-1"))
                    .findFirst().orElseThrow();
            ptz.doClick(0);
            assertTrue(ptz.isSelected());
            assertTrue(SwingUtilities.isDescendingFrom(ptz, devices));
            assertTrue(courts.moveButton().isEnabled());
            assertEquals("ptz-1", devices.selectedCamera().orElseThrow().getId());
            assertEquals(2, gym.court(1).getRecordingCameraCount());
        });
    }

    @Test
    void compactDashboardStacksCaptureAndAimAndExpandsAgain() throws Exception {
        var operator = new OperatorAccess(new br.edu.unipampa.varzinho.infrastructure.persistence.InMemoryOperatorStore())
                .createFirst("Operadora", "operadora", "Senha123!");
        SwingUtilities.invokeAndWait(() -> {
            VarZinhoTheme.install();
            var gym = SampleGym.build();
            var clips = new InMemoryHighlightRepository();
            var workspace = new ArenaWorkspace("Arena", operator, new ManageGym(gym, new InMemoryGymRepository()),
                    new CaptureHighlight(gym, clips), new BrowseArchive(clips), 30, new CaptureActivity(), new MessageBar());
            var hero = descendants(workspace).filter(PitchPanel.class::isInstance).findFirst().orElseThrow();
            var aim = descendants(workspace).filter(QuickPtzPanel.class::isInstance).findFirst().orElseThrow();
            for (int width : new int[]{1024, 1440, 1024}) {
                workspace.setSize(width, 700);
                for (int pass = 0; pass < 5; pass++) layoutTree(workspace);
                var pitch = SwingUtilities.convertRectangle(hero.getParent(), hero.getBounds(), workspace);
                var controls = SwingUtilities.convertRectangle(aim.getParent(), aim.getBounds(), workspace);
                if (width == 1024) assertTrue(controls.y >= pitch.y + pitch.height);
                else assertTrue(controls.x >= pitch.x + pitch.width);
                assertTrue(controls.x + controls.width <= width);
                assertTrue(aim.getY() + aim.getHeight() <= aim.getParent().getHeight(),
                        "The complete PTZ panel must fit in its section after resizing");
            }
        });
    }

    private static void layoutTree(java.awt.Container parent) {
        parent.doLayout();
        for (var child : parent.getComponents()) {
            if (child.isVisible() && child instanceof java.awt.Container nested) layoutTree(nested);
        }
    }

    @Test
    void cameraActionsTargetTheCameraWhoseMenuWasUsed() throws Exception {
        var gym = new ManageGym(SampleGym.build(), new InMemoryGymRepository());
        var courts = new java.util.concurrent.atomic.AtomicReference<CourtPanel>();
        var firstMenu = new java.util.concurrent.atomic.AtomicReference<javax.swing.JPopupMenu>();
        SwingWork.onSwing(() -> {
            courts.set(new CourtPanel(gym, 30, message -> fail(message)));
            var devices = courts.get().devices();
            var menus = descendants(devices).filter(javax.swing.JButton.class::isInstance)
                    .map(javax.swing.JButton.class::cast).filter(b -> "Ações".equals(b.getText())).toList();
            assertEquals(2, menus.size());
            var service = java.util.Arrays.stream(menus.get(1).getComponentPopupMenu().getComponents())
                    .filter(javax.swing.JMenuItem.class::isInstance).map(javax.swing.JMenuItem.class::cast)
                    .filter(item -> "Enviar para manutenção".equals(item.getText())).findFirst().orElseThrow();
            firstMenu.set(menus.get(0).getComponentPopupMenu());
            service.doClick(0);
        });
        SwingWork.awaitRepositoryWork();
        SwingWork.onSwing(() -> {
            var cameras = gym.court(1).getCameras();
            assertTrue(cameras.get(0).isRecording());
            assertTrue(cameras.get(1).isUnderMaintenance());
            courts.get().selectCourt(2);
            java.util.Arrays.stream(firstMenu.get().getComponents()).filter(javax.swing.JMenuItem.class::isInstance)
                    .map(javax.swing.JMenuItem.class::cast).filter(item -> "Remover câmera".equals(item.getText()))
                    .findFirst().orElseThrow().doClick(0);
        });
        SwingWork.awaitRepositoryWork();
        SwingWork.onSwing(() -> assertEquals(1, gym.court(2).getCameraCount()));
    }

    @Test
    void quickAimChangesTheSelectedPtzAndPreviewsOnAFixedOnlyCourt() {
        var gym = SampleGym.build();
        var management = new ManageGym(gym, new InMemoryGymRepository());
        var number = new java.util.concurrent.atomic.AtomicInteger(1);
        var changes = new GymChanges(new CaptureActivity(), () -> { }, () -> { });
        var panel = new java.util.concurrent.atomic.AtomicReference<QuickPtzPanel>();
        SwingWork.onSwing(() -> panel.set(new QuickPtzPanel(management, changes, number::get, message -> { },
                () -> fail("unexpected registration"))));
        var right = button(panel.get(), "→");
        var zoomIn = button(panel.get(), "+");

        SwingWork.onSwing(() -> right.doClick(0));
        SwingWork.awaitRepositoryWork();
        SwingWork.onSwing(() -> zoomIn.doClick(0));
        SwingWork.awaitRepositoryWork();

        SwingWork.onSwing(() -> {
            var ptz = management.court(1).getCameras().stream().filter(CameraView::isAimable)
                    .findFirst().orElseThrow();
            assertEquals(5, ptz.getPan().orElseThrow());
            assertEquals(2, ptz.getZoom().orElseThrow());
            number.set(2);
            panel.get().refresh();
            assertTrue(right.isEnabled());
            assertTrue(zoomIn.isEnabled());
            zoomIn.doClick(0);
            number.set(1);
            panel.get().refresh();
            assertTrue(right.isEnabled());
            assertEquals(2, management.court(1).getCameras().stream().filter(CameraView::isAimable)
                    .findFirst().orElseThrow().getZoom().orElseThrow());
        });
    }

    @Test
    void quickAimStoresOffTheSwingThreadWithItsArrowDisabled() throws Exception {
        var repository = new GatedGymRepository();
        var management = new ManageGym(SampleGym.build(), repository);
        var changes = new GymChanges(new CaptureActivity(), () -> { }, () -> { });
        var panel = new java.util.concurrent.atomic.AtomicReference<QuickPtzPanel>();
        SwingWork.onSwing(() -> panel.set(new QuickPtzPanel(management, changes, () -> 1, message -> { }, () -> { })));
        var right = button(panel.get(), "→");
        repository.gate.close();

        SwingWork.onSwing(() -> right.doClick(0));
        try {
            repository.gate.awaitBlocked();
            assertFalse(repository.gate.ranOnSwing());
            assertTrue(SwingWork.swingResponds());
            SwingWork.onSwing(() -> assertFalse(right.isEnabled()));
        } finally {
            repository.gate.open();
        }
        SwingWork.awaitRepositoryWork();

        SwingWork.onSwing(() -> assertTrue(right.isEnabled()));
    }

    private static javax.swing.JButton button(java.awt.Container parent, String text) {
        return java.util.Arrays.stream(parent.getComponents())
                .flatMap(child -> descendants(child))
                .filter(javax.swing.JButton.class::isInstance).map(javax.swing.JButton.class::cast)
                .filter(item -> text.equals(item.getText())).findFirst().orElseThrow();
    }

    private static java.util.stream.Stream<java.awt.Component> descendants(java.awt.Component component) {
        var self = java.util.stream.Stream.of(component);
        if (!(component instanceof java.awt.Container container)) return self;
        return java.util.stream.Stream.concat(self, java.util.Arrays.stream(container.getComponents())
                .flatMap(DashboardPanelTest::descendants));
    }

    @Test
    void embedsDevicesAndHistoryAndKeepsCourtSelectionWhileRecording() throws Exception {
        var saved = new java.util.concurrent.CountDownLatch(1);
        var historyPanel = new java.util.concurrent.atomic.AtomicReference<ArchivePanel>();
        SwingUtilities.invokeAndWait(() -> {
            VarZinhoTheme.install();
            var gym = SampleGym.build();
            var management = new ManageGym(gym, new InMemoryGymRepository());
            var repository = new InMemoryHighlightRepository();
            var zone = ZoneId.of("America/Sao_Paulo");
            var courts = new CourtPanel(management, 30, message -> fail(message));
            var history = new ArchivePanel(management::courts, new BrowseArchive(repository), zone, message -> fail(message));
            var capture = new CapturePanel(courts, new CaptureHighlight(gym, repository),
                    zone, message -> fail(message), highlight -> {
                        history.refresh();
                        saved.countDown();
                    }, new CaptureActivity());
            historyPanel.set(history);
            var dashboard = new DashboardPanel(management, courts, capture, page -> { }, message -> fail(message));
            assertTrue(button(dashboard, "Abrir transmissão ao vivo").isVisible());
            assertFalse(button(dashboard, "Copiar link público").isEnabled());
            dashboard.attach(courts.devices(), history);
            dashboard.selector().setSelectedItem(2);
            var feed = new LiveFeed(gym);
            Instant start = Instant.parse("2026-11-19T12:00:00Z");
            for (int i = 0; i < 41; i++) {
                feed.tick(start.plusSeconds(i));
                dashboard.tick(management.court(2));
                if (i == 11 || i == 29) {
                    var labels = descendants(dashboard).filter(javax.swing.JLabel.class::isInstance)
                            .map(javax.swing.JLabel.class::cast).map(javax.swing.JLabel::getText).toList();
                    assertTrue(labels.contains((i + 1) + " de 30 segundos no buffer"));
                    assertTrue(labels.contains(i == 11 ? "Preenchendo o buffer" : "Buffer de simulação cheio"));
                }
            }
            assertEquals(2, dashboard.selector().getSelectedItem());
            assertEquals(30, management.court(2).getSecondsRecorded());
            assertTrue(SwingUtilities.isDescendingFrom(courts.devices(), dashboard));
            assertTrue(SwingUtilities.isDescendingFrom(history, dashboard));
            dashboard.saveButton().doClick(0);
            assertFalse(capture.saveButton().isEnabled());
            assertFalse(dashboard.saveButton().isEnabled());
        });
        assertTrue(saved.await(5, java.util.concurrent.TimeUnit.SECONDS));
        SwingUtilities.invokeAndWait(() -> {
            var table = historyPanel.get().table();
            assertEquals(1, table.getRowCount());
            assertEquals(2, table.getValueAt(0, 1));
            assertEquals("30 s", table.getValueAt(0, 3));
        });
    }
}
