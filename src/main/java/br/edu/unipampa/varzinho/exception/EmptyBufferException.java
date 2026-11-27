package br.edu.unipampa.varzinho.exception;

/**
 * Thrown when a clip is asked of a camera that has not recorded enough footage yet —
 * typically one switched on seconds ago.
 *
 * <p>The window requested does not exist. This is not the same as a camera being off:
 * the camera is recording, it simply has no past.
 */
public final class EmptyBufferException extends DomainException {

    /**
     * Builds an exception describing the camera and the window that could not be filled.
     *
     * @param message the camera id and how many seconds were requested
     */
    public EmptyBufferException(String message) {
        super(message);
    }
}
