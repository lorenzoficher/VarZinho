package br.edu.unipampa.varzinho.exception;

/**
 * Thrown when one record in the archive cannot be read back into a highlight.
 *
 * <p>It names the record that failed, not the file: the archive should not be lost
 * because a single line is malformed. Pass the parse failure as the cause — dropping it
 * throws away the only explanation of what the line actually said.
 */
public final class CorruptedRecordException extends RepositoryException {

    public CorruptedRecordException(String message) {
        super(message);
    }

    public CorruptedRecordException(String message, Throwable cause) {
        super(message, cause);
    }
}
