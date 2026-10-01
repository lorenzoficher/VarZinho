package br.edu.unipampa.varzinho.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import br.edu.unipampa.varzinho.domain.capture.FixedCamera;
import br.edu.unipampa.varzinho.domain.capture.Frame;
import br.edu.unipampa.varzinho.domain.structure.Court;
import br.edu.unipampa.varzinho.domain.structure.Gym;
import br.edu.unipampa.varzinho.enums.Resolution;
import br.edu.unipampa.varzinho.stream.SegmentIndex;
import br.edu.unipampa.varzinho.stream.SegmentLog;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SegmentFeedTest {

    private static final Instant NOW = Instant.parse("2026-09-30T22:00:00Z");

    @TempDir
    Path buffer;

    private final Gym gym = new Gym("Arena", "100 Sports Avenue");
    private final SegmentIndex index = new SegmentIndex(40);

    @Test
    void recordsEachFinishedSegmentOnlyIntoThePhonesCourt() throws IOException {
        Court phoneCourt = courtWithActiveCamera(1);
        Court simulated = courtWithActiveCamera(2);
        SegmentFeed feed = feedFor(phoneCourt);
        Files.writeString(list(), "seg-00.ts,0,1\nseg-01.ts,1,2\nseg-02.ts,2,3\n");

        feed.tick(NOW);

        assertEquals(3, phoneCourt.secondsRecorded());
        assertEquals(0, simulated.secondsRecorded());
    }

    @Test
    void remembersWhichSegmentHoldsEachSecond() throws IOException {
        SegmentFeed feed = feedFor(courtWithActiveCamera(1));
        Files.writeString(list(), "seg-00.ts,0,1\nseg-01.ts,1,2\n");

        feed.tick(NOW);

        assertEquals(buffer.resolve("seg-01.ts"), index.segmentOf(new Frame(NOW, 1)));
    }

    @Test
    void recordsNothingUntilTheFirstSegmentIsFinished() throws IOException {
        Court court = courtWithActiveCamera(1);
        SegmentFeed feed = feedFor(court);

        feed.tick(NOW);

        assertEquals(0, court.secondsRecorded());
    }

    private SegmentFeed feedFor(Court phoneCourt) {
        return new SegmentFeed(phoneCourt, new SegmentLog(list()), index);
    }

    private Path list() {
        return buffer.resolve("segments.csv");
    }

    private Court courtWithActiveCamera(int number) {
        Court court = new Court(number);
        gym.addCourt(court);
        FixedCamera camera = new FixedCamera("camera-" + number, "test camera", Resolution.FULL_HD, 30, 45);
        gym.installCamera(number, camera);
        camera.startRecording();
        return court;
    }
}
