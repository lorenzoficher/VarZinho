package br.edu.unipampa.varzinho.ui;

import br.edu.unipampa.varzinho.application.port.RepositoryException;
import br.edu.unipampa.varzinho.exception.DomainException;

import java.util.function.Consumer;

/** Relays the result of a gym change without teaching a panel any domain rule. */
final class GymChangeReporter {
    private GymChangeReporter() { }

    static boolean attempt(Change change, Consumer<String> errorSink) {
        try {
            change.apply();
            return true;
        } catch (RepositoryException failure) {
            errorSink.accept("The gym could not be saved: " + failure.getMessage());
            return true;
        } catch (IllegalStateException | IllegalArgumentException | DomainException refusal) {
            errorSink.accept(refusal.getMessage());
            return false;
        }
    }

    @FunctionalInterface
    interface Change {
        void apply() throws RepositoryException;
    }
}
