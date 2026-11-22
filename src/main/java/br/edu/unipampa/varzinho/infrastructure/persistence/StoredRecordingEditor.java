package br.edu.unipampa.varzinho.infrastructure.persistence;

import br.edu.unipampa.varzinho.application.port.HighlightRepository;
import br.edu.unipampa.varzinho.application.port.RecordingEditor;
import br.edu.unipampa.varzinho.application.port.RepositoryException;
import br.edu.unipampa.varzinho.domain.highlight.Highlight;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;

public final class StoredRecordingEditor implements RecordingEditor {
    private final HighlightRepository repository;
    private final Path directory;

    public StoredRecordingEditor(HighlightRepository repository, Path directory) {
        this.repository = java.util.Objects.requireNonNull(repository);
        this.directory = directory.toAbsolutePath().normalize();
    }

    @Override public void rename(String id, String title) throws RepositoryException {
        repository.rename(id, title);
    }

    @Override public synchronized void delete(String id) throws RepositoryException {
        var entries = repository.findAll();
        Highlight item = entries.stream().filter(entry -> entry.getId().equals(id)).findFirst().orElse(null);
        if (item == null) return;
        try {
            Path clip = Path.of(item.getClip().getFilePath()).toAbsolutePath().normalize();
            if (!directory.equals(clip.getParent()) || !clip.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".mp4"))
                throw new RepositoryException("clip outside the managed video directory");
            if (Files.notExists(clip, LinkOption.NOFOLLOW_LINKS)) {
                repository.delete(id);
                return;
            }
            if (!Files.isRegularFile(clip, LinkOption.NOFOLLOW_LINKS) || !clip.equals(clip.toRealPath()))
                throw new RepositoryException("clip is not a local regular video");
            for (Highlight other : entries) {
                if (id.equals(other.getId())) continue;
                Path otherFile = Path.of(other.getClip().getFilePath());
                if (Files.exists(otherFile) && Files.isSameFile(clip, otherFile))
                    throw new RepositoryException("clip is shared with another recording");
            }
            removeFileAndEntry(item, clip);
        } catch (IOException | java.nio.file.InvalidPathException failure) {
            throw new RepositoryException("could not delete the recording", failure);
        }
    }

    private void removeFileAndEntry(Highlight item, Path clip) throws IOException, RepositoryException {
        // Keep the video recoverable until both the catalog change and the unlink succeed.
        Path staged = Files.createTempFile(directory, ".removing-", ".tmp");
        try {
            Files.move(clip, staged, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException failure) {
            Files.deleteIfExists(staged);
            throw failure;
        }
        try {
            repository.delete(item.getId());
        } catch (RepositoryException failure) {
            Files.move(staged, clip);
            throw failure;
        }
        try {
            Files.delete(staged);
        } catch (IOException failure) {
            Files.move(staged, clip);
            repository.save(item);
            throw failure;
        }
    }
}
