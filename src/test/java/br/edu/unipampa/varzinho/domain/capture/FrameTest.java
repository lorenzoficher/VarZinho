package br.edu.unipampa.varzinho.domain.capture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class FrameTest {

    @Test
    void isEqualToAFrameCapturedAtTheSameInstantAndPosition() {
        Frame frame = new Frame(Instant.ofEpochSecond(7), 7);
        Frame same = new Frame(Instant.ofEpochSecond(7), 7);

        assertEquals(frame, same);
        assertEquals(frame.hashCode(), same.hashCode());
    }

    @Test
    void differsFromAFrameAtAnotherPositionInTheSequence() {
        Frame frame = new Frame(Instant.ofEpochSecond(7), 7);
        Frame later = new Frame(Instant.ofEpochSecond(7), 8);

        assertNotEquals(frame, later);
    }

    @Test
    void rejectsAMissingTimestamp() {
        assertThrows(IllegalArgumentException.class, () -> new Frame(null, 1));
    }
}
