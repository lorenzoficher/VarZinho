package br.edu.unipampa.varzinho.repository;

import br.edu.unipampa.varzinho.domain.highlight.Highlight;
import br.edu.unipampa.varzinho.domain.highlight.VideoClip;
import br.edu.unipampa.varzinho.enums.Resolution;
import br.edu.unipampa.varzinho.exception.RepositoryException;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
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

    /**
     * {@inheritDoc}
     *
     * <p>Here the refused values are the ones the file cannot hold: a comma or a line break
     * inside a field would split its record in two on reload. The refusal comes before the
     * file is read, so a refused highlight leaves the archive exactly as it was.
     */
    @Override
    public void save(Highlight highlight) throws RepositoryException {
        StorableHighlights.requireStorable(highlight);
        String record = format(highlight);
        // Working on the raw lines, not on parsed highlights, so a damaged line neither blocks
        // the save nor is destroyed by it.
        List<String> lines = new ArrayList<>();
        boolean replaced = false;
        for (String existing : readLines().stream().skip(1).collect(Collectors.toList())) {
            if (existing.isBlank()) {
                continue;
            }
            if (!idOf(existing).equals(highlight.getId())) {
                lines.add(existing);
            } else if (!replaced) {
                lines.add(record);
                replaced = true;
            }
        }
        if (!replaced) {
            lines.add(record);
        }
        write(lines);
    }

    @Override
    public Optional<Highlight> findById(String id) throws RepositoryException {
        return findMatching(highlight -> highlight.getId().equals(id)).stream().findFirst();
    }

    @Override
    public List<Highlight> findAll() throws RepositoryException {
        List<String> lines = readLines();
        List<Highlight> highlights = new ArrayList<>();
        int firstMalformedLine = 0;
        RuntimeException firstFailure = null;
        // Index 0 is the header, so the file's line number is the index plus one.
        for (int index = 1; index < lines.size(); index++) {
            String line = lines.get(index);
            if (line.isBlank()) {
                continue;
            }
            try {
                highlights.add(parse(line));
            } catch (IllegalArgumentException | DateTimeParseException failure) {
                if (firstFailure == null) {
                    firstMalformedLine = index + 1;
                    firstFailure = failure;
                }
            }
        }
        if (firstFailure != null) {
            throw new CorruptedRecordException(firstMalformedLine, highlights, firstFailure);
        }
        return highlights;
    }

    @Override
    public List<Highlight> findByCourt(int courtNumber) throws RepositoryException {
        return findMatching(highlight -> highlight.getCourtNumber() == courtNumber);
    }

    // A damaged archive must not hand a filtered search records the search did not ask for.
    private List<Highlight> findMatching(Predicate<Highlight> filter) throws RepositoryException {
        try {
            return findAll().stream().filter(filter).collect(Collectors.toList());
        } catch (CorruptedRecordException damage) {
            throw damage.keeping(filter);
        }
    }

    /** Every line of the file, header first; empty when the file does not exist or is empty. */
    private List<String> readLines() throws RepositoryException {
        if (!Files.exists(file)) {
            return new ArrayList<>();
        }
        List<String> lines;
        try {
            lines = Files.readAllLines(file);
        } catch (IOException cause) {
            throw new RepositoryException("could not read the archive " + file, cause);
        }
        // Skipping an unchecked first line would silently drop a record when the header is lost.
        if (!lines.isEmpty() && !HEADER.equals(lines.get(0))) {
            throw new RepositoryException("the archive " + file + " does not start with the header");
        }
        return lines;
    }

    private static String idOf(String line) {
        int separator = line.indexOf(SEPARATOR);
        return separator < 0 ? line : line.substring(0, separator);
    }

    private void write(List<String> records) throws RepositoryException {
        List<String> lines = new ArrayList<>();
        lines.add(HEADER);
        lines.addAll(records);
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
        return String.join(SEPARATOR, fields);
    }

    private static Highlight parse(String line) {
        String[] fields = line.split(SEPARATOR, -1);
        if (fields.length != COLUMNS) {
            throw new IllegalArgumentException(
                    "expected " + COLUMNS + " columns but found " + fields.length);
        }
        VideoClip clip = new VideoClip(fields[4], Integer.parseInt(fields[5]),
                Resolution.valueOf(fields[6]), Double.parseDouble(fields[7]));
        return new Highlight(fields[0], Instant.parse(fields[1]), Integer.parseInt(fields[2]),
                fields[3], clip);
    }
}
