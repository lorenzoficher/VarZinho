package br.edu.unipampa.varzinho.stream;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * The list FFmpeg appends to each time it finishes a segment, read as it grows.
 *
 * <p>One new line is one new second of footage: this is the clock the recorder keeps,
 * so nothing on the Java side has to guess when a second ended.
 */
public final class SegmentLog {

    private final Path listFile;
    private int linesRead;

    public SegmentLog(Path listFile) {
        if (listFile == null) {
            throw new IllegalArgumentException("a segment log needs the file FFmpeg writes");
        }
        this.listFile = listFile;
    }

    /**
     * The segments finished since the previous call, oldest first.
     *
     * <p>A line without its line break is still being written and waits for the next
     * call. A list that does not exist yet means the recorder has not finished its first
     * segment, not that something failed.
     *
     * @return the new segment files, each resolved next to the list; empty when there
     *         are none
     * @throws IOException if the list exists but cannot be read
     */
    public List<Path> readNew() throws IOException {
        if (!Files.exists(listFile)) {
            return List.of();
        }
        String content = Files.readString(listFile, StandardCharsets.UTF_8);
        String[] finished = content.substring(0, content.lastIndexOf('\n') + 1).split("\n");
        List<Path> segments = new ArrayList<>();
        for (int line = linesRead; line < finished.length; line++) {
            if (!finished[line].isBlank()) {
                segments.add(segmentOn(finished[line]));
            }
        }
        linesRead = Math.max(linesRead, finished.length);
        return segments;
    }

    // FFmpeg may write the name with the folder it was given; every segment lives
    // next to the list anyway, so only the file name is trusted.
    private Path segmentOn(String line) {
        String name = line.strip().split(",", 2)[0];
        return listFile.resolveSibling(Path.of(name).getFileName());
    }
}
