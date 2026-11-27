package br.edu.unipampa.varzinho.application;

import br.edu.unipampa.varzinho.application.port.HighlightRepository;
import br.edu.unipampa.varzinho.application.port.RecordingEditor;
import br.edu.unipampa.varzinho.application.port.RepositoryException;
import br.edu.unipampa.varzinho.domain.highlight.Highlight;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Removes the recordings that have been kept for the whole retention period, so the
 * videos of a gym that records every day do not fill the disk.
 *
 * <p>Each one goes through the {@link RecordingEditor}, the same way an operator deletes a
 * recording, so the archive entry and its video file leave together or not at all.
 */
public final class ExpireRecordings {

    private final HighlightRepository archive;
    private final RecordingEditor editor;
    private final Clock clock;
    private final Duration retention;

    /**
     * Prepares the expiry; nothing is removed until {@link #expire()} runs.
     *
     * @param archive where the recordings are listed
     * @param editor what removes a recording together with its video file
     * @param clock the source of the present instant
     * @param retention how long a recording is kept after it was captured
     * @throws IllegalArgumentException if any argument is missing or the retention period
     *     is not positive
     */
    public ExpireRecordings(HighlightRepository archive, RecordingEditor editor, Clock clock, Duration retention) {
        if (archive == null || editor == null || clock == null) {
            throw new IllegalArgumentException("expiring recordings needs the archive, an editor and a clock");
        }
        if (retention == null || retention.isZero() || retention.isNegative()) {
            throw new IllegalArgumentException("the retention period must be positive, not " + retention);
        }
        this.archive = archive;
        this.editor = editor;
        this.clock = clock;
        this.retention = retention;
    }

    /**
     * Removes every recording captured at least the retention period ago. A recording that
     * cannot be removed does not stop the others; it stays, and is reported once all were
     * tried.
     *
     * @return how many recordings were removed
     * @throws RepositoryException if the archive cannot be read, or if any expired recording
     *     could not be removed; the message names those kept and the cause is the first failure
     */
    public int expire() throws RepositoryException {
        Instant cutoff = clock.instant().minus(retention);
        int removed = 0;
        List<String> kept = new ArrayList<>();
        RepositoryException firstFailure = null;
        for (Highlight recording : archive.findAll()) {
            if (recording.getCapturedAt().isAfter(cutoff)) {
                continue;
            }
            try {
                editor.delete(recording.getId());
                removed++;
            } catch (RepositoryException failure) {
                kept.add(recording.getId());
                if (firstFailure == null) {
                    firstFailure = failure;
                }
            }
        }
        if (firstFailure != null) {
            throw new RepositoryException("removed " + removed + " expired recordings but could not remove "
                    + String.join(", ", kept), firstFailure);
        }
        return removed;
    }
}
