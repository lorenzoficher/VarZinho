package br.edu.unipampa.varzinho.ui;

import br.edu.unipampa.varzinho.domain.capture.Frame;
import br.edu.unipampa.varzinho.domain.structure.Court;
import br.edu.unipampa.varzinho.domain.structure.Gym;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * The gym's clock as the window sees it: every tick is one second of footage handed
 * to every court at once.
 *
 * <p>It holds no capture rule. Whether a camera keeps the second is the camera's
 * decision; this class only produces the seconds, so the window can drive it from a
 * Swing timer and a test can drive it from fixed instants.
 */
final class LiveFeed {

    private final Gym gym;
    private final int bufferSeconds;
    private final Map<Integer, Integer> secondsHeldByCourt = new HashMap<>();
    private int sequence;

    LiveFeed(Gym gym, int bufferSeconds) {
        if (gym == null) throw new IllegalArgumentException("a live feed needs a gym");
        if (bufferSeconds <= 0) {
            throw new IllegalArgumentException("a buffer holds at least one second, not " + bufferSeconds);
        }
        this.gym = gym;
        this.bufferSeconds = bufferSeconds;
    }

    /**
     * Records one second, captured at {@code now}, into every court of the gym.
     */
    void tick(Instant now) {
        Frame frame = new Frame(now, sequence++);
        for (Court court : gym.getCourts()) {
            court.record(frame);
            // The domain keeps its buffer size to itself, so the window counts the
            // seconds it handed to a court that could keep them.
            if (court.hasActiveCamera()) {
                secondsHeldByCourt.merge(court.getNumber(), 1,
                        (held, added) -> Math.min(held + added, bufferSeconds));
            }
        }
    }

    /**
     * @return how many seconds the court's buffer holds, never more than its capacity
     */
    int secondsHeld(Court court) {
        return secondsHeldByCourt.getOrDefault(court.getNumber(), 0);
    }
}
