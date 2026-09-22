package br.edu.unipampa.varzinho.domain.capture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.edu.unipampa.varzinho.exception.EmptyBufferException;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

class CircularBufferTest {

    @Test
    void isEmptyBeforeAnythingIsRecorded() {
        CircularBuffer buffer = new CircularBuffer(3);

        assertTrue(buffer.isEmpty());
    }

    @Test
    void growsWithEachFrameRecordedWhileBelowCapacity() {
        CircularBuffer buffer = bufferOf(3, 1, 2);

        assertEquals(2, buffer.size());
    }

    @Test
    void reportsItselfFullOnceCapacityIsReached() {
        CircularBuffer buffer = bufferOf(3, 1, 2, 3);

        assertTrue(buffer.isFull());
    }

    @Test
    void staysAtCapacityWhenRecordingIntoAFullBuffer() {
        CircularBuffer buffer = bufferOf(3, 1, 2, 3);

        buffer.record(frame(4));

        assertEquals(3, buffer.size());
    }

    @Test
    void discardsTheOldestFrameWhenRecordingIntoAFullBuffer() {
        CircularBuffer buffer = bufferOf(3, 1, 2, 3);

        buffer.record(frame(4));

        assertFalse(buffer.extractLastSeconds(3).contains(frame(1)));
    }

    @Test
    void keepsTheMostRecentFramesInOrderAfterOverwriting() {
        CircularBuffer buffer = bufferOf(3, 1, 2, 3);

        buffer.record(frame(4));

        assertEquals(List.of(frame(2), frame(3), frame(4)), buffer.extractLastSeconds(3));
    }

    @Test
    void holdsOnlyTheLastThreeOfTenFramesWhenCapacityIsThree() {
        CircularBuffer buffer = bufferOf(3, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10);

        assertEquals(List.of(frame(8), frame(9), frame(10)), buffer.extractLastSeconds(3));
    }

    @Test
    void refusesToExtractFromAnEmptyBuffer() {
        CircularBuffer buffer = new CircularBuffer(3);

        assertThrows(EmptyBufferException.class, () -> buffer.extractLastSeconds(1));
    }

    @Test
    void refusesToExtractMoreSecondsThanItHasRecorded() {
        CircularBuffer buffer = bufferOf(5, 1, 2);

        assertThrows(EmptyBufferException.class, () -> buffer.extractLastSeconds(3));
    }

    @Test
    void returnsEveryFrameWhenTheWindowIsExactlyWhatIsRecorded() {
        CircularBuffer buffer = bufferOf(5, 1, 2);

        assertEquals(List.of(frame(1), frame(2)), buffer.extractLastSeconds(2));
    }

    @Test
    void returnsOnlyTheMostRecentFramesWhenTheWindowIsShorterThanWhatIsRecorded() {
        CircularBuffer buffer = bufferOf(5, 1, 2, 3, 4);

        assertEquals(List.of(frame(3), frame(4)), buffer.extractLastSeconds(2));
    }

    @Test
    void keepsItsContentSoTheSameSecondsCanBeExtractedTwice() {
        CircularBuffer buffer = bufferOf(3, 1, 2, 3);

        buffer.extractLastSeconds(3);

        assertEquals(List.of(frame(1), frame(2), frame(3)), buffer.extractLastSeconds(3));
    }

    @Test
    void goesOnRecordingOverAWindowAlreadyExtracted() {
        CircularBuffer buffer = bufferOf(3, 1, 2, 3);
        buffer.extractLastSeconds(3);

        buffer.record(frame(4));

        assertEquals(List.of(frame(2), frame(3), frame(4)), buffer.extractLastSeconds(3));
    }

    @Test
    void rejectsAWindowOfZeroSeconds() {
        CircularBuffer buffer = bufferOf(3, 1, 2, 3);

        assertThrows(IllegalArgumentException.class, () -> buffer.extractLastSeconds(0));
    }

    @Test
    void rejectsAWindowOfNegativeSeconds() {
        CircularBuffer buffer = bufferOf(3, 1, 2, 3);

        assertThrows(IllegalArgumentException.class, () -> buffer.extractLastSeconds(-1));
    }

    @Test
    void rejectsACapacityOfZero() {
        assertThrows(IllegalArgumentException.class, () -> new CircularBuffer(0));
    }

    @Test
    void rejectsANegativeCapacity() {
        assertThrows(IllegalArgumentException.class, () -> new CircularBuffer(-1));
    }

    private CircularBuffer bufferOf(int capacitySeconds, int... sequences) {
        CircularBuffer buffer = new CircularBuffer(capacitySeconds);
        for (int sequence : sequences) {
            buffer.record(frame(sequence));
        }
        return buffer;
    }

    /**
     * One frame per second, numbered from the same value as its instant, so a frame
     * built twice is the same frame and a window can be compared by value.
     */
    private Frame frame(int sequence) {
        return new Frame(Instant.ofEpochSecond(sequence), sequence);
    }
}
