package br.edu.unipampa.varzinho.application.port;

import br.edu.unipampa.varzinho.domain.highlight.Highlight;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Thrown when one record in the archive cannot be read back into a highlight.
 *
 * <p>It names the record that failed, not the file: the archive should not be lost
 * because a single line is malformed. It carries the highlights that did load, so the
 * caller can report the damage and carry on with the rest. Pass the parse failure as
 * the cause — dropping it throws away the only explanation of what the line actually said.
 */
public final class CorruptedRecordException extends RepositoryException {

    /** The first malformed line, counting the header as line 1; 0 when no line applies. */
    private final int lineNumber;

    /** Every highlight read correctly despite the damage. */
    private final List<Highlight> recovered;

    /**
     * Damage found where no line number applies, with nothing recovered.
     *
     * @param message what could not be read
     */
    public CorruptedRecordException(String message) {
        this(message, 0, List.of(), null);
    }

    /**
     * Damage found where no line number applies, with nothing recovered.
     *
     * @param message what could not be read
     * @param cause the parse failure that explains it
     */
    public CorruptedRecordException(String message, Throwable cause) {
        this(message, 0, List.of(), cause);
    }

    /**
     * Damage on a known line of the archive, keeping what the rest of it still holds.
     *
     * @param lineNumber the first malformed line, counting the header as line 1
     * @param recovered every highlight that was read correctly, malformed lines skipped
     * @param cause the parse failure on that line; its message becomes part of this one
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

    /**
     * The same damage, reporting only the recovered highlights that match a search.
     * This exception stays as it was.
     *
     * @param filter which recovered highlights to keep
     * @return a new exception with the same message, line and cause
     */
    public CorruptedRecordException keeping(Predicate<Highlight> filter) {
        List<Highlight> matching = recovered.stream().filter(filter).collect(Collectors.toList());
        return new CorruptedRecordException(getMessage(), lineNumber, matching, getCause());
    }

    /** {@return the first malformed line, or 0 when the damage has no line} */
    public int getLineNumber() {
        return lineNumber;
    }

    /** {@return the highlights that could still be read, unmodifiable} */
    public List<Highlight> getRecovered() {
        return recovered;
    }
}
