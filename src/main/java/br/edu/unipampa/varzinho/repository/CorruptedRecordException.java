package br.edu.unipampa.varzinho.repository;

import br.edu.unipampa.varzinho.domain.highlight.Highlight;
import br.edu.unipampa.varzinho.exception.RepositoryException;

import java.util.List;

/**
 * Thrown when one record in the archive cannot be read back into a highlight.
 *
 * <p>It names the record that failed, not the file: the archive should not be lost
 * because a single line is malformed. It carries the highlights that did load, so the
 * caller can report the damage and carry on with the rest. Pass the parse failure as
 * the cause — dropping it throws away the only explanation of what the line actually said.
 */
public final class CorruptedRecordException extends RepositoryException {

    private final int lineNumber;
    private final List<Highlight> recovered;

    public CorruptedRecordException(String message) {
        this(message, 0, List.of(), null);
    }

    public CorruptedRecordException(String message, Throwable cause) {
        this(message, 0, List.of(), cause);
    }

    /**
     * @param lineNumber the first malformed line, counting the header as line 1
     * @param recovered every highlight that was read correctly, malformed lines skipped
     */
    public CorruptedRecordException(int lineNumber, List<Highlight> recovered, Throwable cause) {
        this("line " + lineNumber + " of the archive cannot be read: " + cause.getMessage(),
                lineNumber, recovered, cause);
    }

    private CorruptedRecordException(String message, int lineNumber, List<Highlight> recovered,
            Throwable cause) {
        super(message, cause);
        this.lineNumber = lineNumber;
        this.recovered = List.copyOf(recovered);
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public List<Highlight> getRecovered() {
        return recovered;
    }
}
