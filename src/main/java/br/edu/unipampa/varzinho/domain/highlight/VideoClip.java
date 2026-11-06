package br.edu.unipampa.varzinho.domain.highlight;

import br.edu.unipampa.varzinho.enums.Resolution;

/**
 * The record of a saved clip file: where it lives, how long it runs, what it was
 * recorded at and how much room it takes.
 *
 * <p>Not the footage, and not a window onto the disk: the domain never asks the file
 * system anything. Whether the file is still there is a question for the layers
 * that already touch files, because the archive keeps the record even after
 * somebody deletes the footage.
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
