package br.edu.unipampa.varzinho.domain.highlight;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.edu.unipampa.varzinho.enums.Resolution;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class VideoClipTest {

    @TempDir
    private Path archive;

    @Test
    void reportsItselfPresentWhileItsFileIsOnDisk() throws IOException {
        Path file = Files.createFile(archive.resolve("goal.mp4"));

        VideoClip clip = clipAt(file.toString());

        assertTrue(clip.exists());
    }

    @Test
    void reportsItselfAbsentOnceItsFileIsDeleted() throws IOException {
        Path file = Files.createFile(archive.resolve("goal.mp4"));
        VideoClip clip = clipAt(file.toString());

        Files.delete(file);

        assertFalse(clip.exists());
    }

    @Test
    void rejectsABlankPath() {
        assertThrows(IllegalArgumentException.class, () -> clipAt("   "));
    }

    @Test
    void rejectsAMissingPath() {
        assertThrows(IllegalArgumentException.class, () -> clipAt(null));
    }

    @Test
    void rejectsAClipThatLastsNoTime() {
        assertThrows(IllegalArgumentException.class,
                () -> new VideoClip("goal.mp4", 0, Resolution.FULL_HD, 12.5));
    }

    @Test
    void rejectsANegativeDuration() {
        assertThrows(IllegalArgumentException.class,
                () -> new VideoClip("goal.mp4", -30, Resolution.FULL_HD, 12.5));
    }

    @Test
    void rejectsANegativeSize() {
        assertThrows(IllegalArgumentException.class,
                () -> new VideoClip("goal.mp4", 30, Resolution.FULL_HD, -0.1));
    }

    @Test
    void rejectsAMissingResolution() {
        assertThrows(IllegalArgumentException.class,
                () -> new VideoClip("goal.mp4", 30, null, 12.5));
    }

    private static VideoClip clipAt(String filePath) {
        return new VideoClip(filePath, 30, Resolution.FULL_HD, 12.5);
    }
}
