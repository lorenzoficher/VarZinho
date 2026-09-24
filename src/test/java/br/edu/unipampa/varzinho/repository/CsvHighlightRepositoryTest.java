package br.edu.unipampa.varzinho.repository;

import br.edu.unipampa.varzinho.domain.highlight.Highlight;
import br.edu.unipampa.varzinho.domain.highlight.VideoClip;
import br.edu.unipampa.varzinho.enums.Resolution;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CsvHighlightRepositoryTest {
    @TempDir
    Path directory;

    @Test
    void savesAndReloadsAHighlightFromDisk() throws Exception {
        HighlightRepository repository = new CsvHighlightRepository(directory.resolve("archive.csv"));
        Highlight expected = new Highlight("h-1", Instant.parse("2026-09-24T12:00:00Z"), 2, "cam-1",
                new VideoClip("clips/h-1.mp4", 5, Resolution.FULL_HD, 10.5));

        repository.save(expected);

        var loaded = repository.findById("h-1");
        assertTrue(loaded.isPresent());
        assertEquals(expected.describe(), loaded.orElseThrow().describe());
        assertEquals(1, repository.findByCourt(2).size());
        assertTrue(repository.findByCourt(1).isEmpty());
    }
}
