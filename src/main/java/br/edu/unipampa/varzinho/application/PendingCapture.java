package br.edu.unipampa.varzinho.application;

import br.edu.unipampa.varzinho.application.port.HighlightRepository;
import br.edu.unipampa.varzinho.application.port.RepositoryException;
import br.edu.unipampa.varzinho.domain.highlight.Highlight;

/**
 * A capture that has happened but is not in the archive yet.
 *
 * <p>Holds the one rule of this step: a highlight is archived only once its clip file
 * exists. A camera that writes no file has nothing to wait for.
 */
public final class PendingCapture {

    private static final Runnable NOTHING_TO_WRITE = () -> { };

    private final Highlight highlight;
    private final HighlightRepository repository;
    private final Runnable write;
    // The write runs on a worker and archive() reads this back on another thread.
    private volatile boolean readyToArchive;

    private PendingCapture(Highlight highlight, HighlightRepository repository, Runnable write,
            boolean readyToArchive) {
        this.highlight = highlight;
        this.repository = repository;
        this.write = write;
        this.readyToArchive = readyToArchive;
    }

    static PendingCapture awaitingWrite(Highlight highlight, HighlightRepository repository, Runnable write) {
        return new PendingCapture(highlight, repository, write, false);
    }

    static PendingCapture withNothingToWrite(Highlight highlight, HighlightRepository repository) {
        return new PendingCapture(highlight, repository, NOTHING_TO_WRITE, true);
    }

    /**
     * @return whether the camera left a clip file to write; when it did not, the capture
     *     can be archived at once
     */
    public boolean hasClipToWrite() {
        return write != NOTHING_TO_WRITE;
    }

    /** What the archive table will show for this capture once it is archived. */
    public HighlightView view() {
        return HighlightView.from(highlight);
    }

    /**
     * Writes the clip file, if this capture has one to write. Takes seconds, so the
     * window runs it off the Swing thread.
     *
     * @throws br.edu.unipampa.varzinho.exception.ClipAssemblyException if the file could
     *         not be written; the capture then stays unarchivable
     */
    public void writeClip() {
        write.run();
        readyToArchive = true;
    }

    /**
     * Saves the highlight.
     *
     * @return what the archive table shows for it
     * @throws IllegalStateException if the clip file has not been written; nothing is saved
     * @throws RepositoryException if the archive could not store it
     */
    public HighlightView archive() throws RepositoryException {
        if (!readyToArchive) {
            throw new IllegalStateException("highlight " + highlight.getId() + " has no clip file yet");
        }
        repository.save(highlight);
        return HighlightView.from(highlight);
    }
}
