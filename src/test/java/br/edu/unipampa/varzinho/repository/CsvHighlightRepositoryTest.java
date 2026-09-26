package br.edu.unipampa.varzinho.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.edu.unipampa.varzinho.domain.highlight.Highlight;
import br.edu.unipampa.varzinho.domain.highlight.VideoClip;
import br.edu.unipampa.varzinho.enums.Resolution;
import br.edu.unipampa.varzinho.exception.RepositoryException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CsvHighlightRepositoryTest extends HighlightRepositoryContractTest {

    @TempDir
    private Path archive;

    @Override
    protected HighlightRepository createRepository() {
        return new CsvHighlightRepository(archive.resolve("highlights.csv"));
    }

    @Test
    void aNewInstanceOnTheSameFileFindsWhatAnotherSaved() throws RepositoryException {
        Path file = archive.resolve("highlights.csv");
        Highlight saved = highlight("h-001", 3);
        new CsvHighlightRepository(file).save(saved);

        Highlight reloaded = new CsvHighlightRepository(file).findById("h-001").orElseThrow();

        assertEquals(saved.getCapturedAt(), reloaded.getCapturedAt());
        assertEquals(saved.getCourtNumber(), reloaded.getCourtNumber());
        assertEquals(saved.getCameraId(), reloaded.getCameraId());
    }

    @Test
    void aReloadedClipKeepsPathDurationResolutionAndSize() throws RepositoryException {
        Path file = archive.resolve("highlights.csv");
        VideoClip clip = new VideoClip("/clips/goal.mp4", 30, Resolution.ULTRA_HD, 41.8);
        new CsvHighlightRepository(file).save(
                new Highlight("h-001", Instant.parse("2026-09-12T20:14:33Z"), 3, "cam-a1", clip));

        VideoClip reloaded = new CsvHighlightRepository(file).findById("h-001").orElseThrow().getClip();

        assertEquals(clip.getFilePath(), reloaded.getFilePath());
        assertEquals(clip.getDurationSeconds(), reloaded.getDurationSeconds());
        assertEquals(clip.getResolution(), reloaded.getResolution());
        assertEquals(clip.getSizeMb(), reloaded.getSizeMb());
    }

    @Test
    void savingCreatesTheFileWhenItDoesNotExist() throws RepositoryException {
        Path file = archive.resolve("data").resolve("highlights.csv");
        assertFalse(Files.exists(file));

        new CsvHighlightRepository(file).save(highlight("h-001", 3));

        assertTrue(Files.exists(file));
    }

    @Test
    void writesTheHeaderExactlyOnce() throws RepositoryException, IOException {
        Path file = archive.resolve("highlights.csv");
        HighlightRepository repository = new CsvHighlightRepository(file);

        repository.save(highlight("h-001", 3));
        repository.save(highlight("h-002", 3));

        List<String> lines = Files.readAllLines(file);
        assertEquals(3, lines.size());
        assertEquals(1, lines.stream().filter(line -> line.startsWith("id,")).count());
    }

    @Test
    void leavesNoTemporaryFileBesideTheArchiveAfterSaving() throws RepositoryException, IOException {
        Path file = archive.resolve("highlights.csv");
        HighlightRepository repository = new CsvHighlightRepository(file);

        repository.save(highlight("h-001", 3));
        repository.save(highlight("h-002", 3));

        try (java.util.stream.Stream<Path> siblings = Files.list(archive)) {
            assertEquals(List.of(file), siblings.collect(java.util.stream.Collectors.toList()));
        }
    }

    @Test
    void refusesToWriteAFieldThatWouldBreakTheLineApart() {
        VideoClip clip = new VideoClip("/clips/a,b.mp4", 30, Resolution.FULL_HD, 1.0);
        Highlight highlight = new Highlight("h-001", Instant.parse("2026-09-12T20:14:33Z"), 3, "cam-a1", clip);

        org.junit.jupiter.api.Assertions.assertThrows(RepositoryException.class,
                () -> createRepository().save(highlight));
    }
}
