package br.edu.unipampa.varzinho.ui;

import br.edu.unipampa.varzinho.application.port.RepositoryException;

import javax.swing.SwingWorker;
import java.awt.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Runs one storage call on a worker, so the window keeps answering while a database
 * does. The controls that started it stay disabled until it ends, the message bar says
 * what is happening, and the outcome comes back on the Swing thread.
 *
 * <p>It owns no rule: what the work does and how a failure is worded stay with the panel.
 */
final class RepositoryTask<T> extends SwingWorker<T, Void> {
    private static final AtomicInteger RUNNING = new AtomicInteger();
    private final Work<T> work;
    private final Consumer<T> onSuccess;
    private final Consumer<Throwable> onFailure;
    private final List<Component> disabled = new ArrayList<>();
    private Runnable withdrawProgress = () -> { };

    @FunctionalInterface
    interface Work<T> {
        T run() throws RepositoryException;
    }

    private RepositoryTask(Work<T> work, Consumer<T> onSuccess, Consumer<Throwable> onFailure) {
        this.work = work;
        this.onSuccess = onSuccess;
        this.onFailure = onFailure;
    }

    /**
     * Starts the work on a worker. Called on the Swing thread; both callbacks run there too,
     * after the controls are enabled again, so a callback may still decide to disable one.
     *
     * @param triggers the controls to keep disabled meanwhile; one already disabled stays so
     * @param progress what the message bar shows until the work ends, or {@code null} for nothing
     * @param onFailure receives the cause the work threw, or the interruption that stopped it
     */
    static <T> void run(List<? extends Component> triggers, String progress, Work<T> work,
                        Consumer<T> onSuccess, Consumer<Throwable> onFailure) {
        if (triggers == null) throw new IllegalArgumentException("a repository task needs its triggers, even none");
        if (work == null) throw new IllegalArgumentException("a repository task needs work to run");
        if (onSuccess == null || onFailure == null) {
            throw new IllegalArgumentException("a repository task needs somewhere to report its outcome");
        }
        RepositoryTask<T> task = new RepositoryTask<>(work, onSuccess, onFailure);
        for (Component trigger : triggers) {
            if (!trigger.isEnabled()) continue;
            trigger.setEnabled(false);
            task.disabled.add(trigger);
        }
        if (!triggers.isEmpty() && progress != null) {
            task.withdrawProgress = MessageBar.busyFor(triggers.get(0), progress);
        }
        RUNNING.incrementAndGet();
        task.execute();
    }

    /** True when no repository task is waiting on storage or still has to report. */
    static boolean idle() {
        return RUNNING.get() == 0;
    }

    @Override
    protected T doInBackground() throws RepositoryException {
        return work.run();
    }

    @Override
    protected void done() {
        try {
            withdrawProgress.run();
            disabled.forEach(control -> control.setEnabled(true));
            report();
        } finally {
            RUNNING.decrementAndGet();
        }
    }

    private void report() {
        T value;
        try {
            value = get();
        } catch (ExecutionException failure) {
            onFailure.accept(failure.getCause());
            return;
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            onFailure.accept(interrupted);
            return;
        }
        onSuccess.accept(value);
    }
}
