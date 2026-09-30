package br.edu.unipampa.varzinho.domain.highlight;

import br.edu.unipampa.varzinho.enums.Resolution;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * The record of a saved clip file: where it lives, how long it runs, what it was
 * recorded at and how much room it takes.
 *
 * <p>Not the footage. No byte of the file is ever read — the only question this
 * model asks the disk is whether the file is still there.
 */
public final class VideoClip {

    private final String filePath;
    private final int durationSeconds;
    private final Resolution resolution;
    private final double sizeMb;

    public VideoClip(String filePath, int durationSeconds, Resolution resolution, double sizeMb) {
        if (filePath == null || filePath.isBlank()) {
            throw new IllegalArgumentException("a clip needs the path of its file");
        }
        if (durationSeconds <= 0) {
            throw new IllegalArgumentException(
                    "a clip runs for at least one second, not " + durationSeconds);
        }
        if (resolution == null) {
            throw new IllegalArgumentException("a clip needs the resolution it was recorded at");
        }
        if (sizeMb < 0) {
            throw new IllegalArgumentException("a clip cannot take up less than nothing: " + sizeMb);
        }
        this.filePath = filePath;
        this.durationSeconds = durationSeconds;
        this.resolution = resolution;
        this.sizeMb = sizeMb;
    }

    /**
     * Whether the file this clip describes is still on disk. A clip outlives its
     * file: the archive keeps the record, and somebody may have deleted the footage.
     *
     * @return {@code true} while the path points at an existing file
     */
    public boolean exists() {
        return Files.exists(Path.of(filePath));
    }

    public String getFilePath() {
        return filePath;
    }

    public int getDurationSeconds() {
        return durationSeconds;
    }

    public Resolution getResolution() {
        return resolution;
    }

    public double getSizeMb() {
        return sizeMb;
    }
}
