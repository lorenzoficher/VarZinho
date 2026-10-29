package br.edu.unipampa.varzinho.stream;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
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

    private static final int CHUNK_BYTES = 8192;

    private final Path listFile;
    private long bytesConsumed;

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
        byte[] appended = readAppended();
        int finishedLength = lastLineBreak(appended) + 1;
        bytesConsumed += finishedLength;
        String finished = new String(appended, 0, finishedLength, StandardCharsets.UTF_8);
        List<Path> segments = new ArrayList<>();
        for (String line : finished.split("\n")) {
            if (!line.isBlank()) {
                segments.add(segmentOn(line));
            }
        }
        return segments;
    }

    private byte[] readAppended() throws IOException {
        try (SeekableByteChannel channel = Files.newByteChannel(listFile)) {
            channel.position(bytesConsumed);
            ByteArrayOutputStream appended = new ByteArrayOutputStream();
            ByteBuffer chunk = ByteBuffer.allocate(CHUNK_BYTES);
            while (channel.read(chunk) > 0) {
                appended.write(chunk.array(), 0, chunk.position());
                chunk.clear();
            }
            return appended.toByteArray();
        }
    }

    // A line break is a single byte in UTF-8 and never part of a wider character, so
    // cutting the bytes there never splits a file name.
    private static int lastLineBreak(byte[] bytes) {
        for (int index = bytes.length - 1; index >= 0; index--) {
            if (bytes[index] == '\n') {
                return index;
            }
        }
        return -1;
    }

    // FFmpeg may write the name with the folder it was given; every segment lives
    // next to the list anyway, so only the file name is trusted.
    private Path segmentOn(String line) {
        String name = line.strip().split(",", 2)[0];
        return listFile.resolveSibling(Path.of(name).getFileName());
    }
}
