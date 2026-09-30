package br.edu.unipampa.varzinho.stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.edu.unipampa.varzinho.domain.capture.Frame;
import br.edu.unipampa.varzinho.exception.ClipAssemblyException;

import java.nio.file.Path;
import java.time.Instant;

import org.junit.jupiter.api.Test;

class SegmentIndexTest {

    @Test
    void findsTheSegmentRegisteredForAFrame() {
        SegmentIndex index = new SegmentIndex(40);
        index.register(7, Path.of("seg-06.ts"));

        assertEquals(Path.of("seg-06.ts"), index.segmentOf(frame(7)));
    }

    @Test
    void refusesAFrameItNeverSawNamingItsSequence() {
        SegmentIndex index = new SegmentIndex(40);

        ClipAssemblyException thrown =
                assertThrows(ClipAssemblyException.class, () -> index.segmentOf(frame(12)));

        assertTrue(thrown.getMessage().contains("12"));
    }

    @Test
    void forgetsSegmentsTheRingHasAlreadyOverwritten() {
        SegmentIndex index = new SegmentIndex(40);
        for (int sequence = 1; sequence <= 41; sequence++) {
            index.register(sequence, Path.of("seg-" + (sequence - 1) % 40 + ".ts"));
        }

        assertThrows(ClipAssemblyException.class, () -> index.segmentOf(frame(1)));
        assertEquals(Path.of("seg-0.ts"), index.segmentOf(frame(41)));
    }

    @Test
    void refusesARingWithNoRoom() {
        assertThrows(IllegalArgumentException.class, () -> new SegmentIndex(0));
    }

    private static Frame frame(int sequence) {
        return new Frame(Instant.ofEpochSecond(sequence), sequence);
    }
}
