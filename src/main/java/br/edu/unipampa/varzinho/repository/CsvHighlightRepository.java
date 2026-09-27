package br.edu.unipampa.varzinho.repository;

import br.edu.unipampa.varzinho.domain.highlight.Highlight;
import br.edu.unipampa.varzinho.domain.highlight.VideoClip;
import br.edu.unipampa.varzinho.enums.Resolution;
import br.edu.unipampa.varzinho.exception.CorruptedRecordException;
import br.edu.unipampa.varzinho.exception.RepositoryException;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Keeps highlights in a CSV file, so they outlive the process that captured them.
 *
 * <p>The file format is private to this class: no other class reads or writes it.
 * Every call goes to the file, so two instances on the same path always agree.
 */
public final class CsvHighlightRepository implements HighlightRepository {

    private static final String HEADER =
            "id,capturedAt,courtNumber,cameraId,clipPath,durationSeconds,resolution,sizeMb";
    private static final String SEPARATOR = ",";
    private static final int COLUMNS = 8;

    private final Path file;

    public CsvHighlightRepository(Path file) {
        if (file == null) {
            throw new IllegalArgumentException("the archive needs a file to live in");
        }
        this.file = file;
    }

    @Override
    public void save(Highlight highlight) throws RepositoryException {
        if (highlight == null) {
            throw new IllegalArgumentException("there is no highlight to save");
        }
        Map<String, Highlight> highlightsById = new LinkedHashMap<>();
        for (Highlight existing : findAll()) {
            highlightsById.put(existing.getId(), existing);
        }
        highlightsById.put(highlight.getId(), highlight);
        write(highlightsById.values());
    }

    @Override
    public Optional<Highlight> findById(String id) throws RepositoryException {
        return findAll().stream().filter(highlight -> highlight.getId().equals(id)).findFirst();
    }

    @Override
    public List<Highlight> findAll() throws RepositoryException {
        if (!Files.exists(file)) {
            return new ArrayList<>();
        }
        List<String> lines;
        try {
            lines = Files.readAllLines(file);
        } catch (IOException cause) {
            throw new RepositoryException("could not read the archive " + file, cause);
        }
        List<Highlight> highlights = new ArrayList<>();
        if (lines.isEmpty()) {
            return highlights;
        }
        // Skipping an unchecked first line would silently drop a record when the header is lost.
        if (!HEADER.equals(lines.get(0))) {
            throw new RepositoryException("the archive " + file + " does not start with the header");
        }
        for (int index = 1; index < lines.size(); index++) {
            highlights.add(parse(lines.get(index), index + 1));
        }
        return highlights;
    }

    @Override
    public List<Highlight> findByCourt(int courtNumber) throws RepositoryException {
        return findAll().stream()
                .filter(highlight -> highlight.getCourtNumber() == courtNumber)
                .collect(Collectors.toList());
    }

    private void write(Iterable<Highlight> highlights) throws RepositoryException {
        List<String> lines = new ArrayList<>();
        lines.add(HEADER);
        for (Highlight highlight : highlights) {
            lines.add(format(highlight));
        }
        try {
            Path directory = file.toAbsolutePath().getParent();
            Files.createDirectories(directory);
            // Writing in place truncates first, so a crash mid-write would lose the whole archive.
            Path temporary = Files.createTempFile(directory, "highlights", ".tmp");
            try {
                Files.write(temporary, lines);
                try {
                    Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING,
                            StandardCopyOption.ATOMIC_MOVE);
                } catch (AtomicMoveNotSupportedException unsupported) {
                    // Some synced or network folders refuse an atomic move; a plain replace still beats failing the save.
                    Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
                }
            } finally {
                Files.deleteIfExists(temporary);
            }
        } catch (IOException cause) {
            throw new RepositoryException("could not write the archive " + file, cause);
        }
    }

    private static String format(Highlight highlight) {
        VideoClip clip = highlight.getClip();
        String[] fields = {
            highlight.getId(),
            highlight.getCapturedAt().toString(),
            Integer.toString(highlight.getCourtNumber()),
            highlight.getCameraId(),
            clip.getFilePath(),
            Integer.toString(clip.getDurationSeconds()),
            clip.getResolution().name(),
            Double.toString(clip.getSizeMb())
        };
        // A separator or line break inside a field would split the record into two on reload.
        for (String field : fields) {
            if (field.contains(SEPARATOR) || field.contains("\n") || field.contains("\r")) {
                throw new IllegalArgumentException("cannot store a value with a comma or line break: " + field);
            }
        }
        return String.join(SEPARATOR, fields);
    }

    private static Highlight parse(String line, int lineNumber) throws CorruptedRecordException {
        String[] fields = line.split(SEPARATOR, -1);
        try {
            if (fields.length != COLUMNS) {
                throw new IllegalArgumentException(
                        "expected " + COLUMNS + " columns but found " + fields.length);
            }
            VideoClip clip = new VideoClip(fields[4], Integer.parseInt(fields[5]),
                    Resolution.valueOf(fields[6]), Double.parseDouble(fields[7]));
            return new Highlight(fields[0], Instant.parse(fields[1]), Integer.parseInt(fields[2]),
                    fields[3], clip);
        } catch (IllegalArgumentException | DateTimeParseException cause) {
            throw new CorruptedRecordException("cannot read line " + lineNumber + " of the archive", cause);
        }
    }
}
