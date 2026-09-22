package br.edu.unipampa.varzinho.domain.capture;

import java.util.ArrayList;
import java.util.List;

/**
 * Fixed-size memory holding the most recent seconds of footage from one camera.
 *
 * <p>Writing past the end wraps around and overwrites the oldest frame. Nothing is
 * ever explicitly removed — this is not a queue.
 */
public final class CircularBuffer {

    private final Frame[] frames;
    private int writePosition;
    private int size;

    public CircularBuffer(int capacitySeconds) {
        if (capacitySeconds <= 0) {
            throw new IllegalArgumentException(
                    "a buffer holds at least one second, not " + capacitySeconds);
        }
        this.frames = new Frame[capacitySeconds];
    }

    public void record(Frame frame) {
        frames[writePosition] = frame;
        writePosition = (writePosition + 1) % frames.length;
        if (size < frames.length) {
            size++;
        }
    }

    /**
     * Reads the most recent window without consuming it. A camera keeps recording
     * over the frames just handed out, which is what lets the same seconds be
     * captured twice.
     *
     * @param seconds how many of the most recent frames to return, oldest first
     * @return the window, unmodifiable
     */
    public List<Frame> extractLastSeconds(int seconds) {
        List<Frame> window = new ArrayList<>(seconds);
        int oldest = Math.floorMod(writePosition - seconds, frames.length);
        for (int offset = 0; offset < seconds; offset++) {
            window.add(frames[(oldest + offset) % frames.length]);
        }
        return List.copyOf(window);
    }

    public int size() {
        return size;
    }

    public boolean isFull() {
        return size == frames.length;
    }

    public boolean isEmpty() {
        return size == 0;
    }
}
