package br.edu.unipampa.varzinho.repository;

import br.edu.unipampa.varzinho.domain.highlight.Highlight;

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
     */
    void save(Highlight highlight);

    Optional<Highlight> findById(String id);

    List<Highlight> findAll();

    List<Highlight> findByCourt(int courtNumber);
}
