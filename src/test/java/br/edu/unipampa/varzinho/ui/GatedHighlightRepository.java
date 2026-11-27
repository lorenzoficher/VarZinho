package br.edu.unipampa.varzinho.ui;

import br.edu.unipampa.varzinho.application.port.HighlightRepository;
import br.edu.unipampa.varzinho.application.port.RepositoryException;
import br.edu.unipampa.varzinho.domain.highlight.Highlight;
import br.edu.unipampa.varzinho.infrastructure.persistence.InMemoryHighlightRepository;

import java.util.List;
import java.util.Optional;

/**
 * A highlight archive in memory that can hold a save or a read at its gate, as a slow
 * database would, and can be told to fail its saves.
 */
final class GatedHighlightRepository implements HighlightRepository {
    private static final int EVERY_COURT = -1;
    final SwingWork.Gate gate = new SwingWork.Gate();
    private final InMemoryHighlightRepository stored = new InMemoryHighlightRepository();
    private volatile boolean failing;
    private volatile int gatedCourt = EVERY_COURT;

    void failSavesFromNowOn() { failing = true; }

    /** Lets reads of every other court straight through, so one slow read can be overtaken. */
    void gateOnlyReadsOf(int courtNumber) { gatedCourt = courtNumber; }

    @Override
    public void save(Highlight highlight) throws RepositoryException {
        gate.pass();
        if (failing) throw new RepositoryException("disk unplugged");
        stored.save(highlight);
    }

    @Override
    public void rename(String id, String title) throws RepositoryException {
        stored.rename(id, title);
    }

    @Override
    public void delete(String id) throws RepositoryException {
        stored.delete(id);
    }

    @Override
    public Optional<Highlight> findById(String id) throws RepositoryException {
        return stored.findById(id);
    }

    @Override
    public List<Highlight> findAll() throws RepositoryException {
        gate.pass();
        return stored.findAll();
    }

    @Override
    public List<Highlight> findByCourt(int courtNumber) throws RepositoryException {
        if (gatedCourt == EVERY_COURT || gatedCourt == courtNumber) gate.pass();
        return stored.findByCourt(courtNumber);
    }
}
