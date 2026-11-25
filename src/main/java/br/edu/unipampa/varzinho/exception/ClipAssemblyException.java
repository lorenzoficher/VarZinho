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

    /**
     * Builds an exception describing why the clip could not be assembled.
     *
     * @param message what failed and which camera was involved
     */
    public ClipAssemblyException(String message) {
        super(message);
    }

    /**
     * Builds an exception that wraps the tool or I/O failure that caused it.
     *
     * @param message what failed and which camera was involved
     * @param cause the underlying failure; carried for diagnostics
     */
    public ClipAssemblyException(String message, Throwable cause) {
        super(message, cause);
    }
}
