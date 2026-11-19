package br.edu.unipampa.varzinho.ui;

import br.edu.unipampa.varzinho.application.Feed;
import br.edu.unipampa.varzinho.application.OperatorAccess;
import br.edu.unipampa.varzinho.application.OperatorView;
import br.edu.unipampa.varzinho.infrastructure.persistence.InMemoryOperatorStore;
import org.junit.jupiter.api.Test;

import javax.swing.JPanel;
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
        OperatorView operator = new OperatorAccess(new InMemoryOperatorStore())
                .createFirst("Rafaela Alves", "rafaela", "Strong1!").getOperator();
        SwingUtilities.invokeAndWait(() -> {
            JPanel courts = new JPanel();
            JPanel archive = new JPanel();
            JPanel content = VarZinhoWindow.navigation("Arena", operator, courts, archive, new MessageBar());
            BorderLayout layout = (BorderLayout) content.getLayout();
            SidebarPanel sidebar = (SidebarPanel) layout.getLayoutComponent(BorderLayout.WEST);
            assertTrue(courts.isVisible());
            assertFalse(archive.isVisible());
            sidebar.archiveButton().doClick();
            assertTrue(archive.isVisible());
            assertFalse(courts.isVisible());
            sidebar.courtsButton().doClick();
            assertFalse(archive.isVisible());
            assertTrue(courts.isVisible());
        });
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
