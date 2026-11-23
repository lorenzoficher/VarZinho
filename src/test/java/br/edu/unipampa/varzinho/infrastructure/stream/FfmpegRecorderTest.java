package br.edu.unipampa.varzinho.infrastructure.stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

class FfmpegRecorderTest {

    private static final String URL = "http://192.168.0.42:8080/video";
    private static final Path BUFFER = Path.of("buffer");
    private static final int RELAY_PORT = 47123;

    @Test
    void readsTheStreamItWasGiven() {
        List<String> command = new FfmpegRecorder(URL, BUFFER, RELAY_PORT).command();

        assertEquals(URL, command.get(command.indexOf("-i") + 1));
    }

    @Test
    void givesUpOnAStreamThatStopsSendingInsteadOfWaitingForever() {
        List<String> command = new FfmpegRecorder(URL, BUFFER, RELAY_PORT).command();

        int timeout = command.indexOf("-rw_timeout");
        assertTrue(timeout >= 0 && timeout < command.indexOf("-i"));
        assertEquals(String.valueOf(FfmpegRecorder.READ_TIMEOUT_MICROSECONDS), command.get(timeout + 1));
    }

    @Test
    void writesTheRingAndItsListByNameSoNoFolderPathNeedsEscaping() {
        String outputs = outputsOf(new FfmpegRecorder(URL, BUFFER, RELAY_PORT).command());

        assertTrue(outputs.contains("segment_list=segments.csv:"), outputs);
        assertTrue(outputs.contains("]seg-%02d.ts|"), outputs);
    }

    @Test
    void keepsTheRingTheIndexIsSizedFor() {
        String outputs = outputsOf(new FfmpegRecorder(URL, BUFFER, RELAY_PORT).command());

        assertTrue(outputs.contains("segment_wrap=" + FfmpegRecorder.RING_SEGMENTS + ":"), outputs);
    }

    @Test
    void readsThePhoneOnceSinceSomePhoneAppsServeASingleClient() {
        List<String> command = new FfmpegRecorder(URL, BUFFER, RELAY_PORT).command();

        assertEquals(1, Collections.frequency(command, "-i"));
    }

    @Test
    void relaysTheVideoItRecordsOnTheLoopbackForTheBroadcast() {
        String outputs = outputsOf(new FfmpegRecorder(URL, BUFFER, RELAY_PORT).command());

        assertTrue(outputs.endsWith("|[f=mpegts:onfail=ignore]udp://127.0.0.1:" + RELAY_PORT + "?pkt_size=1316"),
                outputs);
    }

    @Test
    void theRelayReaderGivesUpWhenTheRecorderStopsSending() {
        String relay = new FfmpegRecorder(URL, BUFFER, RELAY_PORT).liveRelay();

        assertTrue(relay.startsWith("udp://127.0.0.1:" + RELAY_PORT + "?"), relay);
        assertTrue(relay.contains("timeout=" + FfmpegRecorder.READ_TIMEOUT_MICROSECONDS), relay);
    }

    @Test
    void refusesARelayPortOutsideTheValidRange() {
        assertThrows(IllegalArgumentException.class, () -> new FfmpegRecorder(URL, BUFFER, 0));
        assertThrows(IllegalArgumentException.class, () -> new FfmpegRecorder(URL, BUFFER, 65_536));
    }

    @Test
    void refusesAnAddressWithoutItsProtocol() {
        assertThrows(IllegalArgumentException.class,
                () -> new FfmpegRecorder("10.2.168.102:4747/video", BUFFER, RELAY_PORT));
    }

    @Test
    void refusesAnAddressWithoutAHost() {
        assertThrows(IllegalArgumentException.class, () -> new FfmpegRecorder("http:///video", BUFFER, RELAY_PORT));
    }

    @Test
    void acceptsAnRtspStream() {
        String rtsp = "rtsp://192.168.0.42:8554/live";

        List<String> command = new FfmpegRecorder(rtsp, BUFFER, RELAY_PORT).command();

        assertEquals(rtsp, command.get(command.indexOf("-i") + 1));
    }

    private static String outputsOf(List<String> command) {
        assertEquals("tee", command.get(command.size() - 2));
        return command.get(command.size() - 1);
    }

    @Test
    void refusesABlankStreamUrl() {
        assertThrows(IllegalArgumentException.class, () -> new FfmpegRecorder(" ", BUFFER, RELAY_PORT));
    }
}
