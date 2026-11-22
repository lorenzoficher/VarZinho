package br.edu.unipampa.varzinho.ui;

import br.edu.unipampa.varzinho.application.port.RepositoryException;
import br.edu.unipampa.varzinho.exception.DomainException;

import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Relays the result of a gym change without teaching a panel any domain rule. */
final class GymChangeReporter {
    private static final Logger LOGGER = Logger.getLogger(GymChangeReporter.class.getName());

    private GymChangeReporter() { }

    enum Result { SAVED, NOT_PERSISTED, REFUSED }

    static boolean attempt(Change change, Consumer<String> errorSink) {
        return apply(change, errorSink) != Result.REFUSED;
    }

    static Result apply(Change change, Consumer<String> errorSink) {
        try {
            change.apply();
            return Result.SAVED;
        } catch (RepositoryException failure) {
            LOGGER.log(Level.WARNING, "Could not persist the gym change", failure);
            errorSink.accept("Não foi possível salvar a academia. Verifique o armazenamento.");
            return Result.NOT_PERSISTED;
        } catch (IllegalStateException | IllegalArgumentException | DomainException refusal) {
            errorSink.accept(PortugueseText.message(refusal.getMessage()));
            return Result.REFUSED;
        }
    }

    @FunctionalInterface
    interface Change {
        void apply() throws RepositoryException;
    }
}
