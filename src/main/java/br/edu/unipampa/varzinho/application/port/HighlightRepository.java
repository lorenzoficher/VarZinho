package br.edu.unipampa.varzinho.application.port;

import br.edu.unipampa.varzinho.domain.highlight.Highlight;
import br.edu.unipampa.varzinho.domain.highlight.HighlightTitle;

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
     * @param highlight the highlight to keep
     * @throws IllegalArgumentException if the highlight is {@code null}, or its id, camera id
     *     or clip path holds a comma or a line break. Every implementation refuses these, even
     *     one that could store them, so a highlight one repository accepts any other accepts
     *     too. That is bad input rather than a storage failure, so it is unchecked and never a
     *     {@link RepositoryException}, and nothing is stored.
     * @throws RepositoryException if the storage cannot be written
     */
    void save(Highlight highlight) throws RepositoryException;

    /**
     * Gives a stored highlight a new name, keeping its position and everything else it holds.
     *
     * @param id the stored highlight to rename
     * @param title the new name, checked by {@link HighlightTitle}
     * @throws IllegalArgumentException if the name is blank or spans more than one line
     * @throws RepositoryException if no stored highlight has this id, or the storage cannot
     *     be read or written
     */
    void rename(String id, String title) throws RepositoryException;

    /**
     * Removes a stored highlight. An unknown id leaves the storage as it was: the highlight
     * is gone either way, so there is nothing to report.
     *
     * @param id the stored highlight to remove
     * @throws RepositoryException if the storage cannot be read or written
     */
    void delete(String id) throws RepositoryException;

    /**
     * Looks up one highlight. An unknown id, or {@code null}, gives an empty result — never
     * an exception.
     *
     * @param id the highlight to look for
     * @return the stored highlight, or empty when none has this id
     * @throws RepositoryException if the storage cannot be read
     */
    Optional<Highlight> findById(String id) throws RepositoryException;

    /**
     * Every highlight, in the order first saved.
     *
     * @return the stored highlights, empty when nothing was ever saved
     * @throws RepositoryException if the storage cannot be read; a
     *     {@link CorruptedRecordException} still carries the highlights that could
     */
    List<Highlight> findAll() throws RepositoryException;

    /**
     * The highlights of one court, in the order first saved.
     *
     * @param courtNumber the court whose highlights are wanted
     * @return that court's highlights, empty when it has none
     * @throws RepositoryException if the storage cannot be read; a
     *     {@link CorruptedRecordException} still carries the matching highlights that could
     */
    List<Highlight> findByCourt(int courtNumber) throws RepositoryException;
}
