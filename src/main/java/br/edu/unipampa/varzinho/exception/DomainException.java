package br.edu.unipampa.varzinho.exception;

/**
 * Root of every error the model raises about itself.
 *
 * <p>Unchecked, because none of these is a decision the call site can make: a court with
 * no working camera cannot be captured, and no amount of handling changes that. The one
 * failure a caller can genuinely recover from belongs to persistence instead — see
 * {@link RepositoryException}.
 *
 * <p>Abstract on purpose: code catches this type, it never throws it.
 */
public abstract class DomainException extends RuntimeException {

    protected DomainException(String message) {
        super(message);
    }
}
