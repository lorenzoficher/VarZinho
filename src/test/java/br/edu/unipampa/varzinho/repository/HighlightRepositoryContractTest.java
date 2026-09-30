package br.edu.unipampa.varzinho.repository;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.edu.unipampa.varzinho.domain.highlight.Highlight;
import br.edu.unipampa.varzinho.domain.highlight.VideoClip;
import br.edu.unipampa.varzinho.enums.Resolution;
import br.edu.unipampa.varzinho.exception.RepositoryException;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

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
        repository.save(highlight("h-001", 3));

        assertEquals("h-001", repository.findById("h-001").orElseThrow().getId());
    }

    @Test
    void findAllReturnsEveryHighlightSaved() throws RepositoryException {
        HighlightRepository repository = createRepository();
        repository.save(highlight("h-001", 3));
        repository.save(highlight("h-002", 4));

        assertEquals(List.of("h-001", "h-002"), ids(repository.findAll()));
    }

    @Test
    void savingAnExistingIdKeepsItsPosition() throws RepositoryException {
        HighlightRepository repository = createRepository();
        repository.save(highlight("h-001", 3));
        repository.save(highlight("h-002", 3));

        repository.save(highlight("h-001", 4));

        assertEquals(List.of("h-001", "h-002"), ids(repository.findAll()));
        assertEquals(4, repository.findById("h-001").orElseThrow().getCourtNumber());
    }

    @Test
    void findingANullIdGivesAnEmptyResult() throws RepositoryException {
        HighlightRepository repository = createRepository();
        repository.save(highlight("h-001", 3));

        assertTrue(repository.findById(null).isEmpty());
    }

    @Test
    void findByCourtReturnsOnlyThatCourtsHighlights() throws RepositoryException {
        HighlightRepository repository = createRepository();
        repository.save(highlight("h-001", 3));
        repository.save(highlight("h-002", 4));

        assertEquals(List.of("h-001"), ids(repository.findByCourt(3)));
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

    @ParameterizedTest
    @ValueSource(strings = {",", "\n", "\r"})
    void refusesATextFieldHoldingACommaOrLineBreak(String character) {
        HighlightRepository repository = createRepository();
        Instant capturedAt = Instant.parse("2026-09-12T20:14:33Z");
        VideoClip clip = new VideoClip("/clips/a.mp4", 30, Resolution.FULL_HD, 42.5);
        VideoClip unstorableClip = new VideoClip("/clips/a" + character + "b.mp4", 30, Resolution.FULL_HD, 42.5);

        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> repository.save(
                        new Highlight("h" + character + "001", capturedAt, 3, "cam-a1", clip))),
                () -> assertThrows(IllegalArgumentException.class, () -> repository.save(
                        new Highlight("h-001", capturedAt, 3, "cam" + character + "a1", clip))),
                () -> assertThrows(IllegalArgumentException.class, () -> repository.save(
                        new Highlight("h-001", capturedAt, 3, "cam-a1", unstorableClip))));
    }

    @Test
    void aRefusedHighlightLeavesTheStoredOneInPlace() throws RepositoryException {
        HighlightRepository repository = createRepository();
        repository.save(highlight("h-001", 3));
        VideoClip clip = new VideoClip("/clips/h-001.mp4", 30, Resolution.FULL_HD, 42.5);
        Highlight unstorable = new Highlight("h-001", Instant.parse("2026-09-12T20:14:33Z"), 4, "cam,a1", clip);

        assertThrows(IllegalArgumentException.class, () -> repository.save(unstorable));

        assertEquals(List.of("h-001"), ids(repository.findAll()));
        assertEquals("cam-a1", repository.findById("h-001").orElseThrow().getCameraId());
    }

    @Test
    void findByIdReturnsEmptyForAnUnknownId()throws RepositoryException {
        assertTrue(createRepository().findById("nobody").isEmpty());
    }

    @Test
    void savingAnExistingIdReplacesItInsteadOfDuplicating() throws RepositoryException {
        HighlightRepository repository = createRepository();
        repository.save(highlight("h-001", 3));

        repository.save(highlight("h-001", 4));

        List<Highlight> all = repository.findAll();
        assertEquals(List.of("h-001"), ids(all));
        assertEquals(4, all.get(0).getCourtNumber());
    }

    @Test
    void findAllReturnsAnEmptyListWhenNothingWasSaved() throws RepositoryException {
        assertTrue(createRepository().findAll().isEmpty());
    }

    private static List<String> ids(List<Highlight> highlights) {
        return highlights.stream().map(Highlight::getId).collect(Collectors.toList());
    }

    static Highlight highlight(String id, int courtNumber) {
        VideoClip clip = new VideoClip("/clips/" + id + ".mp4", 30, Resolution.FULL_HD, 42.5);
        return new Highlight(id, Instant.parse("2026-09-12T20:14:33Z"), courtNumber, "cam-a1", clip);
    }
}
