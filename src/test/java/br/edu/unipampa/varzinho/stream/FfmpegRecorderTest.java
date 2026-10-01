package br.edu.unipampa.varzinho.stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

class FfmpegRecorderTest {

    private static final String URL = "http://192.168.0.42:8080/video";
    private static final Path BUFFER = Path.of("buffer");

    @Test
    void readsTheStreamItWasGiven() {
        List<String> command = new FfmpegRecorder(URL, BUFFER).command();

        assertEquals(URL, command.get(command.indexOf("-i") + 1));
    }

    @Test
    void givesUpOnAStreamThatStopsSendingInsteadOfWaitingForever() {
        List<String> command = new FfmpegRecorder(URL, BUFFER).command();

        int timeout = command.indexOf("-rw_timeout");
        assertTrue(timeout >= 0 && timeout < command.indexOf("-i"));
        assertEquals(String.valueOf(FfmpegRecorder.READ_TIMEOUT_MICROSECONDS), command.get(timeout + 1));
    }

    @Test
    void writesTheRingAndItsListInsideTheBufferFolder() {
        List<String> command = new FfmpegRecorder(URL, BUFFER).command();

        assertEquals(BUFFER.resolve("seg-%02d.ts").toString(), command.get(command.size() - 1));
        assertTrue(command.contains(BUFFER.resolve("segments.csv").toString()));
    }

    @Test
    void keepsTheRingTheIndexIsSizedFor() {
        List<String> command = new FfmpegRecorder(URL, BUFFER).command();

        assertEquals(String.valueOf(FfmpegRecorder.RING_SEGMENTS),
                command.get(command.indexOf("-segment_wrap") + 1));
    }

    @Test
    void refusesABlankStreamUrl() {
        assertThrows(IllegalArgumentException.class, () -> new FfmpegRecorder(" ", BUFFER));
    }
}
