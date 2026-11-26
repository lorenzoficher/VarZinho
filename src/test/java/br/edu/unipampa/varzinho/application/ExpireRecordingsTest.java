package br.edu.unipampa.varzinho.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.edu.unipampa.varzinho.application.port.HighlightRepository;
import br.edu.unipampa.varzinho.application.port.RecordingEditor;
import br.edu.unipampa.varzinho.application.port.RepositoryException;
import br.edu.unipampa.varzinho.domain.highlight.Highlight;
import br.edu.unipampa.varzinho.domain.highlight.VideoClip;
import br.edu.unipampa.varzinho.enums.Resolution;
import br.edu.unipampa.varzinho.infrastructure.persistence.InMemoryHighlightRepository;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

class ExpireRecordingsTest {
    private static final Instant NOW = Instant.parse("2026-11-26T15:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final Duration SEVEN_DAYS = Duration.ofDays(7);

    private final InMemoryHighlightRepository archive = new InMemoryHighlightRepository();
    private final ArchiveEditor editor = new ArchiveEditor(archive);

    @Test
    void removesARecordingOlderThanTheRetentionPeriod() throws RepositoryException {
        archive.save(capturedDaysAgo("old", 8));

        int removed = new ExpireRecordings(archive, editor, CLOCK, SEVEN_DAYS).expire();

        assertEquals(1, removed);
        assertEquals(Set.of(), ids());
    }

    @Test
    void keepsARecordingYoungerThanTheRetentionPeriod() throws RepositoryException {
        archive.save(capturedDaysAgo("recent", 6));

        int removed = new ExpireRecordings(archive, editor, CLOCK, SEVEN_DAYS).expire();

        assertEquals(0, removed);
        assertEquals(Set.of("recent"), ids());
    }

    @Test
    void removesARecordingExactlyAsOldAsTheRetentionPeriod() throws RepositoryException {
        archive.save(capturedDaysAgo("seven", 7));

        new ExpireRecordings(archive, editor, CLOCK, SEVEN_DAYS).expire();

        assertEquals(Set.of(), ids());
    }

    @Test
    void removesOnlyTheExpiredOnesFromAMixedArchive() throws RepositoryException {
        archive.save(capturedDaysAgo("old", 30));
        archive.save(capturedDaysAgo("recent", 1));
        archive.save(capturedDaysAgo("older", 10));

        int removed = new ExpireRecordings(archive, editor, CLOCK, SEVEN_DAYS).expire();

        assertEquals(2, removed);
        assertEquals(Set.of("recent"), ids());
    }

    @Test
    void keepsRemovingTheOthersWhenOneCannotBeRemovedAndThenReportsIt() throws RepositoryException {
        archive.save(capturedDaysAgo("stuck", 9));
        archive.save(capturedDaysAgo("old", 8));
        RepositoryException refusal = new RepositoryException("clip is shared with another recording");
        editor.refuse("stuck", refusal);

        RepositoryException failure = assertThrows(RepositoryException.class,
                () -> new ExpireRecordings(archive, editor, CLOCK, SEVEN_DAYS).expire());

        assertEquals(Set.of("stuck"), ids());
        assertTrue(failure.getMessage().contains("stuck"), failure.getMessage());
        assertSame(refusal, failure.getCause());
    }

    @Test
    void refusesARetentionPeriodThatIsNotPositive() {
        assertThrows(IllegalArgumentException.class,
                () -> new ExpireRecordings(archive, editor, CLOCK, Duration.ZERO));
    }

    private Set<String> ids() throws RepositoryException {
        return archive.findAll().stream().map(Highlight::getId).collect(Collectors.toSet());
    }

    private static Highlight capturedDaysAgo(String id, int days) {
        return new Highlight(id, NOW.minus(Duration.ofDays(days)), 1, "fixed-1",
                new VideoClip("clips/" + id + ".mp4", 30, Resolution.FULL_HD, 12.5));
    }

    /** Removes from the archive the way the stored editor does, and can be told to refuse one. */
    private static final class ArchiveEditor implements RecordingEditor {
        private final HighlightRepository archive;
        private final Set<String> refused = new HashSet<>();
        private RepositoryException refusal;

        ArchiveEditor(HighlightRepository archive) {
            this.archive = archive;
        }

        void refuse(String id, RepositoryException failure) {
            refused.add(id);
            refusal = failure;
        }

        @Override
        public void rename(String id, String title) throws RepositoryException {
            archive.rename(id, title);
        }

        @Override
        public void delete(String id) throws RepositoryException {
            if (refused.contains(id)) {
                throw refusal;
            }
            archive.delete(id);
        }
    }
}
