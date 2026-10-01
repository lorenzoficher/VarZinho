package br.edu.unipampa.varzinho.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.edu.unipampa.varzinho.domain.capture.FixedCamera;
import br.edu.unipampa.varzinho.domain.capture.Frame;
import br.edu.unipampa.varzinho.domain.capture.StreamCamera;
import br.edu.unipampa.varzinho.domain.structure.Court;
import br.edu.unipampa.varzinho.enums.Resolution;
import br.edu.unipampa.varzinho.exception.NoActiveCameraException;
import br.edu.unipampa.varzinho.stream.SegmentIndex;
import br.edu.unipampa.varzinho.stream.SegmentLog;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SegmentFeedTest {

    private static final Instant NOW = Instant.parse("2026-09-30T22:00:00Z");

    @TempDir
    Path buffer;

    private final SegmentIndex index = new SegmentIndex(40);
    private final List<Path> clipsWritten = new ArrayList<>();
    private final StreamCamera phone = new StreamCamera("phone-1", "Phone", Resolution.HD, 30,
            "http://192.168.0.42:8080/video", (window, target) -> clipsWritten.add(target));
    private final Court phoneCourt = new Court(1);
    private int silences;

    SegmentFeedTest() {
        phoneCourt.installCamera(phone);
        phone.startRecording();
    }

    @Test
    void recordsEachFinishedSegmentOnlyIntoThePhone() throws IOException {
        Court simulated = new Court(2);
        FixedCamera fixed = new FixedCamera("camera-2", "test camera", Resolution.FULL_HD, 30, 45);
        simulated.installCamera(fixed);
        fixed.startRecording();
        SegmentFeed feed = feed();
        Files.writeString(list(), "seg-00.ts,0,1\nseg-01.ts,1,2\nseg-02.ts,2,3\n");

        feed.tick(NOW);

        assertEquals(3, phoneCourt.secondsRecorded());
        assertEquals(0, simulated.secondsRecorded());
    }

    @Test
    void remembersWhichSegmentHoldsEachSecond() throws IOException {
        SegmentFeed feed = feed();
        Files.writeString(list(), "seg-00.ts,0,1\nseg-01.ts,1,2\n");

        feed.tick(NOW);

        assertEquals(buffer.resolve("seg-01.ts"), index.segmentOf(new Frame(NOW, 1)));
    }

    @Test
    void recordsNothingUntilTheFirstSegmentIsFinished() throws IOException {
        SegmentFeed feed = feed();

        feed.tick(NOW);

        assertEquals(0, phoneCourt.secondsRecorded());
    }

    @Test
    void keepsThePhoneRecordingJustUnderTheSilenceLimit() throws IOException {
        SegmentFeed feed = feed();
        Files.writeString(list(), "seg-00.ts,0,1\n");
        feed.tick(NOW);

        feed.tick(NOW.plus(SegmentFeed.SILENCE_LIMIT).minusMillis(1));

        assertTrue(phone.isRecording());
        assertEquals(0, silences);
    }

    @Test
    void stopsThePhoneWhenNoSegmentArrivesForTheSilenceLimit() throws IOException {
        SegmentFeed feed = feed();
        Files.writeString(list(), "seg-00.ts,0,1\n");
        feed.tick(NOW);

        feed.tick(NOW.plus(SegmentFeed.SILENCE_LIMIT));

        assertFalse(phone.isRecording());
    }

    @Test
    void stopsAPhoneThatNeverSentASegment() throws IOException {
        SegmentFeed feed = feed();
        feed.tick(NOW);

        feed.tick(NOW.plus(SegmentFeed.SILENCE_LIMIT));

        assertFalse(phone.isRecording());
    }

    @Test
    void aNewSegmentRestartsTheSilenceCount() throws IOException {
        SegmentFeed feed = feed();
        feed.tick(NOW);
        Files.writeString(list(), "seg-00.ts,0,1\n");
        feed.tick(NOW.plus(SegmentFeed.SILENCE_LIMIT).minusSeconds(1));

        feed.tick(NOW.plus(SegmentFeed.SILENCE_LIMIT));

        assertTrue(phone.isRecording());
    }

    @Test
    void reportsTheSilenceOnce() throws IOException {
        SegmentFeed feed = feed();
        feed.tick(NOW);

        feed.tick(NOW.plus(SegmentFeed.SILENCE_LIMIT));
        feed.tick(NOW.plus(SegmentFeed.SILENCE_LIMIT).plusSeconds(1));

        assertEquals(1, silences);
    }

    @Test
    void aCaptureAfterTheSilenceFindsNoActiveCameraAndWritesNoClip() throws IOException {
        SegmentFeed feed = feed();
        Files.writeString(list(), "seg-00.ts,0,1\n");
        feed.tick(NOW);

        feed.tick(NOW.plus(SegmentFeed.SILENCE_LIMIT));

        assertThrows(NoActiveCameraException.class, phoneCourt::triggerCapture);
        assertTrue(clipsWritten.isEmpty());
    }

    private SegmentFeed feed() {
        return new SegmentFeed(phone, new SegmentLog(list()), index, () -> silences++);
    }

    private Path list() {
        return buffer.resolve("segments.csv");
    }
}
