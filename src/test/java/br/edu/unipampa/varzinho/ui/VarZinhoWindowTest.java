package br.edu.unipampa.varzinho.ui;

import br.edu.unipampa.varzinho.application.Feed;
import br.edu.unipampa.varzinho.application.OperatorAccess;
import br.edu.unipampa.varzinho.application.OperatorView;
import br.edu.unipampa.varzinho.infrastructure.persistence.InMemoryOperatorStore;
import org.junit.jupiter.api.Test;

import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VarZinhoWindowTest {
    @Test
    void selectingPagesInTheSidebarDisplaysTheirCards() throws Exception {
        var operator = new OperatorAccess(new InMemoryOperatorStore())
                .createFirst("Rafaela Alves", "rafaela", "Strong1!");
        SwingUtilities.invokeAndWait(() -> {
            var gym = br.edu.unipampa.varzinho.bootstrap.SampleGym.build();
            var repository = new br.edu.unipampa.varzinho.infrastructure.persistence.InMemoryHighlightRepository();
            var management = new br.edu.unipampa.varzinho.application.ManageGym(gym,
                    new br.edu.unipampa.varzinho.infrastructure.persistence.InMemoryGymRepository());
            var workspace = new ArenaWorkspace("Arena", operator, management,
                    new br.edu.unipampa.varzinho.application.CaptureHighlight(gym, repository),
                    new br.edu.unipampa.varzinho.application.BrowseArchive(repository),
                    30, new CaptureActivity(), new MessageBar());
            var sidebar = (SidebarPanel) ((BorderLayout) workspace.getLayout()).getLayoutComponent(BorderLayout.WEST);
            assertTrue(visible(workspace, DashboardPanel.class));
            sidebar.archiveButton().doClick();
            assertTrue(visible(workspace, ArchivePanel.class));
            assertFalse(visible(workspace, DashboardPanel.class));
            sidebar.courtsButton().doClick();
            assertTrue(visible(workspace, CourtSelectionPanel.class));
            sidebar.camerasButton().doClick();
            assertTrue(visible(workspace, CameraListPanel.class));
            sidebar.highlightsButton().doClick();
            assertTrue(visible(workspace, CapturePanel.class));
            sidebar.operatorsButton().doClick();
            assertTrue(visible(workspace, OperatorsPanel.class));
            sidebar.settingsButton().doClick();
            assertTrue(visible(workspace, SettingsPanel.class));
            sidebar.dashboardButton().doClick();
            assertTrue(visible(workspace, CameraListPanel.class));
            assertTrue(visible(workspace, ArchivePanel.class));
        });
    }

    private static boolean visible(java.awt.Container parent, Class<?> type) {
        for (java.awt.Component component : parent.getComponents()) {
            if (!component.isVisible()) continue;
            if (type.isInstance(component)) return true;
            if (component instanceof java.awt.Container nested && visible(nested, type)) return true;
        }
        return false;
    }

    @Test
    void repeatedFeedFailuresLeaveNewerMessagesInPlaceUntilRecovery() throws Exception {
        AtomicBoolean disconnected = new AtomicBoolean(true);
        Feed intermittentlyAvailable = new Feed() {
            @Override public int periodMillis() { return 1000; }
            @Override public void tick(Instant now) throws IOException {
                if (disconnected.get()) throw new IOException("segment index unavailable");
            }
        };
        SwingUtilities.invokeAndWait(() -> {
            MessageBar bar = new MessageBar();
            Timer clock = VarZinhoWindow.clockFor(intermittentlyAvailable, new CaptureActivity(), () -> { }, bar);
            Runnable tick = () -> clock.getActionListeners()[0].actionPerformed(null);
            tick.run();
            String warning = bar.message();
            assertEquals("Não foi possível ler as imagens do celular.", warning);
            bar.show("Lance salvo.");
            tick.run();
            assertEquals("Lance salvo.", bar.message());
            disconnected.set(false);
            tick.run();
            disconnected.set(true);
            tick.run();
            assertEquals(warning, bar.message());
        });
    }
}
