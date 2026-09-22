package br.edu.unipampa.varzinho.domain.capture;

import br.edu.unipampa.varzinho.exception.EmptyBufferException;

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
     * @throws IllegalArgumentException if the window is not at least one second
     * @throws EmptyBufferException if that many seconds have not been recorded yet
     */
    public List<Frame> extractLastSeconds(int seconds) {
        if (seconds <= 0) {
            throw new IllegalArgumentException(
                    "a window covers at least one second, not " + seconds);
        }
        if (isEmpty()) {
            throw new EmptyBufferException("nothing has been recorded yet");
        }
        if (seconds > size) {
            throw new EmptyBufferException(
                    "a window of " + seconds + "s was asked of the " + size + "s recorded");
        }
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
