package br.edu.unipampa.varzinho.ui;

import br.edu.unipampa.varzinho.application.port.RepositoryException;
import org.junit.jupiter.api.Test;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import java.awt.GraphicsEnvironment;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static br.edu.unipampa.varzinho.ui.SwingWork.awaitRepositoryWork;
import static br.edu.unipampa.varzinho.ui.SwingWork.onSwing;
import static br.edu.unipampa.varzinho.ui.SwingWork.swingResponds;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

class RepositoryTaskTest {
    private final JButton trigger = new JButton("Salvar");
    private final SwingWork.Gate gate = new SwingWork.Gate();

    @Test
    void workRunsOffTheSwingThreadWhileTheTriggerStaysDisabled() throws Exception {
        gate.close();
        List<String> delivered = new ArrayList<>();
        AtomicBoolean deliveredOnSwing = new AtomicBoolean();

        onSwing(() -> RepositoryTask.run(List.of(trigger), "Salvando…", () -> {
            gate.pass();
            return "saved";
        }, value -> {
            deliveredOnSwing.set(SwingUtilities.isEventDispatchThread());
            delivered.add(value);
        }, failure -> delivered.add("failed")));
        try {
            gate.awaitBlocked();
            assertFalse(gate.ranOnSwing());
            assertTrue(swingResponds());
            onSwing(() -> {
                assertFalse(trigger.isEnabled());
                assertTrue(delivered.isEmpty());
            });
        } finally {
            gate.open();
        }
        awaitRepositoryWork();

        assertEquals(List.of("saved"), delivered);
        assertTrue(deliveredOnSwing.get());
        onSwing(() -> assertTrue(trigger.isEnabled()));
    }

    @Test
    void aFailureReachesTheCallerWithItsOwnCauseAndTheTriggerComesBack() {
        AtomicReference<Throwable> reported = new AtomicReference<>();

        onSwing(() -> RepositoryTask.<Void>run(List.of(trigger), "Salvando…", () -> {
            throw new RepositoryException("disk full");
        }, value -> { }, reported::set));
        awaitRepositoryWork();

        assertInstanceOf(RepositoryException.class, reported.get());
        assertEquals("disk full", reported.get().getMessage());
        onSwing(() -> assertTrue(trigger.isEnabled()));
    }

    @Test
    void aTriggerThatWasAlreadyDisabledStaysDisabled() {
        onSwing(() -> {
            trigger.setEnabled(false);
            RepositoryTask.run(List.of(trigger), "Salvando…", () -> "saved", value -> { }, failure -> { });
        });
        awaitRepositoryWork();

        onSwing(() -> assertFalse(trigger.isEnabled()));
    }

    @Test
    void theMessageBarShowsProgressAndGivesBackWhatItShowedBefore() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "needs a window to hold the message bar");
        gate.close();
        MessageBar bar = new MessageBar();
        AtomicReference<JFrame> frame = new AtomicReference<>();
        onSwing(() -> {
            frame.set(new JFrame());
            frame.get().add(trigger);
            bar.serve(frame.get().getRootPane());
            bar.show("Câmera iniciada.");
            RepositoryTask.run(List.of(trigger), "Salvando a academia…", () -> {
                gate.pass();
                return "saved";
            }, value -> { }, failure -> { });
        });
        try {
            gate.awaitBlocked();
            onSwing(() -> assertEquals("Salvando a academia…", bar.message()));
        } finally {
            gate.open();
        }
        awaitRepositoryWork();

        onSwing(() -> {
            assertEquals("Câmera iniciada.", bar.message());
            frame.get().dispose();
        });
    }
}
