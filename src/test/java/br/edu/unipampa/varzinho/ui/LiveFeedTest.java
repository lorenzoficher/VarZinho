package br.edu.unipampa.varzinho.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import br.edu.unipampa.varzinho.domain.capture.FixedCamera;
import br.edu.unipampa.varzinho.domain.structure.Court;
import br.edu.unipampa.varzinho.domain.structure.Gym;
import br.edu.unipampa.varzinho.enums.Resolution;

class LiveFeedTest {

    private static final int BUFFER_SECONDS = 30;
    private static final Instant START = Instant.parse("2026-09-28T20:00:00Z");

    @Test
    void recordsEachSecondIntoEveryCourtOfTheGym() {
        Gym gym = new Gym("Arena", "100 Sports Avenue");
        Court first = courtWithActiveCamera(1);
        Court second = courtWithActiveCamera(2);
        gym.addCourt(first);
        gym.addCourt(second);
        LiveFeed feed = new LiveFeed(gym, BUFFER_SECONDS);

        tick(feed, Court.DEFAULT_CAPTURE_SECONDS);

        Instant lastSecond = START.plusSeconds(Court.DEFAULT_CAPTURE_SECONDS - 1L);
        assertEquals("clips/camera-1-" + lastSecond.getEpochSecond() + ".mp4",
                first.triggerCapture().getClip().getFilePath());
        assertEquals("clips/camera-2-" + lastSecond.getEpochSecond() + ".mp4",
                second.triggerCapture().getClip().getFilePath());
    }

    @Test
    void secondsHeldGrowWithEachTickUntilTheBufferIsFull() {
        Gym gym = new Gym("Arena", "100 Sports Avenue");
        Court court = courtWithActiveCamera(1);
        gym.addCourt(court);
        LiveFeed feed = new LiveFeed(gym, BUFFER_SECONDS);

        tick(feed, 3);
        assertEquals(3, feed.secondsHeld(court));

        tick(feed, BUFFER_SECONDS);
        assertEquals(BUFFER_SECONDS, feed.secondsHeld(court));
    }

    @Test
    void aCourtWhoseCameraIsOffHoldsNoSeconds() {
        Gym gym = new Gym("Arena", "100 Sports Avenue");
        Court court = new Court(1);
        court.installCamera(new FixedCamera("camera-1", "test camera",
                Resolution.FULL_HD, BUFFER_SECONDS, 45));
        gym.addCourt(court);
        LiveFeed feed = new LiveFeed(gym, BUFFER_SECONDS);

        tick(feed, Court.DEFAULT_CAPTURE_SECONDS);

        assertEquals(0, feed.secondsHeld(court));
    }

    private static Court courtWithActiveCamera(int number) {
        Court court = new Court(number);
        FixedCamera camera = new FixedCamera("camera-" + number, "test camera",
                Resolution.FULL_HD, BUFFER_SECONDS, 45);
        court.installCamera(camera);
        camera.startRecording();
        return court;
    }

    private static void tick(LiveFeed feed, int seconds) {
        for (int second = 0; second < seconds; second++) {
            feed.tick(START.plusSeconds(second));
        }
    }
}
