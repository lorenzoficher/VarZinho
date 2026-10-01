package br.edu.unipampa.varzinho.ui;

import br.edu.unipampa.varzinho.domain.capture.Frame;
import br.edu.unipampa.varzinho.domain.structure.Court;
import br.edu.unipampa.varzinho.domain.structure.Gym;

import java.time.Instant;

/**
 * The gym's clock as the window sees it: every tick is one second of footage handed
 * to every court at once, except a court whose footage comes from somewhere else.
 *
 * <p>It holds no capture rule. Whether a camera keeps the second is the camera's
 * decision; this class only produces the seconds, so the window can drive it from a
 * Swing timer and a test can drive it from fixed instants.
 */
final class LiveFeed implements Feed {

    private static final int ONE_SECOND_MILLIS = 1000;

    private final Gym gym;
    private final Court leftOut;
    private int sequence;

    LiveFeed(Gym gym) {
        if (gym == null) throw new IllegalArgumentException("a live feed needs a gym");
        this.gym = gym;
        this.leftOut = null;
    }

    /**
     * A feed for every court but {@code leftOut}, which another feed records. Courts
     * added to the gym later are fed too.
     */
    LiveFeed(Gym gym, Court leftOut) {
        if (gym == null) throw new IllegalArgumentException("a live feed needs a gym");
        if (leftOut == null) throw new IllegalArgumentException("a live feed needs the court it leaves out");
        this.gym = gym;
        this.leftOut = leftOut;
    }

    @Override
    public int periodMillis() {
        return ONE_SECOND_MILLIS;
    }

    /**
     * Records one second, captured at {@code now}, into every court of the gym it feeds.
     */
    @Override
    public void tick(Instant now) {
        Frame frame = new Frame(now, sequence++);
        for (Court court : gym.getCourts()) {
            if (court != leftOut) {
                court.record(frame);
            }
        }
    }

    /**
     * @return how many seconds the court could capture from right now, as the court
     *     itself counts them
     */
    int secondsHeld(Court court) {
        return court.secondsRecorded();
    }
}
