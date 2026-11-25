package br.edu.unipampa.varzinho.exception;

/**
 * Root of every error the model raises about itself.
 *
 * <p>Unchecked, because none of these is a decision the call site can make: a court with
 * no working camera cannot be captured, and no amount of handling changes that. The one
 * failure a caller can genuinely recover from belongs to persistence instead — see
 * {@code RepositoryException}.
 *
 * <p>Abstract on purpose: code catches this type, it never throws it.
 */
public abstract class DomainException extends RuntimeException {

    /**
     * Builds an exception with the given human-readable description.
     *
     * @param message what went wrong, in terms the operator can act on
     */
    protected DomainException(String message) {
        super(message);
    }

    /**
     * Builds an exception that wraps the lower-level failure that caused it.
     *
     * @param message what went wrong, in terms the operator can act on
     * @param cause the original failure; carried for diagnostics
     */
    protected DomainException(String message, Throwable cause) {
        super(message, cause);
    }
}
