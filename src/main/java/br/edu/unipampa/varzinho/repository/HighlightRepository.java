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
     * Stores a highlight, replacing any earlier one with the same id.
     *
     * @throws IllegalArgumentException if the highlight is {@code null}
     * @throws RepositoryException if the storage cannot be written
     */
    void save(Highlight highlight) throws RepositoryException;

    Optional<Highlight> findById(String id) throws RepositoryException;

    List<Highlight> findAll() throws RepositoryException;

    List<Highlight> findByCourt(int courtNumber) throws RepositoryException;
}
