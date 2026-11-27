package br.edu.unipampa.varzinho.application;

import br.edu.unipampa.varzinho.application.port.HighlightRepository;
import br.edu.unipampa.varzinho.application.port.RepositoryException;
import br.edu.unipampa.varzinho.domain.highlight.Highlight;

/**
 * A completed capture awaiting archival. The camera has already finished its clip;
 * this object holds no deferred work or shared registry of writes.
 */
public final class PendingCapture {
    private final Highlight highlight;
    private final HighlightRepository repository;

    PendingCapture(Highlight highlight, HighlightRepository repository) {
        if (highlight == null) throw new IllegalArgumentException("a pending capture needs a highlight");
        if (repository == null) throw new IllegalArgumentException("a pending capture needs an archive");
        this.highlight = highlight;
        this.repository = repository;
    }

    /** {@return what the archive table will show once this capture is archived} */
    public HighlightView view() {
        return HighlightView.from(highlight);
    }

    /**
     * Saves the completed highlight.
     *
     * @return what the archive table shows for it
     * @throws RepositoryException if the archive could not store it
     */
    public HighlightView archive() throws RepositoryException {
        repository.save(highlight);
        return HighlightView.from(highlight);
    }
}
