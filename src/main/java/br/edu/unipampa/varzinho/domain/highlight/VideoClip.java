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

    /**
     * Records a clip under the file name its camera chose. The size is the camera's
     * estimate, never a measurement of the file.
     *
     * @param filePath the file the clip belongs in
     * @param durationSeconds how long the clip runs, at least one second
     * @param resolution the resolution the clip was recorded at
     * @param sizeMb the estimated size, in megabytes
     * @throws IllegalArgumentException if the path is blank, the duration is not
     *         positive, the resolution is missing or the size is negative
     */
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

    /** {@return the clip's file path, which only a stream camera's clip fills on disk} */
    public String getFilePath() {
        return filePath;
    }

    /** {@return how long the clip runs, in seconds, always at least one} */
    public int getDurationSeconds() {
        return durationSeconds;
    }

    /** {@return the resolution the clip was recorded at} */
    public Resolution getResolution() {
        return resolution;
    }

    /** {@return the estimated size of the clip, in megabytes, never negative} */
    public double getSizeMb() {
        return sizeMb;
    }
}
