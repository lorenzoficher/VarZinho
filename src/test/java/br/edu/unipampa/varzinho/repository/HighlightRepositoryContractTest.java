package br.edu.unipampa.varzinho.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.edu.unipampa.varzinho.domain.highlight.Highlight;
import br.edu.unipampa.varzinho.domain.highlight.VideoClip;
import br.edu.unipampa.varzinho.enums.Resolution;
import br.edu.unipampa.varzinho.exception.RepositoryException;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * What every {@link HighlightRepository} must do, whatever it stores into. Each
 * implementation's test extends this class, so a second implementation proves the
 * abstraction is real.
 */
abstract class HighlightRepositoryContractTest {

    protected abstract HighlightRepository createRepository();

    @Test
    void findsASavedHighlightById() throws RepositoryException {
        HighlightRepository repository = createRepository();
        Highlight highlight = highlight("h-001", 3);

        repository.save(highlight);

        assertSame(highlight, repository.findById("h-001").orElseThrow());
    }

    @Test
    void findAllReturnsEveryHighlightSaved() throws RepositoryException {
        HighlightRepository repository = createRepository();
        Highlight first = highlight("h-001", 3);
        Highlight second = highlight("h-002", 4);
        repository.save(first);
        repository.save(second);

        assertEquals(2, repository.findAll().size());
        assertTrue(repository.findAll().containsAll(List.of(first, second)));
    }

    @Test
    void findByCourtReturnsOnlyThatCourtsHighlights() throws RepositoryException {
        HighlightRepository repository = createRepository();
        Highlight onCourtThree = highlight("h-001", 3);
        repository.save(onCourtThree);
        repository.save(highlight("h-002", 4));

        assertEquals(List.of(onCourtThree), repository.findByCourt(3));
    }

    @Test
    void findByCourtReturnsAnEmptyListWhenNothingMatches() throws RepositoryException {
        HighlightRepository repository = createRepository();
        repository.save(highlight("h-001", 3));

        assertTrue(repository.findByCourt(9).isEmpty());
    }

    @Test
    void rejectsSavingAMissingHighlight() throws RepositoryException {
        HighlightRepository repository = createRepository();

        assertThrows(IllegalArgumentException.class, () -> repository.save(null));
    }

    @Test
    void findByIdReturnsEmptyForAnUnknownId() throws RepositoryException {
        assertTrue(createRepository().findById("nobody").isEmpty());
    }

    @Test
    void savingAnExistingIdReplacesItInsteadOfDuplicating() throws RepositoryException {
        HighlightRepository repository = createRepository();
        repository.save(highlight("h-001", 3));
        Highlight replacement = highlight("h-001", 4);

        repository.save(replacement);

        assertEquals(List.of(replacement), repository.findAll());
    }

    @Test
    void findAllReturnsAnEmptyListWhenNothingWasSaved() throws RepositoryException {
        assertTrue(createRepository().findAll().isEmpty());
    }

    protected static Highlight highlight(String id, int courtNumber) {
        VideoClip clip = new VideoClip("/clips/" + id + ".mp4", 30, Resolution.FULL_HD, 42.5);
        return new Highlight(id, Instant.parse("2026-09-12T20:14:33Z"), courtNumber, "cam-a1", clip);
    }
}
