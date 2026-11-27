package br.edu.unipampa.varzinho.infrastructure.persistence;

import br.edu.unipampa.varzinho.application.port.HighlightRepository;
import br.edu.unipampa.varzinho.application.port.RepositoryException;
import br.edu.unipampa.varzinho.domain.highlight.Highlight;
import br.edu.unipampa.varzinho.domain.highlight.VideoClip;
import br.edu.unipampa.varzinho.enums.Resolution;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class StoredRecordingEditorTest {
    @TempDir private Path directory;

    @Test
    void aFailedCatalogDeletionRestoresTheOriginalVideo() throws Exception {
        Path video = Files.writeString(directory.resolve("game.mp4"), "video fixture");
        Highlight item = highlight("one", video);
        HighlightRepository catalog = new HighlightRepository() {
            public List<Highlight> findAll() { return List.of(item); }
            public void delete(String id) throws RepositoryException { throw new RepositoryException("offline"); }
            public void rename(String id, String title) { fail(); }
            public void save(Highlight value) { fail(); }
            public Optional<Highlight> findById(String id) { throw new AssertionError(); }
            public List<Highlight> findByCourt(int court) { throw new AssertionError(); }
        };
        var editor = new StoredRecordingEditor(catalog, directory);
        assertThrows(RepositoryException.class, () -> editor.delete("one"));
        assertEquals("video fixture", Files.readString(video));
        try (var files = Files.list(directory)) { assertEquals(List.of(video), files.toList()); }
    }

    @Test
    void externalAndSharedVideosArePreserved() throws Exception {
        Path clips = Files.createDirectory(directory.resolve("clips"));
        var catalog = new InMemoryHighlightRepository();
        var editor = new StoredRecordingEditor(catalog, clips);
        Path external = Files.writeString(directory.resolve("private.mp4"), "external");
        Path shared = Files.writeString(clips.resolve("shared.mp4"), "shared");
        catalog.save(highlight("one", external));
        catalog.save(highlight("two", shared));
        catalog.save(highlight("three", shared));
        assertThrows(RepositoryException.class, () -> editor.delete("one"));
        assertThrows(RepositoryException.class, () -> editor.delete("two"));
        assertTrue(Files.exists(external));
        assertTrue(Files.exists(shared));
        assertEquals(3, catalog.findAll().size());
    }

    private static Highlight highlight(String id, Path file) {
        return new Highlight(id, Instant.EPOCH, 1, "phone",
                new VideoClip(file.toString(), 30, Resolution.HD, 1));
    }
}
