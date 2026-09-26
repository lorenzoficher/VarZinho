package br.edu.unipampa.varzinho.repository;

import br.edu.unipampa.varzinho.domain.highlight.Highlight;
import br.edu.unipampa.varzinho.exception.RepositoryException;

import java.util.List;
import java.util.Optional;

/**
 * Where highlights are kept between one request and the next. Every caller depends
 * on this interface, never on a class that knows how the storage works.
 *
 * <p>There is no lookup by kind of play: the system does not classify plays.
 */
public interface HighlightRepository {

    /**
     * Stores a highlight, replacing any earlier one with the same id. A replaced highlight
     * keeps its position: only a new id goes to the end.
     *
     * @throws IllegalArgumentException if the highlight is {@code null}, or holds a value the
     *     storage cannot represent. That is bad input rather than a storage failure, so it is
     *     unchecked and never a {@link RepositoryException}. The CSV file cannot hold a comma
     *     or a line break in a field; an implementation with no such limit accepts them.
     * @throws RepositoryException if the storage cannot be written
     */
    void save(Highlight highlight) throws RepositoryException;

    /** An unknown id, or {@code null}, gives an empty result — never an exception. */
    Optional<Highlight> findById(String id) throws RepositoryException;

    /** Every highlight, in the order first saved. */
    List<Highlight> findAll() throws RepositoryException;

    /** The highlights of one court, in the order first saved. */
    List<Highlight> findByCourt(int courtNumber) throws RepositoryException;
}
