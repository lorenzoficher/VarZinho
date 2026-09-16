package br.edu.unipampa.varzinho.exception;

/**
 * Thrown when a capture is triggered on a court whose cameras are all inactive or under
 * maintenance.
 *
 * <p>Somebody pressed the button and got nothing back. Returning an empty highlight
 * would leave the gym believing the play was saved.
 */
public final class NoActiveCameraException extends DomainException {

    public NoActiveCameraException(String message) {
        super(message);
    }
}
