package br.edu.unipampa.varzinho.exception;

/**
 * Thrown when a camera is installed under an id the gym already uses.
 *
 * <p>A clip is named after its camera, and the archive tells cameras apart by id alone.
 * Two cameras sharing one would make their clips collide and their highlights
 * indistinguishable, so the second one is refused rather than quietly ignored.
 */
public final class DuplicateCameraException extends DomainException {

    public DuplicateCameraException(String message) {
        super(message);
    }
}
