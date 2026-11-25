package br.edu.unipampa.varzinho.ui;

import javax.swing.SwingUtilities;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** What the window tests share to wait for work the panels hand to a worker. */
final class SwingWork {
    private static final Duration PATIENCE = Duration.ofSeconds(5);

    private SwingWork() { }

    static void onSwing(Runnable action) {
        try {
            SwingUtilities.invokeAndWait(action);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new AssertionError("interrupted while waiting for the Swing thread", interrupted);
        } catch (java.lang.reflect.InvocationTargetException failure) {
            if (failure.getCause() instanceof RuntimeException runtime) throw runtime;
            if (failure.getCause() instanceof Error error) throw error;
            throw new AssertionError(failure.getCause());
        }
    }

    /** Waits until every repository task has finished and reported on the Swing thread. */
    static void awaitRepositoryWork() {
        awaitOnSwing(RepositoryTask::idle);
    }

    static void awaitOnSwing(BooleanSupplier condition) {
        assertTimeoutPreemptively(PATIENCE, () -> {
            AtomicBoolean met = new AtomicBoolean();
            while (!met.get()) onSwing(() -> met.set(condition.getAsBoolean()));
        });
    }

    /** True when the Swing thread runs a queued action while a worker is still blocked. */
    static boolean swingResponds() throws InterruptedException {
        CountDownLatch responded = new CountDownLatch(1);
        SwingUtilities.invokeLater(responded::countDown);
        return responded.await(PATIENCE.toSeconds(), TimeUnit.SECONDS);
    }

    /**
     * Holds a storage call until the test releases it, remembering whether it ran on the
     * Swing thread. While open it lets every call straight through.
     */
    static final class Gate {
        private final CountDownLatch started = new CountDownLatch(1);
        private final CountDownLatch released = new CountDownLatch(1);
        private final AtomicBoolean ranOnSwing = new AtomicBoolean();
        private volatile boolean closed;

        void close() { closed = true; }

        void open() {
            closed = false;
            released.countDown();
        }

        /** Called by the test double from inside the storage call. */
        void pass() {
            if (!closed) return;
            ranOnSwing.set(SwingUtilities.isEventDispatchThread());
            started.countDown();
            try {
                released.await(PATIENCE.toSeconds(), TimeUnit.SECONDS);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            }
        }

        void awaitBlocked() throws InterruptedException {
            assertTrue(started.await(PATIENCE.toSeconds(), TimeUnit.SECONDS), "the storage call never started");
        }

        boolean ranOnSwing() { return ranOnSwing.get(); }
    }
}
