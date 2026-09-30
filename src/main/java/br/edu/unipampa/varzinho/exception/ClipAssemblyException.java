package br.edu.unipampa.varzinho.exception;

/**
 * Thrown when a camera kept the seconds but the file that should hold them could not be
 * written — the video tool failed, timed out, or the disk refused.
 *
 * <p>Unlike an empty buffer, the footage exists; what is missing is the clip. When the
 * failure comes from something that threw, the cause is always carried, because the
 * reason lives there and not here.
 */
public final class ClipAssemblyException extends DomainException {

    public ClipAssemblyException(String message) {
        super(message);
    }

    public ClipAssemblyException(String message, Throwable cause) {
        super(message, cause);
    }
}
