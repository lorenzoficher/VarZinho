package br.edu.unipampa.varzinho.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.edu.unipampa.varzinho.domain.highlight.Highlight;
import br.edu.unipampa.varzinho.domain.highlight.VideoClip;
import br.edu.unipampa.varzinho.enums.Resolution;
import br.edu.unipampa.varzinho.infrastructure.persistence.InMemoryHighlightRepository;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

class BrowseArchiveTest {

    @Test
    void listsHighlightsNewestFirstAsReadModels() throws Exception {
        InMemoryHighlightRepository repository = new InMemoryHighlightRepository();
        repository.save(highlight("old", "2026-09-12T20:14:33Z", 1));
        repository.save(highlight("new", "2026-09-12T20:41:02Z", 2));

        ArchiveListing listing = new BrowseArchive(repository).all();

        assertEquals(List.of("camera-new", "camera-old"), cameraIds(listing));
        assertEquals(2, listing.getHighlights().get(0).getCourtNumber());
        assertEquals(30, listing.getHighlights().get(0).getDurationSeconds());
        assertEquals("clips/new.mp4", listing.getHighlights().get(0).getClipPath());
        assertTrue(listing.getWarning().isEmpty());
    }

    @Test
    void listsOnlyTheChosenCourtNewestFirst() throws Exception {
        InMemoryHighlightRepository repository = new InMemoryHighlightRepository();
        repository.save(highlight("court-3-old", "2026-09-12T20:14:33Z", 3));
        repository.save(highlight("other", "2026-09-12T20:50:00Z", 5));
        repository.save(highlight("court-3-new", "2026-09-12T20:41:02Z", 3));

        ArchiveListing listing = new BrowseArchive(repository).byCourt(3);

        assertEquals(List.of("camera-court-3-new", "camera-court-3-old"), cameraIds(listing));
        assertTrue(listing.getHighlights().stream()
                .allMatch(highlight -> highlight.getCourtNumber() == 3));
    }

    private static List<String> cameraIds(ArchiveListing listing) {
        return listing.getHighlights().stream()
                .map(HighlightView::getCameraId)
                .collect(Collectors.toList());
    }

    private static Highlight highlight(String id, String capturedAt, int courtNumber) {
        VideoClip clip = new VideoClip("clips/" + id + ".mp4", 30, Resolution.FULL_HD, 42.5);
        return new Highlight("highlight-" + id, Instant.parse(capturedAt), courtNumber,
                "camera-" + id, clip);
    }
}
