package br.edu.unipampa.varzinho.application.port;

import br.edu.unipampa.varzinho.exception.DomainException;

/**
 * Root of the errors raised while reading or writing stored data: highlights, the gym or
 * operator accounts.
 *
 * <p>Checked, unlike {@link DomainException}, because here recovery is a real decision:
 * a caller loading the archive may skip what it cannot read and keep the rest. Forcing
 * that choice into the signature is the point.
 */
public class RepositoryException extends Exception {

    /**
     * A storage failure with nothing underneath to chain.
     *
     * @param message what could not be read or written
     */
    public RepositoryException(String message) {
        super(message);
    }

    /**
     * A storage failure caused by another one, kept so its stack trace survives.
     *
     * @param message what could not be read or written
     * @param cause the storage failure that explains it
     */
    public RepositoryException(String message, Throwable cause) {
        super(message, cause);
    }
}
