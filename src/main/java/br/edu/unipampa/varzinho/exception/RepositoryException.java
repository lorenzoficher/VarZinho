package br.edu.unipampa.varzinho.exception;

/**
 * Root of the errors raised while reading or writing the highlight archive.
 *
 * <p>Checked, unlike {@link DomainException}, because here recovery is a real decision:
 * a caller loading the archive may skip what it cannot read and keep the rest. Forcing
 * that choice into the signature is the point.
 */
public class RepositoryException extends Exception {

    public RepositoryException(String message) {
        super(message);
    }

    public RepositoryException(String message, Throwable cause) {
        super(message, cause);
    }
}
