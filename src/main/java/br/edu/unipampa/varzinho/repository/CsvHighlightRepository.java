package br.edu.unipampa.varzinho.repository;

import br.edu.unipampa.varzinho.domain.highlight.Highlight;
import br.edu.unipampa.varzinho.domain.highlight.VideoClip;
import br.edu.unipampa.varzinho.enums.Resolution;
import br.edu.unipampa.varzinho.exception.CorruptedRecordException;
import br.edu.unipampa.varzinho.exception.RepositoryException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class CsvHighlightRepository implements HighlightRepository {
    private static final String HEADER = "id,capturedAt,courtNumber,cameraId,clipPath,durationSeconds,resolution,sizeMb";
    private final Path file;

    public CsvHighlightRepository(Path file) throws RepositoryException {
        if (file == null) throw new IllegalArgumentException("an archive needs a file");
        this.file = file;
        initialise();
    }

    private void initialise() throws RepositoryException {
        try {
            Path parent = file.toAbsolutePath().getParent();
            if (parent != null) Files.createDirectories(parent);
            if (Files.notExists(file)) Files.writeString(file, HEADER + System.lineSeparator(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new RepositoryException("Could not initialise archive " + file, exception);
        }
    }

    @Override
    public void save(Highlight highlight) throws RepositoryException {
        if (highlight == null) throw new IllegalArgumentException("cannot save a null highlight");
        try {
            Files.writeString(file, encode(highlight) + System.lineSeparator(), StandardCharsets.UTF_8,
                    StandardOpenOption.APPEND);
        } catch (IOException exception) {
            throw new RepositoryException("Could not save highlight " + highlight.getId(), exception);
        }
    }

    @Override
    public Optional<Highlight> findById(String id) throws RepositoryException {
        return findAll().stream().filter(item -> item.getId().equals(id)).findFirst();
    }

    @Override
    public List<Highlight> findAll() throws RepositoryException {
        try {
            List<Highlight> highlights = new ArrayList<>();
            List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            for (int index = 1; index < lines.size(); index++) {
                if (!lines.get(index).isBlank()) highlights.add(decode(lines.get(index), index + 1));
            }
            return List.copyOf(highlights);
        } catch (IOException exception) {
            throw new RepositoryException("Could not read archive " + file, exception);
        }
    }

    @Override
    public List<Highlight> findByCourt(int courtNumber) throws RepositoryException {
        return findAll().stream().filter(item -> item.getCourtNumber() == courtNumber).toList();
    }

    private String encode(Highlight item) {
        VideoClip clip = item.getClip();
        return String.join(",", item.getId(), item.getCapturedAt().toString(),
                Integer.toString(item.getCourtNumber()), item.getCameraId(), clip.getFilePath(),
                Integer.toString(clip.getDurationSeconds()), clip.getResolution().name(),
                Double.toString(clip.getSizeMb()));
    }

    private Highlight decode(String line, int lineNumber) throws CorruptedRecordException {
        try {
            String[] value = line.split(",", -1);
            if (value.length != 8) throw new IllegalArgumentException("expected 8 columns");
            VideoClip clip = new VideoClip(value[4], Integer.parseInt(value[5]),
                    Resolution.valueOf(value[6]), Double.parseDouble(value[7]));
            return new Highlight(value[0], Instant.parse(value[1]), Integer.parseInt(value[2]), value[3], clip);
        } catch (IllegalArgumentException exception) {
            throw new CorruptedRecordException("Corrupted archive record at line " + lineNumber, exception);
        }
    }
}
