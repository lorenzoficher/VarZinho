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

    static boolean attempt(Change change, Consumer<String> errorSink) {
        try {
            change.apply();
            return true;
        } catch (RepositoryException failure) {
            LOGGER.log(Level.WARNING, "Could not persist the gym change", failure);
            errorSink.accept("Não foi possível salvar a academia. Verifique o armazenamento.");
            return true;
        } catch (IllegalStateException | IllegalArgumentException | DomainException refusal) {
            errorSink.accept(PortugueseText.message(refusal.getMessage()));
            return false;
        }
    }

    @FunctionalInterface
    interface Change {
        void apply() throws RepositoryException;
    }
}
