package br.edu.unipampa.varzinho.ui;

import br.edu.unipampa.varzinho.application.port.RepositoryException;
import br.edu.unipampa.varzinho.exception.DomainException;

import java.awt.Component;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Runs a gym change on a worker and relays its result without teaching a panel any
 * domain rule.
 *
 * <p>A change edits the gym in memory and then stores it, so the Swing thread lends the
 * model to the worker exactly as it does for a capture: feeds skip their ticks, the
 * court controls pause, and a second change or a capture waits for the first to return.
 */
final class GymChanges {
    private static final Logger LOGGER = Logger.getLogger(GymChanges.class.getName());
    private static final String PROGRESS = "Salvando a academia…";
    static final String NOT_SAVED =
            "Não foi possível salvar a academia. A alteração foi desfeita; verifique o armazenamento.";
    private final CaptureActivity activity;
    private final Runnable pause;
    private final Runnable resume;

    /** How a change ended. Only a saved change is still in the gym: the rest left it as it was. */
    enum Result { SAVED, NOT_PERSISTED, REFUSED }

    @FunctionalInterface
    interface Change {
        void apply() throws RepositoryException;
    }

    /**
     * @param activity the loan of the model shared with captures
     * @param pause stops the operator from starting another change meanwhile
     * @param resume undoes {@code pause}
     */
    GymChanges(CaptureActivity activity, Runnable pause, Runnable resume) {
        if (activity == null) throw new IllegalArgumentException("gym changes need the model's loan");
        if (pause == null || resume == null) throw new IllegalArgumentException("gym changes need to pause the controls");
        this.activity = activity;
        this.pause = pause;
        this.resume = resume;
    }

    /**
     * Starts the change on a worker. Does nothing while a capture or another change holds
     * the model, the same way a disabled button would.
     *
     * @param trigger the control that asked for the change, disabled until it ends
     * @param errorSink where a refusal or a storage failure is worded, on the Swing thread
     * @param onResult told how the change ended, on the Swing thread, after the controls are back
     */
    void apply(Component trigger, Change change, Consumer<String> errorSink, Consumer<Result> onResult) {
        if (activity.isRunning()) return;
        activity.begin();
        pause.run();
        RepositoryTask.<Void>run(List.of(trigger), PROGRESS, () -> {
            change.apply();
            return null;
        }, done -> {
            giveBack();
            onResult.accept(Result.SAVED);
        }, failure -> {
            giveBack();
            onResult.accept(report(failure, errorSink));
        });
    }

    /** Like {@link #apply}, for a panel that only cares whether the gym kept the change. */
    void attempt(Component trigger, Change change, Consumer<String> errorSink, Runnable onSaved) {
        apply(trigger, change, errorSink, result -> {
            if (result == Result.SAVED) onSaved.run();
        });
    }

    private void giveBack() {
        resume.run();
        activity.finish();
    }

    private static Result report(Throwable failure, Consumer<String> errorSink) {
        if (failure instanceof RepositoryException) {
            LOGGER.log(Level.WARNING, "Could not persist the gym change", failure);
            errorSink.accept(NOT_SAVED);
            return Result.NOT_PERSISTED;
        }
        if (failure instanceof IllegalStateException || failure instanceof IllegalArgumentException
                || failure instanceof DomainException) {
            errorSink.accept(PortugueseText.message(failure.getMessage()));
            return Result.REFUSED;
        }
        LOGGER.log(Level.SEVERE, "A gym change failed unexpectedly", failure);
        errorSink.accept("Falha inesperada ao salvar a academia. A alteração foi desfeita.");
        return Result.NOT_PERSISTED;
    }
}
