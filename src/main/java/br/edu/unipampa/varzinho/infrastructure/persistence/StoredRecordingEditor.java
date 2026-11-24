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

/**
 * Renames and deletes recordings on disk, keeping the archive and the video files in step.
 *
 * <p>Deleting removes both the catalog entry and the {@code .mp4} it points to, but only a
 * file this application manages: a regular video directly inside the managed directory,
 * shared with no other recording. If any step fails, the video and its entry are put back,
 * so a failed delete never leaves an entry without its file or a file without its entry.
 */
public final class StoredRecordingEditor implements RecordingEditor {
    private final HighlightRepository repository;
    private final Path directory;

    /**
     * Edits the recordings kept by {@code repository} whose videos live in {@code directory}.
     *
     * @param repository the archive the entries are renamed in and deleted from
     * @param directory the only folder whose videos this editor may delete
     * @throws NullPointerException if the repository or the directory is missing
     */
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
