package br.edu.unipampa.varzinho.stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SegmentLogTest {

    @TempDir
    Path buffer;

    @Test
    void returnsOnlyTheSegmentsFinishedSinceTheLastRead() throws IOException {
        Path list = buffer.resolve("segments.csv");
        SegmentLog log = new SegmentLog(list);
        append(list, "seg-00.ts,0.000000,1.000000\nseg-01.ts,1.000000,2.000000\n");
        log.readNew();

        append(list, "seg-02.ts,2.000000,3.000000\n");

        assertEquals(List.of(buffer.resolve("seg-02.ts")), log.readNew());
    }

    @Test
    void waitsForALineFfmpegIsStillWriting() throws IOException {
        Path list = buffer.resolve("segments.csv");
        SegmentLog log = new SegmentLog(list);
        append(list, "seg-00.ts,0.000000,1.000000\nseg-01.ts,1.0000");

        List<Path> first = log.readNew();
        append(list, "00,2.000000\n");

        assertEquals(List.of(buffer.resolve("seg-00.ts")), first);
        assertEquals(List.of(buffer.resolve("seg-01.ts")), log.readNew());
    }

    @Test
    void findsNothingBeforeTheRecorderHasWrittenTheList() throws IOException {
        SegmentLog log = new SegmentLog(buffer.resolve("segments.csv"));

        assertTrue(log.readNew().isEmpty());
    }

    @Test
    void placesEverySegmentNextToTheList() throws IOException {
        Path list = buffer.resolve("segments.csv");
        SegmentLog log = new SegmentLog(list);
        append(list, "buffer/seg-07.ts,7.000000,8.000000\n");

        assertEquals(List.of(buffer.resolve("seg-07.ts")), log.readNew());
    }

    private static void append(Path file, String text) throws IOException {
        Files.writeString(file, text, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }
}
