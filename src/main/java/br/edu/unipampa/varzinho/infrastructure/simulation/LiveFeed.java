package br.edu.unipampa.varzinho.infrastructure.simulation;

import br.edu.unipampa.varzinho.application.Feed;
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
public final class LiveFeed implements Feed {

    private static final int ONE_SECOND_MILLIS = 1000;
    private static final int NO_COURT = 0;

    private final Gym gym;
    private final int leftOutCourtNumber;
    private int sequence;

    public LiveFeed(Gym gym) {
        if (gym == null) throw new IllegalArgumentException("a live feed needs a gym");
        this.gym = gym;
        // Court numbers start at 1, so no court is ever left out.
        this.leftOutCourtNumber = NO_COURT;
    }

    /**
     * A feed for every court but the one numbered {@code leftOutCourtNumber}, which
     * another feed records. Courts added to the gym later are fed too.
     */
    public LiveFeed(Gym gym, int leftOutCourtNumber) {
        if (gym == null) throw new IllegalArgumentException("a live feed needs a gym");
        if (leftOutCourtNumber <= NO_COURT) {
            throw new IllegalArgumentException("a live feed cannot leave out court " + leftOutCourtNumber);
        }
        this.gym = gym;
        this.leftOutCourtNumber = leftOutCourtNumber;
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
            if (court.getNumber() != leftOutCourtNumber) {
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
